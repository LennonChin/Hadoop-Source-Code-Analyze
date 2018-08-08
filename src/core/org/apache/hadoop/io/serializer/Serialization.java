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

package org.apache.hadoop.io.serializer;

/**
 * <p>
 * Encapsulates a {@link Serializer}/{@link Deserializer} pair.
 * </p>
 * @param <T>
 */
public interface Serialization<T> {
  
  /**
   * Allows clients to test whether this {@link Serialization}
   * supports the given class.
   *
   * 客户端用于判断序列化实现是否支持该类对象
   *
   */
  boolean accept(Class<?> c);
  
  /**
   * @return a {@link Serializer} for the given class.
   *
   * 获得用于序列化的Serializer实现
   *
   */
  Serializer<T> getSerializer(Class<T> c);

  /**
   * @return a {@link Deserializer} for the given class.
   *
   * 获得用于反序列化对象的Deserizlizer实现
   *
   */
  Deserializer<T> getDeserializer(Class<T> c);
}
