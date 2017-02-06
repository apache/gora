/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gora.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.apache.avro.specific.SpecificRecordBuilderBase;
import org.apache.gora.persistency.impl.PersistentBase;

/**
 * Utility methods related to reflection
 */
public class ReflectionUtils {

  public static Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
  public static Object[] EMPTY_OBJECT_ARRAY = new Object[0];

  /**
   * Returns the empty argument constructor of the class.
   *
   * @param clazz the class reference of given type <code>T</code>.
   * @param <T> class type variable.
   * @return constructor for the given class type <code>T</code>.
   * @throws SecurityException if the caller's class loader is not the same as the
   *          class loader of above class.
   * @throws NoSuchMethodException default construct cannot by found for given class type.
   */
  public static<T> Constructor<T> getConstructor(Class<T> clazz) 
      throws SecurityException, NoSuchMethodException {
    if(clazz == null) {
      throw new IllegalArgumentException("class cannot be null");
    }
    Constructor<T> cons = clazz.getConstructor(EMPTY_CLASS_ARRAY);
    cons.setAccessible(true);
    return cons;
  }

  /**
   * Returns whether the class defines an empty argument constructor.
   *
   * @param clazz class reference of given type <code>T</code>.
   * @return boolean indicating constructor for the given class type <code>T</code> exist.
   * @throws SecurityException if the caller's class loader is not the same as the
   *          class loader of above class.
   * @throws NoSuchMethodException default construct cannot by found for given class type.
   */
  public static boolean hasConstructor(Class<?> clazz) 
      throws SecurityException, NoSuchMethodException {
    if(clazz == null) {
      throw new IllegalArgumentException("class cannot be null");
    }
    Constructor<?>[] consts = clazz.getConstructors();

    boolean found = false;
    for(Constructor<?> cons : consts) {
      if(cons.getParameterTypes().length == 0) {
        found = true;
      }
    }

    return found;
  }

  /**
   * Constructs a new instance of the class using the no-arg constructor.
   *
   * @param clazz the class of the object.
   * @param <T> class type variable.
   * @return a new instance of the object.
   * @throws SecurityException if the caller's class loader is not the same as the
   *          class loader of above class.
   * @throws IllegalArgumentException this will not be thrown since <code>field.get(obj)</code> passing obj is null
   *         since the field is a static class level variable inside the class.
   * @throws IllegalAccessException if the field is inaccessible due to java language access control.
   * @throws InstantiationException could not be instantiated from the given constructor.
   * @throws NoSuchMethodException default construct cannot by found for given class type.
   * @throws InvocationTargetException if the underlying constructor throws an exception.
   */
  public static <T> T newInstance(Class<T> clazz) 
      throws InstantiationException, IllegalAccessException, 
      SecurityException, NoSuchMethodException, IllegalArgumentException, 
      InvocationTargetException {

    Constructor<T> cons = getConstructor(clazz);

    return cons.newInstance(EMPTY_OBJECT_ARRAY);
  }

  /**
   * Constructs a new instance of the class using the no-arg constructor.
   *
   * @param classStr the class name of the object.
   * @return a new instance of the object.
   * @throws SecurityException if the caller's class loader is not the same as the
   *          class loader of above class.
   * @throws IllegalArgumentException this will not be thrown since <code>field.get(obj)</code> passing obj is null.
   *         since the field is a static class level variable inside the class.
   * @throws IllegalAccessException if the field is inaccessible due to java language access control.
   * @throws ClassNotFoundException class definition cannot be found for the class type.
   * @throws InstantiationException could not be instantiated from the given constructor.
   * @throws NoSuchMethodException default construct cannot by found for given class type.
   * @throws InvocationTargetException if the underlying constructor throws an exception.
   */
  public static Object newInstance(String classStr) 
      throws InstantiationException, IllegalAccessException, 
      ClassNotFoundException, SecurityException, IllegalArgumentException, 
      NoSuchMethodException, InvocationTargetException {
    if(classStr == null) {
      throw new IllegalArgumentException("class cannot be null");
    }
    Class<?> clazz = ClassLoadingUtils.loadClass(classStr);
    return newInstance(clazz);
  }

  /**
   * Returns the value of a named static field.
   *
   * @param clazz the class of the object.
   * @param fieldName field name of the instance which value is required.
   * @return a new instance of the object.
   * @throws SecurityException if the caller's class loader is not the same as the
   *          class loader of above class.
   * @throws NoSuchFieldException if a field with the specified name is not found.
   * @throws IllegalArgumentException this will not be thrown since <code>field.get(obj)</code> passing obj is null
   *         since the field is a static class level variable inside the class.
   * @throws IllegalAccessException if the field is inaccessible due to java language access control.
   */
  public static Object getStaticField(Class<?> clazz, String fieldName) 
      throws IllegalArgumentException, SecurityException,
      IllegalAccessException, NoSuchFieldException {

    return clazz.getField(fieldName).get(null);
  }

  @SuppressWarnings("unchecked")
  public static <T extends PersistentBase> SpecificRecordBuilderBase<T> classBuilder(Class<T> clazz) 
      throws SecurityException, NoSuchMethodException, IllegalArgumentException, 
      IllegalAccessException, InvocationTargetException {
    return (SpecificRecordBuilderBase<T>) clazz.getMethod("newBuilder").invoke(null);
  }

}
