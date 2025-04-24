package org.greatgamesonly.opensource.utils.reflectionutils;

import org.apache.commons.beanutils.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public final class ReflectionUtils {

    public static final List<Class<?>> BASE_VALUE_TYPES = List.of(
            String.class,
            Character.class,
            Byte.class,
            Integer.class,
            Short.class,
            Long.class,
            Boolean.class,
            Double.class,
            Float.class,
            BigDecimal.class,
            BigInteger.class,
            Number.class,
            Timestamp.class,
            Calendar.class,
            java.sql.Date.class,
            java.util.Date.class,
            byte[].class, // its sort of primitive just wrapped in a array and the usual base way of working with bytes for every object
            char.class,
            byte.class,
            int.class,
            short.class,
            long.class,
            boolean.class,
            double.class,
            float.class
    );

    private static final ConcurrentHashMap<String, List<ReflectionSimilarClassToClassMethod>> similarClassToClassMethodGroupingByClassToClassNames = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Method> methodsCached = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Field> fieldsCached = new ConcurrentHashMap<>();
    private static final String CALL_METHOD_QUICK_CACHE_KEY_LOOKUP_UNFORMATTED = "cc%s_%s__%s";
    private static final String SET_FIELD_QUICK_CACHE_KEY_LOOKUP_UNFORMATTED = "ff%s_%s__";
    private static final String SET_METHOD_QUICK_CACHE_KEY_LOOKUP_UNFORMATTED = "mm%s_%s__";


    private static final Timer cacheCleanerLowProfileTimer;
    static {
        Timer timer = new Timer();
        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        methodsCached.clear();
                        fieldsCached.clear();
                        similarClassToClassMethodGroupingByClassToClassNames.clear();
                    }
                },
                300000L, 300000L
        );
        cacheCleanerLowProfileTimer = timer;
    }

    public static boolean fieldExists(Class<?> clazz, String field) {
        return fieldExists(field,clazz);
    }

    public static boolean fieldExists(String field, Class<?> clazz) {
        return Arrays.stream(getClassFields(clazz, false,null)).
                anyMatch(clazzField -> clazzField.getName().equals(field));
    }

    public static <T> T getFieldValueNoException(String field, Object instance) {
        T result = null;
        try {
            result = getFieldValue(field, instance);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {}
        return result;
    }

    public static <T> T getFieldValue(String field, Object instance) throws NoSuchFieldException, IllegalAccessException {
        T result = null;
        Field fieldReflection = null;
        boolean hadToSetMethodToAccessible = false;
        boolean hadToSetFieldToAccessible = false;
        boolean isStaticField;
        try {
            fieldReflection = instance.getClass().getDeclaredField(field);
            isStaticField = Modifier.isStatic(fieldReflection.getModifiers());
        } catch(NoSuchFieldException e) {
            try {
                Method fieldGetterMethod = instance.getClass().getMethod("get" + capitalize(field));
                try {
                    if (!fieldGetterMethod.canAccess(instance)) {
                        hadToSetMethodToAccessible = true;
                        fieldGetterMethod.setAccessible(true);
                        result = (T) fieldGetterMethod.invoke(instance);
                    } else {
                        result = (T) fieldGetterMethod.invoke(instance);
                    }
                    return result;
                } finally {
                    if(hadToSetMethodToAccessible) {
                        fieldGetterMethod.setAccessible(false);
                    }
                }
            } catch(NoSuchMethodException | InvocationTargetException innerException) {
                throw e;
            }
        }
        try {
            boolean canAccess = isStaticField ? fieldReflection.canAccess(null) : fieldReflection.canAccess(instance);
            if(!canAccess) {
                fieldReflection.setAccessible(true);
                hadToSetFieldToAccessible = true;
            }
            result = isStaticField ? (T) fieldReflection.get(null) : (T) fieldReflection.get(instance);
        } finally {
            if(hadToSetFieldToAccessible) {
                fieldReflection.setAccessible(false);
            }
        }
        return result;
    }

    public static void setFieldToNull(Object object, String fieldName) throws IllegalAccessException, NoSuchFieldException {
        setFieldValue(object, fieldName, null);
    }

    public static void setFieldValueNoException(Object object, String fieldName, Object fieldValue) {
        try {
            setFieldValue(object,fieldName,fieldValue);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {}
    }

    public static void setFieldValueViaSetter(Object object, String fieldName, Object fieldValue) throws IllegalAccessException, NoSuchMethodException {
        String methodCacheKey = String.format(SET_METHOD_QUICK_CACHE_KEY_LOOKUP_UNFORMATTED, object.getClass(), fieldName);
        Method method = methodsCached.get(methodCacheKey);
        try {
            if (method == null) {
                Class<?> fieldType = fieldValue != null ? fieldValue.getClass() : getFieldType(object, fieldName);
                method = object.getClass().getMethod("set" + capitalize(fieldName), fieldType);
                methodsCached.put(methodCacheKey, method);
            }
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodException("Setter method not found for field: " + fieldName);
        }

        boolean hadToSetMethodToAccessible = false;
        try {
            if (!method.canAccess(object)) {
                hadToSetMethodToAccessible = true;
                method.setAccessible(true);
            }
            method.invoke(object, fieldValue);
        } catch (InvocationTargetException e) {
            throw new IllegalAccessException(e.getCause().getMessage());
        } finally {
            if (hadToSetMethodToAccessible) {
                method.setAccessible(false);
            }
        }
    }

    private static Class<?> getFieldType(Object object, String fieldName) throws NoSuchMethodException {
        try {
            return object.getClass().getMethod("get" + capitalize(fieldName)).getReturnType();
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodException("Getter method not found for field: " + fieldName);
        }
    }

    public static void setFieldViaDirectAccess(Object object, Field field, Object fieldValue) throws IllegalAccessException {
        boolean hadToSetFieldToAccessible = false;
        try {
            if(!field.canAccess(object)) {
                field.setAccessible(true);
                hadToSetFieldToAccessible = true;
            }
            field.set(object, fieldValue);
        } finally {
            if(hadToSetFieldToAccessible) {
                field.setAccessible(false);
            }
        }
    }

    public static void setFieldValueAsynchronously(Object object, String fieldName, Object fieldValue) throws IllegalAccessException, NoSuchFieldException {
        setFieldValueAsynchronouslyInternal(object,fieldName,fieldValue,0);
    }

    private static void setFieldValueAsynchronouslyInternal(Object object, String fieldName, Object fieldValue, int totalTimesRetried) throws IllegalAccessException, NoSuchFieldException {
        String fieldCacheKey = String.format(SET_FIELD_QUICK_CACHE_KEY_LOOKUP_UNFORMATTED,object.getClass(),fieldName);
        Field field = fieldsCached.get(fieldCacheKey);
        if(field == null) {
            field = object.getClass().getDeclaredField(fieldName);
            fieldsCached.put(fieldCacheKey, field);
        }
        try {
            setFieldValueViaSetter(object,fieldName,fieldValue);
        } catch (NoSuchMethodException e) {
            if(totalTimesRetried >= 5) {
                setFieldViaDirectAccess(object, field, fieldValue);
            } else {
                setFieldValueAsynchronouslyInternal(object,fieldName,fieldValue,++totalTimesRetried);
            }
        }
    }

    public static void setFieldValue(Object object, String fieldName, Object fieldValue) throws IllegalAccessException, NoSuchFieldException {
        String fieldCacheKey = String.format(SET_FIELD_QUICK_CACHE_KEY_LOOKUP_UNFORMATTED,object.getClass(),fieldName);
        Field field = fieldsCached.get(fieldCacheKey);
        if(field == null) {
            try {
                field = object.getClass().getDeclaredField(fieldName);
                fieldsCached.put(fieldCacheKey, field);
            } catch (NoSuchFieldException e) {
                try {
                    setFieldValueViaSetter(object,fieldName,fieldValue);
                } catch (NoSuchMethodException ex) {
                    throw new NoSuchFieldException(ex.getMessage());
                }
            }
        }
        setFieldViaDirectAccess(object, field, fieldValue);
    }

    public static Field[] getClassFields(Class<?> clazz) {
        return getClassFields(clazz, false, new ArrayList<>(), true, true);
    }

    public static Field[] getClassFields(Class<?> clazz, boolean excludeDeclaredCustomClassFields) {
        return getClassFields(clazz, excludeDeclaredCustomClassFields, new ArrayList<>(), true, true);
    }

    public static Field[] getClassFields(Class<?> clazz, boolean excludeDeclaredCustomClassFields, List<Class<? extends Annotation>> bypassWithTheseAnnotations) {
        return getClassFields(clazz, excludeDeclaredCustomClassFields, bypassWithTheseAnnotations, true, true);
    }

    public static Field[] getClassFields(Class<?> clazz, boolean excludeDeclaredCustomClassFields, List<Class<? extends Annotation>> bypassWithTheseAnnotations, boolean includeLists) {
        return getClassFields(clazz, excludeDeclaredCustomClassFields, bypassWithTheseAnnotations, includeLists, true);
    }

    public static Field[] getClassFields(Class<?> clazz, boolean excludeDeclaredCustomClassFields, List<Class<? extends Annotation>> bypassWithTheseAnnotations, boolean includeLists, boolean includeMaps) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> (
                        ((!excludeDeclaredCustomClassFields && !checkIfClassIsFromMainJavaPackages(field.getType())) ||
                        BASE_VALUE_TYPES.contains(field.getType()) ||
                        field.getType().isPrimitive() ||
                        field.getType().isEnum() ||
                        (includeLists && Collection.class.isAssignableFrom(field.getType())) ||
                        (includeMaps && Map.class.isAssignableFrom(field.getType()))) &&
                        Arrays.stream(field.getAnnotations()).noneMatch(annotation -> bypassWithTheseAnnotations != null && bypassWithTheseAnnotations.contains(annotation.annotationType()))
                )).toArray(Field[]::new);
    }

    public static Field[] getClassFieldsOfType(Class<?> clazz, Class<?> classType) {
        return Arrays.stream(getClassFields(clazz))
                .filter(field -> classType.isAssignableFrom(field.getType()))
                .toArray(Field[]::new);
    }

    public static <T> List<T> getObjectFieldValuesOfTypeNoException(Object object, Class<T> type) {
        List<T> result = new ArrayList<>();
        try {
            result = getObjectFieldValuesOfType(object, type);
        } catch(NoSuchFieldException | IllegalAccessException ignore) {}
        return result;
    }

    public static <T> List<T> getObjectFieldValuesOfType(Object object, Class<T> type) throws NoSuchFieldException, IllegalAccessException {
        List<T> result = new ArrayList<>();
        Field[] fields = Arrays.stream(getClassFields(object.getClass()))
                .filter(field -> type.isAssignableFrom(field.getType()))
                .toArray(Field[]::new);
        for(Field field : fields) {
            result.add(getFieldValue(field.getName(),object));
        }
        return result;
    }

    public static Set<String> getGetters(Class<?> clazz) throws IntrospectionException {
        return Arrays.stream(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
                .filter(propertyDescriptor -> propertyDescriptor.getReadMethod() != null)
                .map(propertyDescriptor -> propertyDescriptor.getReadMethod().getName())
                .collect(Collectors.toSet());
    }

    public static Set<Method> getGetterMethods(Class<?> clazz) throws IntrospectionException {
        return Arrays.stream(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
                .filter(propertyDescriptor -> propertyDescriptor.getReadMethod() != null)
                .map(PropertyDescriptor::getReadMethod)
                .collect(Collectors.toSet());
    }

    public static Set<String> getGettersForBaseValueTypes(Class<?> clazz, boolean includeEnums, boolean includeLists) throws IntrospectionException {
        return getGetters(clazz, BASE_VALUE_TYPES, true, includeEnums, includeLists);
    }

    public static Set<String> getGetters(Class<?> clazz, List<Class<?>> onlyForTheseValueTypes, boolean includePrimitives, boolean includeEnums, boolean includeLists) throws IntrospectionException {
        List<Class<?>> finalOnlyForTheseValueTypes = (onlyForTheseValueTypes == null) ? new ArrayList<>() : new ArrayList<>(onlyForTheseValueTypes);
        return Arrays.stream(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
                .filter(
                    propertyDescriptor -> propertyDescriptor.getReadMethod() != null &&
                    (
                        finalOnlyForTheseValueTypes.contains(propertyDescriptor.getPropertyType()) ||
                        (includePrimitives && propertyDescriptor.getPropertyType().isPrimitive()) ||
                        (includeLists && Collection.class.isAssignableFrom(propertyDescriptor.getPropertyType())) ||
                        (includeEnums && propertyDescriptor.getPropertyType().isEnum())
                    )
                )
                .map(propertyDescriptor -> propertyDescriptor.getReadMethod().getName())
                .collect(Collectors.toSet());
    }

    public static Set<String> getSettersForBaseValueTypes(Class<?> clazz, boolean includeEnums, boolean includeLists) throws IntrospectionException {
        return getSetters(clazz, BASE_VALUE_TYPES, true, includeEnums, includeLists);
    }

    public static Set<String> getSetters(Class<?> clazz) throws IntrospectionException {
        return Arrays.stream(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
                .filter(propertyDescriptor -> propertyDescriptor .getWriteMethod() != null)
                .map(propertyDescriptor -> propertyDescriptor.getWriteMethod().getName())
                .collect(Collectors.toSet());
    }

    public static Set<String> getSetters(Class<?> clazz, List<Class<?>> onlyForTheseValueTypes) throws IntrospectionException {
        List<Class<?>> finalOnlyForTheseValueTypes = (onlyForTheseValueTypes == null) ? new ArrayList<>() : onlyForTheseValueTypes;;
        return Arrays.stream(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
                .filter(propertyDescriptor -> propertyDescriptor.getWriteMethod() != null && finalOnlyForTheseValueTypes.contains(propertyDescriptor.getPropertyType()))
                .map(propertyDescriptor -> propertyDescriptor.getWriteMethod().getName())
                .collect(Collectors.toSet());
    }

    public static Set<String> getSetters(Class<?> clazz, List<Class<?>> onlyForTheseValueTypes, boolean includePrimitives, boolean includeEnums, boolean includeLists) throws IntrospectionException {
        List<Class<?>> finalOnlyForTheseValueTypes = (onlyForTheseValueTypes == null) ? new ArrayList<>() : onlyForTheseValueTypes;
        return Arrays.stream(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
                .filter(
                    propertyDescriptor -> propertyDescriptor.getWriteMethod() != null &&
                    (
                        finalOnlyForTheseValueTypes.contains(propertyDescriptor.getPropertyType()) ||
                        (includePrimitives && propertyDescriptor.getPropertyType().isPrimitive()) ||
                        (includeLists && Collection.class.isAssignableFrom(propertyDescriptor.getPropertyType())) ||
                        (includeEnums && propertyDescriptor.getPropertyType().isEnum())
                    )
                )
                .map(propertyDescriptor -> propertyDescriptor.getWriteMethod().getName())
                .collect(Collectors.toSet());
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param packageName The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static List<Class<?>> getClasses(String packageName)
            throws ClassNotFoundException, IOException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        List<Class<?>> classes = new ArrayList<>();
        for (File directory : dirs) {
            List<Class<?>> innerClasses = findClasses(directory, packageName);
            for (Class<?> clazz : innerClasses) {
                Class<?> classToAdd = getClassByName(clazz.getName());
                classToAdd = classToAdd == null ? Class.forName(clazz.getName()) : classToAdd;
                classes.add(classToAdd);
            }
        }

        if(classes.size() <= 0) {
            classes = findAllClassesUsingRunningJarFile(packageName);
        }

        return classes;
    }

    public static List<Class<?>> findAllClassesUsingRunningJarFile(String packageName) {
        List<Class<?>> result = new ArrayList<>();

        Set<String> classNames = new HashSet<>();
        JarFile jarFile = getCurrentRunningJarFile(null);
        if(jarFile != null) {
            Enumeration<JarEntry> e = jarFile.entries();
            while (e.hasMoreElements()) {
                JarEntry jarEntry = e.nextElement();
                if (jarEntry.getName().endsWith(".class")) {
                    String className = jarEntry.getName()
                            .replace("/", ".")
                            .replace(".class", "");
                        classNames.add(className);
                }
            }

            for (String className : classNames) {
                try {
                    Class<?> classToAdd = getClassByName(className);
                    classToAdd = classToAdd == null ? Class.forName(className) : classToAdd;
                    result.add(classToAdd);
                } catch (Exception | LinkageError ignored) {
                }
            }
        }
        return result;
    }

    private static JarFile getCurrentRunningJarFile(String jarFileName) {
        JarFile jarFile = null;

        try {
            jarFile = new JarFile(getRunningJarPath(getCallerClassName()).toFile());
        } catch(Exception ignored) {}

        if(jarFile == null && getContextClassLoader().getClass().getProtectionDomain() != null && getContextClassLoader().getClass().getProtectionDomain().getCodeSource() != null)
        {
            try {
                jarFile = new JarFile(getContextClassLoader().getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
            } catch (Exception ignored) {}
        }
        if(jarFile == null) {
            try {
                jarFile = new JarFile("application.jar");
            } catch (Exception ignored) {}
        }
        if(jarFile == null) {
            try {
                jarFile = new JarFile("app.jar");
            } catch (Exception ignored) {}
        }
        if(jarFile == null && jarFileName != null) {
            try {
                jarFile = new JarFile(jarFileName);
            } catch (Exception ignored) {}
        }
        return jarFile;
    }

    private static String getCallerClassName() {
        StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
        String callerClassName = null;
        for (int i=1; i<stElements.length; i++) {
            StackTraceElement ste = stElements[i];
            if (!ste.getClassName().equals(ReflectionUtils.class.getName())&& ste.getClassName().indexOf("java.lang.Thread")!=0) {
                if (callerClassName==null) {
                    callerClassName = ste.getClassName();
                } else if (!callerClassName.equals(ste.getClassName())) {
                    return ste.getClassName();
                }
            }
        }
        return null;
    }

    private static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    private static Path getRunningJarPath(String callerClassName) {
        Path result = null;
        try {
            result = Paths.get(Thread.currentThread()
                    .getContextClassLoader()
                    .loadClass(callerClassName)
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
        } catch(ClassNotFoundException | URISyntaxException ignored) {}
        return result;
    }

    private static Class<?> getClass(String className, String packageName) {
        try {
            return Class.forName(packageName + "."
                    + className.substring(0, className.lastIndexOf('.')));
        } catch (ClassNotFoundException e) {
            // handle the exception
        }
        return null;
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param directory   The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        if(files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    assert !file.getName().contains(".");
                    classes.addAll(findClasses(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
                }
            }
        }
        return classes;
    }

    public static List<Object> getAllConstantValuesInClass(Class<?> clazz) throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        List<Object> result = new ArrayList<>();

        Field[] fields = getClassFields(clazz);
        for (Field field : fields) {
            // Check if the field is a constant (static and final)
            if (isConstantField(field)) {
                result.add(clazz.getDeclaredField(field.getName()).get(null));
            }
        }

        return result;
    }

    public static boolean isConstantField(Field field) {
        // Check if the field is a constant (static and final)
        int modifiers = field.getModifiers();
        return (java.lang.reflect.Modifier.isStatic(modifiers) && java.lang.reflect.Modifier.isFinal(modifiers));
    }

    public static String capitalize(String str) {
        return capitalizeString(str);
    }

    public static String capitalizeString(String str) {
        return (str != null && str.length() > 0) ? str.substring(0, 1).toUpperCase() + str.substring(1) : str;
    }

    public static <T> T[] concatenate(T[] a, T[] b) {
        int aLen = a.length;
        int bLen = b.length;
        @SuppressWarnings("unchecked")
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    public static boolean checkIfClassIsFromMainJavaPackages(Class<?> clazz) {
        return (clazz.getName().startsWith("java.") ||
                clazz.getName().startsWith("javax.") ||
                clazz.getName().startsWith("javafx.") ||
                clazz.getName().startsWith("com.sun.") ||
                clazz.getName().startsWith("com.oracle.") ||
                clazz.getName().startsWith("org.apache.") ||
                clazz.getName().startsWith("jdk.") ||
                clazz.getName().startsWith("org.w3c.") ||
                clazz.getName().startsWith("org.xml.") ||
                clazz.getName().startsWith("org.ietf.")
        );
    }

    public static Object callReflectionMethod(Object object, String methodName) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        return callReflectionMethod(object, methodName, null, null);
    }

    // I WOULD SAY WITH THE LATER VERSIONS OF JAVA, JAVA Reflection logic should run fast enough (if kept simple)
    public static Object callReflectionMethod(Object object, String methodName, Object[] methodParams, Class<?>[] methodParamTypes) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Object methodResult;
        boolean setParams = methodParams != null && methodParams.length != 0;
        Method method = setParams ? object.getClass().getMethod(methodName, methodParamTypes) : object.getClass().getMethod(methodName);
        boolean hadToSetMethodToAccessible = false;
        if(!method.canAccess(object)) {
            method.setAccessible(true);
            hadToSetMethodToAccessible = true;
        }
        try {
            if (setParams) {
                methodResult = method.invoke(object, methodParams);
            } else {
                methodResult = method.invoke(object);
            }
        } finally {
            if(hadToSetMethodToAccessible) {
                method.setAccessible(false);
            }
        }
        return methodResult;
    }

    public static Object callReflectionMethodQuick(Object object, String methodName) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        return callReflectionMethodQuick(object,methodName,null,Object.class);
    }

    public static Object callReflectionMethodQuick(Object object, String methodName, Object methodParam, Class<?> methodParamType) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        boolean setParams = methodParam != null && methodParamType != null;
        String methodCacheKey = String.format(CALL_METHOD_QUICK_CACHE_KEY_LOOKUP_UNFORMATTED,object.getClass(),methodName,methodParamType);
        Method method = methodsCached.get(methodCacheKey);
        if(method == null) {
            method = setParams ? object.getClass().getMethod(methodName, methodParamType) : object.getClass().getMethod(methodName);
            methodsCached.put(methodCacheKey,method);
        }
        return (setParams) ? method.invoke(object, methodParam) : method.invoke(object);
    }

    public static Object callReflectionMethodQuickIgnoreException(Object object, String methodName) {
        return callReflectionMethodQuickIgnoreException(object,methodName, null, Object.class);
    }

    public static Object callReflectionMethodQuickIgnoreException(Object object, String methodName, Object methodParam, Class<?> methodParamType) {
        Object result = null;
        boolean setParams = methodParam != null && methodParamType != null;
        String methodCacheKey;
        Method method;
        try {
            methodCacheKey = String.format(CALL_METHOD_QUICK_CACHE_KEY_LOOKUP_UNFORMATTED,object.getClass(),methodName,methodParamType);
            method = methodsCached.get(methodCacheKey);
            if (method == null) {
                method = setParams ? object.getClass().getMethod(methodName, methodParamType) : object.getClass().getMethod(methodName);
                methodsCached.put(methodCacheKey, method);
            }
            result = (setParams) ? method.invoke(object, methodParam) : method.invoke(object);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException ignored) {}
        return result;
    }

    public static Object callReflectionMethodQuick(Object object, String methodName, Object[] methodParam, Class<?>[] methodParamType) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        boolean setParams = methodParam != null && methodParam.length > 0;

        String methodCacheKey = setParams ?
                object.getClass()+"_"+methodName+"_"+methodParamType[0]+"_"+methodParamType.length :
                object.getClass()+"_"+methodName;

        Method method = methodsCached.get(methodCacheKey);
        if(method == null) {
            method = setParams ? object.getClass().getMethod(methodName, methodParamType) : object.getClass().getMethod(methodName);
            methodsCached.put(methodCacheKey,method);
        }
        return (setParams) ? method.invoke(object, methodParam) : method.invoke(object);
    }

    public static Object callReflectionMethod(Object object, Method method) throws InvocationTargetException, IllegalAccessException {
        return callReflectionMethod(object,method,null);
    }

    public static Object callReflectionMethod(Object object, Method method, Object... methodParams) throws InvocationTargetException, IllegalAccessException {
        Object methodResult;
        boolean setParams = method.getParameterTypes().length > 0 && methodParams != null;
        boolean hadToSetMethodToAccessible = false;
        if(!method.canAccess(object)) {
            method.setAccessible(true);
            hadToSetMethodToAccessible = true;
        }
        try {
            if (setParams) {
                methodResult = method.invoke(object, methodParams);
            } else {
                methodResult = method.invoke(object);
            }
        } finally {
            if(hadToSetMethodToAccessible) {
                method.setAccessible(false);
            }
        }
        return methodResult;
    }

    public static Object callReflectionMethodQuick(Object object, Method method, Object... methodParams) throws InvocationTargetException, IllegalAccessException {
        return (methodParams != null) ? method.invoke(object, methodParams) : method.invoke(object);
    }

    public static <T> T callReflectionMethodGeneric(Object object, String methodName) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        return callReflectionMethodGeneric(object, methodName, null, null);
    }

    public static <T> T callReflectionMethodGeneric(Object object, String methodName, Object[] methodParams, Class<?>[] methodParamTypes) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Object methodResult;
        boolean setParams = methodParams != null && methodParams.length != 0;
        Method method = setParams ? object.getClass().getMethod(methodName, methodParamTypes) : object.getClass().getMethod(methodName);
        boolean hadToSetMethodToAccessible = false;
        if(!method.canAccess(object)) {
            method.setAccessible(true);
            hadToSetMethodToAccessible = true;
        }
        try {
            if (setParams) {
                methodResult = method.invoke(object, methodParams);
            } else {
                methodResult = method.invoke(object);
            }
        } finally {
            if(hadToSetMethodToAccessible) {
                method.setAccessible(false);
            }
        }
        return (T) methodResult;
    }

    public static Class<?> getClassByName(String fullName) {
        Class<?> result = null;
        Method method = null;
        boolean methodHadToBeSetToAccessible = false;
        try {
            try {
                result = Thread.currentThread().getContextClassLoader().loadClass(fullName);
            } catch (Exception ignore) {}
            if(result == null) {
                method = Thread.currentThread().getContextClassLoader().getClass().getDeclaredMethod("findClass", String.class, String.class);
                if (!method.canAccess(Thread.currentThread().getContextClassLoader())) {
                    method.setAccessible(true);
                    methodHadToBeSetToAccessible = true;
                }
                result = (Class<?>) method.invoke(Thread.currentThread().getContextClassLoader(), Thread.currentThread().getContextClassLoader().getUnnamedModule().getName(), fullName);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        } finally {
            if(methodHadToBeSetToAccessible) {
                method.setAccessible(false);
            }
        }
        return result;
    }

    public static <T> T cleanObject(T objectToClean) throws NoSuchFieldException, IllegalAccessException {
        if(objectToClean != null) {
            List<Field> fields = List.of(getClassFields(objectToClean.getClass()));
            for(Field field : fields) {
                if(field.getType().isPrimitive()) {
                    if (isNumericField(field)) {
                        setFieldValue(objectToClean, field.getName(), 0);
                    } else if (field.getType().equals(boolean.class)) {
                        setFieldValue(objectToClean, field.getName(), false);
                    } else if (field.getType().equals(char.class)) {
                        setFieldValue(objectToClean, field.getName(), '\u0000');
                    }
                } else {
                    setFieldToNull(objectToClean, field.getName());
                }
            }
        }
        return objectToClean;
    }

    public static boolean isNumericField(Field field) {
        return field != null &&
                (field.getType().equals(Short.class) ||
                        field.getType().equals(Integer.class) ||
                        field.getType().equals(Long.class) ||
                        field.getType().equals(Float.class) ||
                        field.getType().equals(Double.class) ||
                        field.getType().equals(BigInteger.class) ||
                        field.getType().equals(BigDecimal.class) ||
                        field.getType().equals(short.class) ||
                        field.getType().equals(int.class) ||
                        field.getType().equals(long.class) ||
                        field.getType().equals(float.class) ||
                        field.getType().equals(double.class)
                );
    }

    public List<Object> getObjectFieldValues(Object object) throws NoSuchFieldException, IllegalAccessException {
        List<Object> result = new ArrayList<>();

        Field[] fields = getClassFields(object.getClass());
        for (Field field : fields) {
            result.add(object.getClass().getDeclaredField(field.getName()).get(object));
        }

        return result;
    }

    public static <T> T mergeNonBaseObjectIntoSimilarNonBaseObject(Object objectFrom, T objectTo) throws Exception {
        return mergeNonBaseObjectIntoSimilarNonBaseObject(objectFrom, objectTo, true);
    }

    public static <T> T mergeNonBaseObjectIntoSimilarNonBaseObject(Object objectFrom, T objectTo, boolean copyOverEmptyValues) throws Exception {
        if(objectTo.getClass().isAssignableFrom(objectFrom.getClass())) {
            RecursiveBeanUtils recursiveBeanUtils = new RecursiveBeanUtils();
            recursiveBeanUtils.copyProperties(objectTo, objectFrom, copyOverEmptyValues);
        } else {
            throw new Exception("objectTo cannot be assigned to objectFrom");
        }
        return objectTo;
    }

    public static <T> T mergeNonBaseObjectIntoNonBaseObject(Object objectFrom, T objectTo) throws Exception {
        return mergeNonBaseObjectIntoNonBaseObject(objectFrom,objectTo,true);
    }

    public static <T> T mergeNonBaseObjectIntoNonBaseObject(Object objectFrom, T objectTo, boolean copyOverEmptyValues) throws Exception {
        RecursiveBeanUtils recursiveBeanUtils = new RecursiveBeanUtils();
        recursiveBeanUtils.copyProperties(objectTo, objectFrom, copyOverEmptyValues);
        return objectTo;
    }

    protected static class RecursiveBeanUtils extends BeanUtilsBean {
        // to keep from any chance infinite recursion lets limit each object to 1 instance at a time in the stack
        public List<Object> lookingAt = new ArrayList<>();
        private CustomPropertyUtilsBean propertyUtilsBean = new CustomPropertyUtilsBean();

        private boolean copyOverEmptyValues;

        /**
         * Override to ensure that we dont end up in infinite recursion
         * @param dest
         * @param orig
         * @throws IllegalAccessException
         * @throws InvocationTargetException
         */
        @Override
        public void copyProperties(Object dest, Object orig) throws IllegalAccessException, InvocationTargetException {
            copyProperties(dest,orig,true);
        }

        public void copyProperties(Object dest, Object orig, boolean copyOverEmptyValues) throws IllegalAccessException, InvocationTargetException {
            try {
                this.copyOverEmptyValues = copyOverEmptyValues;
                // if we have an object in our list, that means we hit some sort of recursion, stop here.
                if(lookingAt.stream().anyMatch(o->o == dest)) {
                    return; // recursion detected
                }
                lookingAt.add(dest);
                super.copyProperties(dest, orig);
            } finally {
                lookingAt.remove(dest);
            }
        }

        @Override
        public void copyProperty(Object dest, String name, Object value)
                throws IllegalAccessException, InvocationTargetException {
            boolean isCollection = value instanceof Collection<?>;
            boolean isMap = value instanceof Map<?,?>;
            boolean isEnum = value instanceof Enum<?>;
            boolean isEmptyValue = false;
            if(isCollection) {
                isEmptyValue = ((Collection<?>) value).isEmpty();
            }

            if(isMap) {
                isEmptyValue = ((Map<?,?>) value).isEmpty();
            }

            // don't copy over null or empty values unless specified to
            if (value != null && (!isEmptyValue || this.copyOverEmptyValues)) {
                // attempt to check if the value is a pojo we can clone using nested calls
                if(isMap) {
                    try {
                        Object prop = super.getPropertyUtils().getProperty(dest, name);
                        ((Map<Object, Object>) prop).putAll(((Map<Object, Object>) value));
                    } catch (NoSuchMethodException e) {
                        return;
                    }
                } else if(isEnum) {
                    try {
                        setFieldValue(dest, name, value);
                    } catch (java.lang.NoSuchFieldException e) {
                        throw new InvocationTargetException(e);
                    }
                } else if(!value.getClass().isPrimitive() && !value.getClass().isSynthetic() && !checkIfClassIsFromMainJavaPackages(value.getClass())) {
                    try {
                        Object prop = super.getPropertyUtils().getProperty(dest, name);
                        // get current value, if its null then clone the value and set that to the value
                        if (prop == null) {
                            super.setProperty(dest, name, super.cloneBean(value));
                        } else {
                            // get the destination value and then recursively call
                            copyProperties(prop, value);
                        }
                    } catch (NoSuchMethodException e) {
                        return;
                    } catch (InstantiationException e) {
                        throw new RuntimeException("Nested property could not be cloned.", e);
                    }
                } else {
                    super.copyProperty(dest, name, value);
                }
            }
        }

        @Override
        public CustomPropertyUtilsBean getPropertyUtils() {
            return this.propertyUtilsBean;
        }
    }

    public static class CustomPropertyUtilsBean extends PropertyUtilsBean {
        private final Log log = LogFactory.getLog(PropertyUtils.class);

        @Override
        public void setSimpleProperty(Object bean, String name, Object value) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
            if (bean == null) {
                throw new IllegalArgumentException("No bean specified");
            } else if (name == null) {
                throw new IllegalArgumentException("No name specified for bean class '" + bean.getClass() + "'");
            } else if (this.getResolver().hasNested(name)) {
                throw new IllegalArgumentException("Nested property names are not allowed: Property '" + name + "' on bean class '" + bean.getClass() + "'");
            //} else if (this.getResolver().isIndexed(name)) {
                //throw new IllegalArgumentException("Indexed property names are not allowed: Property '" + name + "' on bean class '" + bean.getClass() + "'");
            //} else if (this.getResolver().isMapped(name)) {
                //throw new IllegalArgumentException("Mapped property names are not allowed: Property '" + name + "' on bean class '" + bean.getClass() + "'");
            } else if (bean instanceof DynaBean) {
                DynaProperty descriptor = ((DynaBean)bean).getDynaClass().getDynaProperty(name);
                if (descriptor == null) {
                    throw new NoSuchMethodException("Unknown property '" + name + "' on dynaclass '" + ((DynaBean)bean).getDynaClass() + "'");
                } else {
                    ((DynaBean)bean).set(name, value);
                }
            } else {
                PropertyDescriptor descriptor = this.getPropertyDescriptor(bean, name);
                if (descriptor == null) {
                    throw new NoSuchMethodException("Unknown property '" + name + "' on class '" + bean.getClass() + "'");
                } else {
                    Method writeMethod = this.getWriteMethod(bean.getClass(), descriptor);
                    if (writeMethod == null) {
                        throw new NoSuchMethodException("Property '" + name + "' has no setter method in class '" + bean.getClass() + "'");
                    } else {
                        Object[] values = new Object[]{value};
                        if (this.log.isTraceEnabled()) {
                            String valueClassName = value == null ? "<null>" : value.getClass().getName();
                            this.log.trace("setSimpleProperty: Invoking method " + writeMethod + " with value " + value + " (class " + valueClassName + ")");
                        }
                        this.invokeMethod(writeMethod, bean, values);
                    }
                }
            }
        }

        private Object invokeMethod(Method method, Object bean, Object[] values) throws IllegalAccessException, InvocationTargetException {
            if (bean == null) {
                throw new IllegalArgumentException("No bean specified - this should have been checked before reaching this method");
            } else {
                String valueString;
                Class[] parTypes;
                String expectedString;
                IllegalArgumentException e;
                try {
                    return method.invoke(bean, values);
                } catch (NullPointerException var9) {
                    valueString = "";
                    if (values != null) {
                        for(int i = 0; i < values.length; ++i) {
                            if (i > 0) {
                                valueString = valueString + ", ";
                            }

                            if (values[i] == null) {
                                valueString = valueString + "<null>";
                            } else {
                                valueString = valueString + values[i].getClass().getName();
                            }
                        }
                    }

                    expectedString = "";
                    parTypes = method.getParameterTypes();
                    if (parTypes != null) {
                        for(int i = 0; i < parTypes.length; ++i) {
                            if (i > 0) {
                                expectedString = expectedString + ", ";
                            }

                            expectedString = expectedString + parTypes[i].getName();
                        }
                    }

                    e = new IllegalArgumentException("Cannot invoke " + method.getDeclaringClass().getName() + "." + method.getName() + " on bean class '" + bean.getClass() + "' - " + var9.getMessage() + " - had objects of type \"" + valueString + "\" but expected signature \"" + expectedString + "\"");
                    if (!BeanUtils.initCause(e, var9)) {
                        this.log.error("Method invocation failed", var9);
                    }

                    throw e;
                } catch (IllegalArgumentException var10) {
                    valueString = "";
                    if (values != null) {
                        for(int i = 0; i < values.length; ++i) {
                            if (i > 0) {
                                valueString = valueString + ", ";
                            }

                            if (values[i] == null) {
                                valueString = valueString + "<null>";
                            } else {
                                valueString = valueString + values[i].getClass().getName();
                            }
                        }
                    }

                    expectedString = "";
                    parTypes = method.getParameterTypes();
                    if (parTypes != null) {
                        for(int i = 0; i < parTypes.length; ++i) {
                            if (i > 0) {
                                expectedString = expectedString + ", ";
                            }

                            expectedString = expectedString + parTypes[i].getName();
                        }
                    }

                    e = new IllegalArgumentException("Cannot invoke " + method.getDeclaringClass().getName() + "." + method.getName() + " on bean class '" + bean.getClass() + "' - " + var10.getMessage() + " - had objects of type \"" + valueString + "\" but expected signature \"" + expectedString + "\"");
                    if (!BeanUtils.initCause(e, var10)) {
                        this.log.error("Method invocation failed", var10);
                    }

                    throw e;
                }
            }
        }
    }

    /*public static <T> T mergeNonBaseObjectIntoNonBaseObject(Object objectFrom, T objectTo) throws Exception {
        List<ReflectionSimilarClassToClassMethod> reflectionSimilarClassToClassMethods = getAllSimilarClassToClassMethodToMethodWrappers(objectFrom, objectTo);
        for(ReflectionSimilarClassToClassMethod reflectionSimilarClassToClassMethod : reflectionSimilarClassToClassMethods) {
            Object getterValue = callReflectionMethod(objectFrom,reflectionSimilarClassToClassMethod.getMethodObjectFromGetter());
            if(getterValue == null) {
                continue;
            }

            Class<?> setterParamValueType = Collection.class.isAssignableFrom(getterValue.getClass()) ?
                    findValueTypeForNonEmptyList((List<?>) getterValue) :
                    getterValue.getClass();

            setterParamValueType = setterParamValueType.isArray() ? findValueTypeForNonEmptyArray(new Object[]{getterValue}) : setterParamValueType;
            if(setterParamValueType == null) {
                continue;
            }

            if(
                !setterParamValueType.isPrimitive() &&
                !setterParamValueType.isEnum() &&
                !BASE_VALUE_TYPES.contains(setterParamValueType) &&
                !checkIfClassIsFromMainJavaPackages(setterParamValueType)
            ) {
                getterValue = mergeNonBaseObjectIntoNonBaseObject(getterValue, setterParamValueType.getDeclaredConstructor().newInstance());
            }
            callReflectionMethod(objectTo, reflectionSimilarClassToClassMethod.getMethodObjectToSetter(), getterValue);
        }
        return objectTo;
    }

    public static <T> T shallowMergeNonBaseObjectIntoNonBaseObject(Object objectFrom, T objectTo) throws Exception {
        List<ReflectionSimilarClassToClassMethod> reflectionSimilarClassToClassMethods = getAllSimilarClassToClassMethodToMethodWrappers(objectFrom, objectTo);
        for(ReflectionSimilarClassToClassMethod reflectionSimilarClassToClassMethod : reflectionSimilarClassToClassMethods) {
            Object getterValue = callReflectionMethod(objectFrom,reflectionSimilarClassToClassMethod.getMethodObjectFromGetter());
            if(getterValue == null) {
                continue;
            }
            callReflectionMethod(objectTo, reflectionSimilarClassToClassMethod.getMethodObjectToSetter(), getterValue);
        }
        return objectTo;
    }

    public static <T> T shallowMergeNonBaseObjectIntoNonBaseObjectQuick(Object objectFrom, T objectTo) throws Exception {
        List<ReflectionSimilarClassToClassMethod> reflectionSimilarClassToClassMethods = getAllSimilarClassToClassMethodToMethodWrappers(objectFrom, objectTo);
        for(ReflectionSimilarClassToClassMethod reflectionSimilarClassToClassMethod : reflectionSimilarClassToClassMethods) {
            if(!reflectionSimilarClassToClassMethod.getMethodObjectToSetter().canAccess(objectTo) || !reflectionSimilarClassToClassMethod.getMethodObjectFromGetter().canAccess(objectFrom)) {
                continue;
            }
            Object getterValue = callReflectionMethodQuick(objectFrom,reflectionSimilarClassToClassMethod.getMethodObjectFromGetter());
            if(getterValue == null) {
                continue;
            }
            callReflectionMethod(objectTo, reflectionSimilarClassToClassMethod.getMethodObjectToSetter(), getterValue);
        }
        return objectTo;
    }

    public static <T> List<ReflectionSimilarClassToClassMethod> getAllSimilarClassToClassMethodToMethodWrappers(Object objectFrom, T objectTo) throws Exception {
        String key = objectFrom.getClass().getName()+"_TO_"+objectTo.getClass().getName();
        List<ReflectionSimilarClassToClassMethod> reflectionSimilarClassToClassMethods = similarClassToClassMethodGroupingByClassToClassNames.get(key);
        if(reflectionSimilarClassToClassMethods == null) {
            if(
                (objectFrom.getClass().isPrimitive() || objectTo.getClass().isPrimitive()) ||
                BASE_VALUE_TYPES.contains(objectFrom.getClass()) ||
                BASE_VALUE_TYPES.contains(objectTo.getClass())
            ) {
                throw new Exception("objectFrom and/or objectTo invalid");
            }
            List<PropertyDescriptor> allObjectFromPropertyDescriptors = Arrays.stream(Introspector.getBeanInfo(objectFrom.getClass()).getPropertyDescriptors())
                .filter(propertyDescriptor -> propertyDescriptor.getWriteMethod() != null && propertyDescriptor.getReadMethod() != null)
                .collect(Collectors.toList());

            List<PropertyDescriptor> allObjectToPropertyDescriptors = Arrays.stream(Introspector.getBeanInfo(objectTo.getClass()).getPropertyDescriptors())
                .filter(propertyDescriptor -> propertyDescriptor.getWriteMethod() != null && propertyDescriptor.getReadMethod() != null)
                .collect(Collectors.toList());

            reflectionSimilarClassToClassMethods = allObjectFromPropertyDescriptors.stream()
                .map(objectFromPropertyDescriptor -> {
                    Method matchedMethodInObjectToSetter = allObjectToPropertyDescriptors.stream()
                        .filter(objectToDescriptor ->
                            objectToDescriptor.getWriteMethod().getName().equals(objectFromPropertyDescriptor.getWriteMethod().getName()) &&
                            Arrays.equals(objectFromPropertyDescriptor.getWriteMethod().getParameterTypes(),objectToDescriptor.getWriteMethod().getParameterTypes())
                        )
                        .map(PropertyDescriptor::getWriteMethod)
                        .findFirst().orElse(null);
                    if (matchedMethodInObjectToSetter != null) {
                        return new ReflectionSimilarClassToClassMethod(objectFromPropertyDescriptor.getReadMethod(),matchedMethodInObjectToSetter);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            similarClassToClassMethodGroupingByClassToClassNames.put(key,reflectionSimilarClassToClassMethods);
        }
        return reflectionSimilarClassToClassMethods;
    }*/

    public static Class<?> findValueTypeForNonEmptyList(List<?> list) {
        if(list != null && !list.isEmpty() && !list.stream().allMatch(Objects::nonNull)) {
            return list.stream().filter(Objects::nonNull).findFirst().get().getClass();
        } else {
            return null;
        }
    }

    public static <T> Class<?> findValueTypeForNonEmptyArray(T[] list) {
        if(list != null && list.length > 0 && !Arrays.stream(list).allMatch(Objects::nonNull)) {
            return Arrays.stream(list).filter(Objects::nonNull).findFirst().get().getClass();
        } else {
            return null;
        }
    }

}
