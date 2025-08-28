
package com.google.cloud.bigtable.jdbc.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ReflectorTest {

  public static class TestClass implements Serializable {
    private final String value;

    public TestClass(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      TestClass testClass = (TestClass) obj;
      return value.equals(testClass.value);
    }
  }

  @Test
  public void testCreateVerifiedInstance() {
    TestClass instance =
        Reflector.createVerifiedInstance(
            "com.google.cloud.bigtable.jdbc.util.ReflectorTest$TestClass",
            TestClass.class,
            "test");
    assertNotNull(instance);
    assertEquals("test", instance.getValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateVerifiedInstanceWrongClass() {
    Reflector.createVerifiedInstance(
        "com.google.cloud.bigtable.jdbc.util.ReflectorTest$TestClass",
        String.class,
        "test");
  }

  @Test
  public void testVerifySerialization() {
    TestClass original = new TestClass("test");
    TestClass deserialized = Reflector.verifySerialization(original);
    assertEquals(original, deserialized);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateVerifiedInstanceClassNotFound() {
    Reflector.createVerifiedInstance("non.existent.class", Object.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateVerifiedInstanceNoSuchMethod() {
    Reflector.createVerifiedInstance(
        "com.google.cloud.bigtable.jdbc.util.ReflectorTest$TestClass", TestClass.class, 123);
  }

  public abstract static class AbstractTestClass implements Serializable {
    public AbstractTestClass() {}
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateVerifiedInstanceInstantiationException() {
    Reflector.createVerifiedInstance(
        "com.google.cloud.bigtable.jdbc.util.ReflectorTest$AbstractTestClass",
        AbstractTestClass.class);
  }

  public static class PrivateConstructorTestClass implements Serializable {
    private PrivateConstructorTestClass() {}
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateVerifiedInstanceIllegalAccessException() {
    Reflector.createVerifiedInstance(
        "com.google.cloud.bigtable.jdbc.util.ReflectorTest$PrivateConstructorTestClass",
        PrivateConstructorTestClass.class);
  }

  public static class ExceptionInConstructorTestClass implements Serializable {
    public ExceptionInConstructorTestClass() {
      throw new RuntimeException("test exception");
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateVerifiedInstanceInvocationTargetException() {
    Reflector.createVerifiedInstance(
        "com.google.cloud.bigtable.jdbc.util.ReflectorTest$ExceptionInConstructorTestClass",
        ExceptionInConstructorTestClass.class);
  }

  public static class NotSerializableClass {}

  @Test(expected = IllegalArgumentException.class)
  public void testVerifySerializationNotSerializable() {
    Reflector.verifySerialization(new NotSerializableClass());
  }
}
