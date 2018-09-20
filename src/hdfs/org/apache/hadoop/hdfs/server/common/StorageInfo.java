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
package org.apache.hadoop.hdfs.server.common;


/**
 * Common class for storage information.
 * 
 * TODO namespaceID should be long and computed as hash(address + port)
 */
public class StorageInfo {
  // 负整数，HDFS存储系统信息结构的版本号
  public int   layoutVersion;  // Version read from the stored file.
  // 存储系统的唯一标示符
  public int   namespaceID;    // namespace id of the storage
  // 该存储系统信息的创建时间
  public long  cTime;          // creation timestamp
  
  public StorageInfo () {
    this(0, 0, 0L);
  }
  
  public StorageInfo(int layoutV, int nsID, long cT) {
    layoutVersion = layoutV;
    namespaceID = nsID;
    cTime = cT;
  }
  
  public StorageInfo(StorageInfo from) {
    setStorageInfo(from);
  }

  public int    getLayoutVersion(){ return layoutVersion; }
  public int    getNamespaceID()  { return namespaceID; }
  public long   getCTime()        { return cTime; }

  public void   setStorageInfo(StorageInfo from) {
    layoutVersion = from.layoutVersion;
    namespaceID = from.namespaceID;
    cTime = from.cTime;
  }
}