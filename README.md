# Java-Reflection-Utils
A small library for wrapping some reflection logic

see https://github.com/ricomaster9000/Java-Reflection-Utils/blob/main/src/main/java/org/greatgamesonly/reflection/utils/ReflectionUtils.java for all the methods one can use

### methods available:

    public static Field[] getClassFields(Class<?> clazz)

    public static Field[] getClassFields(Class<?> clazz, boolean excludeDeclaredCustomClassFields)

    public static Field[] getClassFields(Class<?> clazz, boolean excludeDeclaredCustomClassFields, List<Class<? extends Annotation>> 
    bypassWithTheseAnnotations)

    public static Field[] getClassFields(Class<?> clazz, boolean excludeDeclaredCustomClassFields, List<Class<? extends Annotation>> 
    bypassWithTheseAnnotations, boolean includeLists)

    public static Set<String> getGetters(Class<?> clazz) throws IntrospectionException

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

    public static String capitalizeString(String str)

    public static <T> T[] concatenate(T[] a, T[] b)

    public static boolean checkIfClassIsFromMainJavaPackages(Class<?> clazz)

    public static Object callReflectionMethod(Object object, String methodName) throws InvocationTargetException, NoSuchMethodException, 
    IllegalAccessException

    public static Object callReflectionMethod(Object object, String methodName, Object[] methodParams, Class<?>[] methodParamTypes) 
    throws InvocationTargetException, NoSuchMethodException, IllegalAccessException

    public static Object callReflectionMethodQuick(Object object, String methodName, Object methodParam, Class<?> methodParamType) 
    throws InvocationTargetException, NoSuchMethodException, IllegalAccessException

    public static Object callReflectionMethod(Object object, Method method) throws InvocationTargetException, NoSuchMethodException, 
    IllegalAccessException

    public static Object callReflectionMethod(Object object, Method method, Object... methodParams) throws InvocationTargetException, 
    NoSuchMethodException, IllegalAccessException

    public static <T> T callReflectionMethodGeneric(Object object, String methodName) throws InvocationTargetException, 
    NoSuchMethodException, IllegalAccessException

    public static <T> T callReflectionMethodGeneric(Object object, String methodName, Object[] methodParams, Class<?>[] methodParamTypes)  
    throws InvocationTargetException, NoSuchMethodException, IllegalAccessException

    public static Class<?> getClassByName(String fullName)

    public static <T> T mergeNonBaseObjectIntoNonBaseObject(Object objectFrom, T objectTo) throws Exception

    public static <T> T shallowMergeNonBaseObjectIntoNonBaseObject(Object objectFrom, T objectTo) throws Exception

    public static <T> List<ReflectionSimilarClassToClassMethod> getAllSimilarClassToClassMethodToMethodWrappers(
      Object objectFrom,
      TobjectTo
    ) throws Exception

    public static Class<?> findValueTypeForNonEmptyList(List<?> list)

    public static <T> Class<?> findValueTypeForNonEmptyArray(T[] list)

add as dependency by using jitpack.io, go to this link: https://jitpack.io/#ricomaster9000/Java-Reflection-Utils/1.0.15.2
