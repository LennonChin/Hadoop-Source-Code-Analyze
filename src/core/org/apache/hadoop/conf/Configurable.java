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
 *
 * 查看 {@link org.apache.hadoop.mapred.SequenceFileInputFilter.RegexFilter} 类
 *
 * 查看 {@link org.apache.hadoop.util.ReflectionUtils#setConf } 类，
 * 在这个类中，{@link org.apache.hadoop.util.ReflectionUtils#newInstance } 方法在反射创建好对象后会调用setConf方法
 * 而在setConf方法中会判断当前类是否实现了Configurable接口，如果实现了就调用当前类的setConf方法
 */

package org.apache.hadoop.conf;

/** Something that may be configured with a {@link Configuration}. */
public interface Configurable {

  /**
   * Set the configuration to be used by this object.
   *
   * 对象创建后就应该调用此方法为对象提供进一步的初始化工作
   * */
  void setConf(Configuration conf);

  /** Return the configuration used by this object. */
  Configuration getConf();
}
