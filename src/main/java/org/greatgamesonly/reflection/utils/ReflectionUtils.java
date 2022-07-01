package org.greatgamesonly.reflection.utils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class ReflectionUtils {

    private static Field[] getClassFields(Class<?> clazz) {
        return getClassFields(clazz, false, new ArrayList<>());
    }

    private static Field[] getClassFields(Class<?> clazz, boolean excludeDeclaredCustomClassFields) {
        return getClassFields(clazz, excludeDeclaredCustomClassFields, new ArrayList<>());
    }

    private static Field[] getClassFields(Class<?> clazz, boolean excludeDeclaredCustomClassFields, List<Annotation> bypassWithTheseAnnotations) {
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

    private static Set<String> getGetters(Class<?> clazz) throws IntrospectionException {
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

    private static Set<String> getSetters(Class<?> clazz) throws IntrospectionException {
        return Arrays.stream(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
                .filter(propertyDescriptor -> propertyDescriptor .getWriteMethod() != null)
                .map(propertyDescriptor -> propertyDescriptor.getWriteMethod().getName())
                .collect(Collectors.toSet());
    }

    private static String capitalizeString(String str) {
        return (str != null && str.length() > 0) ? str.substring(0, 1).toUpperCase() + str.substring(1) : str;
    }

    private static <T> T[] concatenate(T[] a, T[] b) {
        int aLen = a.length;
        int bLen = b.length;
        @SuppressWarnings("unchecked")
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    private static boolean checkIfClassIsFromMainJavaPackages(Class<?> clazz) {
        return (clazz.getName().startsWith("java.lang") ||
                clazz.getName().startsWith("java.") ||
                clazz.getName().startsWith("javax.") ||
                clazz.getName().startsWith("com.sun") ||
                clazz.getName().startsWith("com.oracle") ||
                clazz.getName().startsWith("org.apache.*")
        );
    }

    // I WOULD SAY WITH THE LATER VERSIONS OF JAVA, JAVA Reflection logic should run fast enoug (if kept simple)
    protected static Object callReflectionMethod(Object object, String methodName, Object... methodParams) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Object methodResult = null;
        if(methodParams == null || methodParams.length == 0) {
            methodResult = object.getClass().getMethod(methodName).invoke(object);
        } else {
            methodResult = object.getClass().getMethod(methodName).invoke(object, methodParams);
        }
        return methodResult;
    }

}
