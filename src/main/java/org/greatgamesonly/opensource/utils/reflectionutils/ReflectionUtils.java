package org.greatgamesonly.opensource.utils.reflectionutils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
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
            byte[].class // its sort of primitive just wrapped in a array and the usual base way of working with bytes for every object
    );

    private static final HashMap<String, List<ReflectionSimilarClassToClassMethod>> similarClassToClassMethodGroupingByClassToClassNames = new HashMap<>();
    private static final HashMap<String, Method> methodsCached = new HashMap<>();
    private static final HashMap<String, Field> fieldsCached = new HashMap<>();
    private static final String CALL_METHOD_QUICK_CACHE_KEY_LOOKUP_UNFORMATTED = "cc%s_%s__%s";
    private static final String SET_FIELD_QUICK_CACHE_KEY_LOOKUP_UNFORMATTED = "ff%s_%s__";

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
        Field fieldReflection = instance.getClass().getDeclaredField(field);
        if(!fieldReflection.canAccess(instance)) {
            fieldReflection.setAccessible(true);
            try {
                result = (T) fieldReflection.get(instance);
            } finally {
                fieldReflection.setAccessible(false);
            }
        } else {
            result = (T) fieldReflection.get(instance);
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

    public static void setFieldValue(Object object, String fieldName, Object fieldValue) throws IllegalAccessException, NoSuchFieldException {
        String fieldCacheKey = String.format(SET_FIELD_QUICK_CACHE_KEY_LOOKUP_UNFORMATTED,object.getClass(),fieldName);
        Field field = fieldsCached.get(fieldCacheKey);
        boolean hadToSetMethodToAccessible = false;
        if(field == null) {
            field = object.getClass().getDeclaredField(fieldName);
            fieldsCached.put(fieldCacheKey, field);
        }
        try {
            if(!field.canAccess(object)) {
                field.setAccessible(true);
                hadToSetMethodToAccessible = true;
            }
            field.set(object, fieldValue);
        } finally {
            if(hadToSetMethodToAccessible) {
                field.setAccessible(false);
            }
        }
    }

    public static Field[] getClassFields(Class<?> clazz) {
        return getClassFields(clazz, false, new ArrayList<>());
    }

    public static Field[] getClassFields(Class<?> clazz, boolean excludeDeclaredCustomClassFields) {
        return getClassFields(clazz, excludeDeclaredCustomClassFields, new ArrayList<>());
    }

    public static Field[] getClassFields(Class<?> clazz, boolean excludeDeclaredCustomClassFields, List<Class<? extends Annotation>> bypassWithTheseAnnotations) {
        return getClassFields(clazz, excludeDeclaredCustomClassFields, bypassWithTheseAnnotations, true);
    }

    public static Field[] getClassFields(Class<?> clazz, boolean excludeDeclaredCustomClassFields, List<Class<? extends Annotation>> bypassWithTheseAnnotations, boolean includeLists) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> (
                        ((!excludeDeclaredCustomClassFields && !checkIfClassIsFromMainJavaPackages(field.getType())) ||
                        BASE_VALUE_TYPES.contains(field.getType()) ||
                        field.getType().isPrimitive() ||
                        field.getType().isEnum() ||
                        (includeLists && Collection.class.isAssignableFrom(field.getType()))) &&
                        Arrays.stream(field.getAnnotations()).noneMatch(annotation -> bypassWithTheseAnnotations != null && bypassWithTheseAnnotations.contains(annotation.annotationType()))
                )).toArray(Field[]::new);
    }

    public static Set<String> getGetters(Class<?> clazz) throws IntrospectionException {
        return Arrays.stream(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
                .filter(propertyDescriptor -> propertyDescriptor.getReadMethod() != null)
                .map(propertyDescriptor -> propertyDescriptor.getReadMethod().getName())
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
            method = Thread.currentThread().getContextClassLoader().getClass().getDeclaredMethod("findClass", String.class, String.class);
            if(!method.canAccess(Thread.currentThread().getContextClassLoader())) {
                method.setAccessible(true);
                methodHadToBeSetToAccessible = true;
            }
            result = (Class<?>) method.invoke(Thread.currentThread().getContextClassLoader(), Thread.currentThread().getContextClassLoader().getUnnamedModule().getName(), fullName);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        } finally {
            if(methodHadToBeSetToAccessible) {
                method.setAccessible(false);
            }
        }
        return result;
    }

    public static <T> T mergeNonBaseObjectIntoNonBaseObject(Object objectFrom, T objectTo) throws Exception {
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
    }

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
