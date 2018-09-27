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

package org.apache.hadoop.io;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.util.ReflectionUtils;
import java.util.HashMap;

/** Factories for non-public writables.  Defining a factory permits {@link
 * ObjectWritable} to be able to construct instances of non-public classes. */
public class WritableFactories {
  private static final HashMap<Class, WritableFactory> CLASS_TO_FACTORY =
    new HashMap<Class, WritableFactory>();

  private WritableFactories() {}                  // singleton

  /**
   * Define a factory for a class.
   *
   * 可以查看 {@link org.apache.hadoop.hdfs.protocol.Block} 中静态代码块的实现
   *
   * */
  public static synchronized void setFactory(Class c, WritableFactory factory) {
    CLASS_TO_FACTORY.put(c, factory);
  }

  /** Define a factory for a class. */
  public static synchronized WritableFactory getFactory(Class c) {
    return CLASS_TO_FACTORY.get(c);
  }

  /** Create a new instance of a class with a defined factory. */
  public static Writable newInstance(Class<? extends Writable> c, Configuration conf) {
    // 获取工厂
    WritableFactory factory = WritableFactories.getFactory(c);
    if (factory != null) {
      // 如果工厂不为空，则使用工厂创建对象
      Writable result = factory.newInstance();
      if (result instanceof Configurable) {
        // 设置Configuration
        ((Configurable) result).setConf(conf);
      }
      return result;
    } else {
      // 如果工厂为空，则使用反射创建对象
      return ReflectionUtils.newInstance(c, conf);
    }
  }
  
  /** Create a new instance of a class with a defined factory. */
  public static Writable newInstance(Class<? extends Writable> c) {
    return newInstance(c, null);
  }

}

