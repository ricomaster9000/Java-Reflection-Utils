package org.greatgamesonly.reflection.utils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class ReflectionUtils {

    public static Field[] getClassFields(Class<?> clazz) {
        return getClassFields(clazz, false, new ArrayList<>());
    }

    public static Field[] getClassFields(Class<?> clazz, boolean excludeDeclaredCustomClassFields) {
        return getClassFields(clazz, excludeDeclaredCustomClassFields, new ArrayList<>());
    }

    public static Field[] getClassFields(Class<?> clazz, boolean excludeDeclaredCustomClassFields, List<Class<? extends Annotation>> bypassWithTheseAnnotations) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> (
                        (!excludeDeclaredCustomClassFields && !checkIfClassIsFromMainJavaPackages(field.getType())) ||
                        field.getType().equals(String.class) ||
                        field.getType().equals(Long.class) ||
                        field.getType().equals(Integer.class) ||
                        field.getType().equals(Date.class) ||
                        field.getType().equals(Boolean.class) ||
                        field.getType().isPrimitive() ||
                        field.getType().equals(Timestamp.class) ||
                        field.getType().isEnum() ||
                        bypassWithTheseAnnotations.stream().noneMatch(bypassWithTheseAnnotation -> Arrays.asList(field.getAnnotations()).contains(bypassWithTheseAnnotation)))
                ).toArray(Field[]::new);
    }

    public static Set<String> getGetters(Class<?> clazz) throws IntrospectionException {
        return Arrays.stream(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
                .filter(propertyDescriptor -> propertyDescriptor.getPropertyType().equals(String.class) ||
                        propertyDescriptor.getPropertyType().equals(Long.class) ||
                        propertyDescriptor.getPropertyType().equals(Integer.class) ||
                        propertyDescriptor.getPropertyType().equals(java.sql.Date.class) ||
                        propertyDescriptor.getPropertyType().equals(Date.class) ||
                        propertyDescriptor.getPropertyType().equals(Boolean.class) ||
                        propertyDescriptor.getPropertyType().isPrimitive() ||
                        propertyDescriptor.getPropertyType().equals(Timestamp.class) ||
                        propertyDescriptor.getPropertyType().isEnum()
                )
                .map(propertyDescriptor -> propertyDescriptor.getReadMethod().getName())
                .collect(Collectors.toSet());
    }

    public static Set<String> getSetters(Class<?> clazz) throws IntrospectionException {
        return Arrays.stream(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
                .filter(propertyDescriptor -> propertyDescriptor .getWriteMethod() != null)
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

}
