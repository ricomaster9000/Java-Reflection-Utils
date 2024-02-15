# Java-Reflection-Utils
A small library for wrapping some reflection logic

see https://github.com/ricomaster9000/Java-Reflection-Utils/blob/main/src/main/java/org/greatgamesonly/opensource/utils/reflectionutils/ReflectionUtils.java for all the methods one can use

add as dependency by using jitpack.io, go to this link: https://jitpack.io/#ricomaster9000/Java-Reflection-Utils/1.1.11

### methods available:

    public static boolean fieldExists(String field, Class<?> clazz)

    public static <T> T getFieldValue(String field, Object instance) throws NoSuchFieldException, IllegalAccessException

    public static <T> T getFieldValueNoException(String field, Object instance)

    public static void setFieldToNull(Object object, String fieldName) throws IllegalAccessException, NoSuchFieldException

    public static void setFieldValueNoException(Object object, String fieldName, Object fieldValue)

    public static void setFieldValue(Object object, String fieldName, Object fieldValue) throws IllegalAccessException, NoSuchFieldException

    public static Field[] getClassFields(Class<?> clazz)

    public static Field[] getClassFields(Class<?> clazz, boolean excludeDeclaredCustomClassFields)

    public static Field[] getClassFields(Class<?> clazz, boolean excludeDeclaredCustomClassFields, List<Class<? extends Annotation>> 
    bypassWithTheseAnnotations)

    public static Field[] getClassFields(Class<?> clazz, boolean excludeDeclaredCustomClassFields, List<Class<? extends Annotation>> 
    bypassWithTheseAnnotations, boolean includeLists)

    public static Set<String> getGetters(Class<?> clazz) throws IntrospectionException

    public static Set<Method> getGetterMethods(Class<?> clazz) throws IntrospectionException

    public static Set<String> getGettersForBaseValueTypes(Class<?> clazz, boolean includeEnums, boolean includeLists) throws 
    IntrospectionException

    public static Set<String> getGetters(Class<?> clazz, List<Class<?>> onlyForTheseValueTypes, boolean includePrimitives, boolean 
    includeEnums, boolean includeLists) throws IntrospectionException

    public static Set<String> getSettersForBaseValueTypes(Class<?> clazz, boolean includeEnums, boolean includeLists) throws 
    IntrospectionException

    public static Set<String> getSetters(Class<?> clazz) throws IntrospectionException

    public static Set<String> getSetters(Class<?> clazz, List<Class<?>> onlyForTheseValueTypes) throws IntrospectionException

    public static Set<String> getSetters(Class<?> clazz, List<Class<?>> onlyForTheseValueTypes, boolean includePrimitives, boolean 
    includeEnums, boolean includeLists) throws IntrospectionException

    public static List<Object> getAllConstantValuesInClass(String packageName) throws IOException, ClassNotFoundException

    public static List<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException

    public static String capitalize(String str)

    public static String capitalizeString(String str)

    public static <T> T[] concatenate(T[] a, T[] b)

    public static boolean checkIfClassIsFromMainJavaPackages(Class<?> clazz)

    public static Object callReflectionMethod(Object object, String methodName) throws InvocationTargetException, NoSuchMethodException, 
    IllegalAccessException

    public static Object callReflectionMethod(Object object, String methodName, Object[] methodParams, Class<?>[] methodParamTypes) 
    throws InvocationTargetException, NoSuchMethodException, IllegalAccessException
    
    public static Object callReflectionMethodQuick(Object object, String methodName) throws
    InvocationTargetException,NoSuchMethodException,IllegalAccessException

    public static Object callReflectionMethodQuick(Object object, String methodName, Object methodParam, Class<?> methodParamType) 
    throws InvocationTargetException, NoSuchMethodException, IllegalAccessException

    public static Object callReflectionMethodQuick(Object object, String methodName, Object[] methodParam, Class<?>[] methodParamType) 
    throws InvocationTargetException, NoSuchMethodException, IllegalAccessException

    public static Object callReflectionMethodQuickIgnoreException(Object object, String methodName)

    public static Object callReflectionMethodQuickIgnoreException(Object object, String methodName, Object methodParam, Class<?> methodParamType)

    public static Object callReflectionMethod(Object object, Method method) throws InvocationTargetException, IllegalAccessException

    public static Object callReflectionMethod(Object object, Method method, Object... methodParams) throws InvocationTargetException, IllegalAccessException

    public static Object callReflectionMethodQuick(Object object, Method method, Object... methodParams) throws InvocationTargetException, IllegalAccessException

    public static <T> T callReflectionMethodGeneric(Object object, String methodName) throws InvocationTargetException, 
    NoSuchMethodException, IllegalAccessException

    public static <T> T callReflectionMethodGeneric(Object object, String methodName, Object[] methodParams, Class<?>[] methodParamTypes)  
    throws InvocationTargetException, NoSuchMethodException, IllegalAccessException

    public static Class<?> getClassByName(String fullName)

    public static <T> T cleanObject(T objectToClean) throws NoSuchFieldException, IllegalAccessException

    public static <T> T mergeNonBaseObjectIntoNonBaseObject(Object objectFrom, T objectTo) throws Exception

    public static <T> T mergeNonBaseObjectIntoSimilarNonBaseObject(Object objectFrom, T objectTo) throws Exception

    public static Class<?> findValueTypeForNonEmptyList(List<?> list)

    public static <T> Class<?> findValueTypeForNonEmptyArray(T[] list)

