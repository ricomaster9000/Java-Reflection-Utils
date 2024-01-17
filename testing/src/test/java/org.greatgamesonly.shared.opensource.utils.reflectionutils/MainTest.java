package org.greatgamesonly.shared.opensource.utils.reflectionutils;


import org.greatgamesonly.opensource.utils.reflectionutils.ReflectionUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
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

    @AfterClass
    public static void PostTestClassRun() {
        System.out.println("TESTS - CLEAN UP DATA");
        System.out.println("TESTS - END");
    }
}
