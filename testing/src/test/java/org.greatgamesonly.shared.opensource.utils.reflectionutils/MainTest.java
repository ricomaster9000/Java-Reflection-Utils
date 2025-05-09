package org.greatgamesonly.shared.opensource.utils.reflectionutils;


import com.sun.jdi.connect.Transport;
import org.greatgamesonly.opensource.utils.reflectionutils.ReflectionUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class MainTest {


    @BeforeClass
    public static void setupEnvironment() {
        System.out.println("TESTS - Setting up repositories&connection-pools and establishing connection");
        System.out.println("TESTS - BEGIN");
    }

    @Test()
    public void testSetFieldToNull() throws NoSuchFieldException, IllegalAccessException {
        System.out.println("TESTS - Set field to null via reflection twice, the second time is to test that caching does not cause issues");

        TestModelClass test1 = new TestModelClass();
        test1.setName("testName");
        ReflectionUtils.setFieldToNull(test1,"name");

        Assert.assertNull("setFieldToNull - field must be set to null - first try", test1.getName());

        test1.setName("testName");
        ReflectionUtils.setFieldToNull(test1,"name");

        Assert.assertNull("setFieldToNull - field must be set to null - second try", test1.getName());
    }

    @Test()
    public void testMergeObjectsOverrideMappedFields() throws Exception {
        System.out.println("TESTS - Override field that is already set in similar object via mergeNonBaseObjectIntoNonBaseObject method");
        String nameOverrideValue = "testNamesdfsdfsdfsdfsdf";
        String descriptionOverrideValue = "sdasad_DESCRIPTION";
        TestSubObjectClass subOverride = new TestSubObjectClass();
        subOverride.setSubName("hello");

        TestModelClass test1 = new TestModelClass();
        test1.setName("testName");
        test1.setDescription(null);
        TestModelClass test1DuplicateToMergeIn = new TestModelClass();
        test1DuplicateToMergeIn.setName(nameOverrideValue);
        test1DuplicateToMergeIn.setDescription(descriptionOverrideValue);
        test1DuplicateToMergeIn.setSub(subOverride);

        ReflectionUtils.mergeNonBaseObjectIntoNonBaseObject(test1DuplicateToMergeIn,test1);
        Assert.assertEquals("OverrideField name - name of object merged into must be equals to name of merged in object", test1.getName(),nameOverrideValue);
        Assert.assertEquals("OverrideField name - description of object merged into must be equals to description of merged in object", test1.getDescription(),descriptionOverrideValue);
        Assert.assertTrue("OverrideField name - sub of object merged into must be equals to sub of merged in object", test1.getSub() != null && test1.getSub().getSubName().equals("hello"));
    }

    @Test()
    public void testSetFieldValue() throws NoSuchFieldException, IllegalAccessException {
        System.out.println("TESTS - Set field to a value via reflection twice, the second time is to test that caching does not cause issues");

        String expectedNewFieldValue1 = "nameybaydray1";
        String expectedNewFieldValue2 = "nameybaydray2";

        TestModelClass test1 = new TestModelClass();
        test1.setName("testName1");
        ReflectionUtils.setFieldValue(test1,"name",expectedNewFieldValue1);

        Assert.assertEquals("setFieldToNull - field must be set to correct value - first try", test1.getName(), expectedNewFieldValue1);

        ReflectionUtils.setFieldValue(test1,"name", expectedNewFieldValue2);

        Assert.assertEquals("setFieldToNull - field must be set to correct value - second try", test1.getName(), expectedNewFieldValue2);
    }

    @Test()
    public void testSetFieldValueNoException() {
        System.out.println("TESTS - test SetFieldValueNoException to set a non-existing field to not throw an exception, run twice to ensure caching does not cause issues");

        String expectedNewFieldValue1 = "nameybaydray1";
        String expectedNewFieldValue2 = "nameybaydray2";

        TestModelClass test1 = new TestModelClass();

        boolean exceptionThrown = false;
        try {
            ReflectionUtils.setFieldValueNoException(test1, "nameNoTExists", expectedNewFieldValue1);
        } catch(Exception e) {
            exceptionThrown = true;
        }

        Assert.assertFalse("setFieldToValueNoException - no exception must be thrown - first try", exceptionThrown);

        boolean exceptionThrown2 = false;
        try {
            ReflectionUtils.setFieldValueNoException(test1, "_name_", expectedNewFieldValue1);
        } catch(Exception e) {
            exceptionThrown2 = true;
        }

        Assert.assertFalse("setFieldToValueNoException - no exception must be thrown - first try", exceptionThrown2);
    }

    @Test()
    public void testGetFieldValue() throws NoSuchFieldException, IllegalAccessException {
        System.out.println("TESTS - Get field via reflection twice, the second time is to test that caching does not cause issues");

        String expectedNewFieldValue1 = "nameybaydray155555";
        String expectedNewFieldValue2 = "nameybaydray2555555";
        TestModelClass test1 = new TestModelClass();

        test1.setName(expectedNewFieldValue1);
        Assert.assertEquals("getFieldValue - field returned must be correct value - first try", ReflectionUtils.getFieldValue("name",test1), expectedNewFieldValue1);

        test1.setName(expectedNewFieldValue2);
        Assert.assertEquals("getFieldValue - field returned must be correct value - second try", ReflectionUtils.getFieldValue("name",test1), expectedNewFieldValue2);
    }

    @Test()
    public void testGetClassesInPackage() throws IOException, ClassNotFoundException {
        System.out.println("TESTS - test get classes from package name");

        List<Class<?>> classes = ReflectionUtils.getClasses("org.greatgamesonly.shared.opensource.utils.reflectionutils");

        Assert.assertTrue("TestModelClass class must be returned in retrieved classes", classes.contains(TestModelClass.class));
    }

    @Test()
    public void testGetPublicConstantsInClass() throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        System.out.println("TESTS - test get public constants from class");

        List<Object> constantObjects = ReflectionUtils.getAllConstantValuesInClass(TestModelClass.class);

        Assert.assertTrue("TestModelClass class must be returned in retrieved classes", constantObjects.stream().anyMatch(constant -> constant.equals("test_constant_value")));
    }

    @Test()
    public void testGetClassFieldsOfType_positiveMatch() {
        System.out.println("TESTS - test getClassFieldsOfType - positive match");
        Field[] stringFields = ReflectionUtils.getClassFieldsOfType(TestModelClass.class, String.class);
        Assert.assertTrue("There must be at least one String field", stringFields.length > 0 && Arrays.stream(stringFields).anyMatch(field -> field.getName().equals("name")));
    }

    @Test()
    public void testGetClassFieldsOfType_negativeMatch() {
        System.out.println("TESTS - test getClassFieldsOfType - negative match");
        Field[] integerFields = ReflectionUtils.getClassFieldsOfType(TestModelClass.class, Transport.class);
        Assert.assertTrue("There should be no Transport fields", integerFields.length == 0);
    }

    @Test()
    public void testGetObjectFieldValuesOfType_positiveMatch() throws NoSuchFieldException, IllegalAccessException {
        System.out.println("TESTS - test getObjectFieldValuesOfType - positive match");

        TestModelClass testModel = new TestModelClass();
        testModel.setName("testName");
        testModel.setDescription("testDescription");

        List<String> stringFieldValues = ReflectionUtils.getObjectFieldValuesOfType(testModel, String.class);

        Assert.assertTrue("There must be at least two string values returned", stringFieldValues.size() >= 2);
        Assert.assertTrue("Returned values must include testName", stringFieldValues.contains("testName"));
        Assert.assertTrue("Returned values must include testDescription", stringFieldValues.contains("testDescription"));
    }

    @Test()
    public void testGetObjectFieldValuesOfType_negativeMatch() throws NoSuchFieldException, IllegalAccessException {
        System.out.println("TESTS - test getObjectFieldValuesOfType - negative match");

        TestModelClass testModel = new TestModelClass();
        testModel.setName("testName");
        testModel.setDescription("testDescription");

        List<Integer> integerFieldValues = ReflectionUtils.getObjectFieldValuesOfType(testModel, Integer.class);

        Assert.assertTrue("There should be no Integer field values", integerFieldValues.isEmpty());
    }

    @AfterClass
    public static void PostTestClassRun() {
        System.out.println("TESTS - CLEAN UP DATA");
        System.out.println("TESTS - END");
    }
}
