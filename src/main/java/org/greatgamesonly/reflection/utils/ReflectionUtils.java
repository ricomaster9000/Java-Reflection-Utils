package org.greatgamesonly.reflection.utils;

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

public class ReflectionUtils {

    public static final List<Class<?>> BASE_VALUE_TYPES = List.of(
            String.class,
            Long.class,
            Integer.class,
            java.sql.Date.class,
            java.util.Date.class,
            Boolean.class,
            Timestamp.class,
            Double.class,
            Float.class,
            Short.class,
            BigDecimal.class,
            BigInteger.class,
            Character.class,
            Calendar.class
    );

    private static final HashMap<String, List<ReflectionSimilarClassToClassMethod>> similarClassToClassMethodGroupingByClassToClassNames = new HashMap<>();

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
                        (!excludeDeclaredCustomClassFields && !checkIfClassIsFromMainJavaPackages(field.getType())) ||
                        BASE_VALUE_TYPES.contains(field.getType()) ||
                        field.getType().isPrimitive() ||
                        field.getType().isEnum() ||
                        (includeLists && Collection.class.isAssignableFrom(field.getType())) ||
                        bypassWithTheseAnnotations.stream().noneMatch(bypassWithTheseAnnotation -> Arrays.asList(field.getAnnotations()).contains(bypassWithTheseAnnotation)))
                ).toArray(Field[]::new);
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
        return (clazz.getName().startsWith("java.lang") ||
                clazz.getName().startsWith("java.") ||
                clazz.getName().startsWith("javax.") ||
                clazz.getName().startsWith("com.sun") ||
                clazz.getName().startsWith("com.oracle") ||
                clazz.getName().startsWith("org.apache.")
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

    public static Object callReflectionMethod(Object object, Method method) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        return callReflectionMethod(object,method,null);
    }

    public static Object callReflectionMethod(Object object, Method method, Object... methodParams) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
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
        try {
            method = Thread.currentThread().getContextClassLoader().getClass().getDeclaredMethod("findClass", String.class, String.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
        method.setAccessible(true);
        try {
            result = (Class<?>) method.invoke(Thread.currentThread().getContextClassLoader(), Thread.currentThread().getContextClassLoader().getUnnamedModule().getName(),fullName);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return null;
        }
        return result;
    }

    public static <T> T mergeNonBaseObjectIntoNonBaseObject(Object objectFrom, T objectTo) throws Exception {
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
            List<Method> allObjectFromGetters = Arrays.stream(Introspector.getBeanInfo(objectFrom.getClass()).getPropertyDescriptors())
                    .map(PropertyDescriptor::getReadMethod)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<Method> allObjectToSetters = Arrays.stream(Introspector.getBeanInfo(objectTo.getClass()).getPropertyDescriptors())
                    .map(PropertyDescriptor::getWriteMethod)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            reflectionSimilarClassToClassMethods = allObjectFromGetters.stream()
                    .map(objectFromGetter -> {
                        Method matchedMethodInObjectToSetters = allObjectToSetters.stream()
                                .filter(objectToSetter -> objectToSetter.getName().equals(objectFromGetter.getName()) && Arrays.equals(objectToSetter.getParameterTypes(), objectFromGetter.getParameterTypes()))
                                .findFirst().orElse(null);
                        if (matchedMethodInObjectToSetters != null) {
                            return new ReflectionSimilarClassToClassMethod(objectFromGetter, matchedMethodInObjectToSetters);
                        } else {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            similarClassToClassMethodGroupingByClassToClassNames.put(key,reflectionSimilarClassToClassMethods);
        }
        for(ReflectionSimilarClassToClassMethod reflectionSimilarClassToClassMethod : reflectionSimilarClassToClassMethods) {
            callReflectionMethod(
                    objectTo,
                    reflectionSimilarClassToClassMethod.getMethodObjectToSetter(),
                    callReflectionMethod(objectFrom,reflectionSimilarClassToClassMethod.getMethodObjectFromGetter())
            );
        }
        return objectTo;
    }

}
