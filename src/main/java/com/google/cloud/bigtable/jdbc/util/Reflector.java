/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.bigtable.jdbc.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.function.IntFunction;

public class Reflector {
  public static <T> T createVerifiedInstance(
    String fullyQualifiedClassName, Class<T> requiredClass, Object... constructorArgs) {
    try {
      Class<?> clazz = Class.forName(fullyQualifiedClassName);
      Object result =
        clazz
          .getDeclaredConstructor(
            Arrays.stream(constructorArgs)
              .map(Object::getClass)
              .toArray((IntFunction<Class<?>[]>) Class[]::new))
          .newInstance(constructorArgs);
      if (!requiredClass.isInstance(result)) {
        throw new IllegalArgumentException(
          String.format(
            "% does not implement %s",
            clazz.getCanonicalName(), requiredClass.getCanonicalName()));
      }
      return (T) result;
    } catch (ClassNotFoundException
             | InstantiationException
             | IllegalAccessException
             | InvocationTargetException
             | NoSuchMethodException e) {
      throw new IllegalArgumentException(
        String.format(
          "Could not instantiate class [%s], implementing %s",
          fullyQualifiedClassName, requiredClass.getCanonicalName()),
        e);
    }
  }

  public static <T> T verifySerialization(T obj) {
    try {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(buffer);
      out.writeObject(obj);
      out.flush();
      out.close();
      ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
      T result = (T) in.readObject();
      in.close();
      return result;
    } catch (IOException | ClassNotFoundException e) {
      throw new IllegalArgumentException(
        "Serialization test of " + obj.getClass().getCanonicalName() + " failed", e);
    }
  }
}
