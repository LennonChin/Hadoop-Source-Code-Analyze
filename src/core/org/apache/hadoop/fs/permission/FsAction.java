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
package org.apache.hadoop.fs.permission;

/**
 * File system actions, e.g. read, write, etc.
 *
 * 这个枚举模拟了Linux下的POSIX风格的文件权限标识方法
 */
public enum FsAction {
  // POSIX style
  NONE("---"),            // 0 -> 000
  EXECUTE("--x"),         // 1 -> 001
  WRITE("-w-"),           // 2 -> 010
  WRITE_EXECUTE("-wx"),   // 3 -> 011
  READ("r--"),            // 4 -> 100
  READ_EXECUTE("r-x"),    // 5 -> 101
  READ_WRITE("rw-"),      // 6 -> 110
  ALL("rwx");             // 7 -> 111

  /** Retain reference to value array. */
  private final static FsAction[] vals = values();

  /** Symbolic representation */
  public final String SYMBOL;

  private FsAction(String s) {
    SYMBOL = s;
  }

  /**
   * Return true if this action implies that action.
   * @param that
   */
  public boolean implies(FsAction that) {
    if (that != null) {
      /**
       * 通过位运算来判断当前权限是否包含that表示的权限，如：
       * 当前权限READ_WRITE（110），that权限READ（100），有计算方式：
       * 110 & 100 = 100 == 100 ，即当前权限包含that表示的权限
       * 当前权限READ_WRITE（110），that权限READ_EXECUTE（101），有计算方式：
       * 110 & 101 = 111 != 100 ，即当前权限不包含that表示的权限
       */
      return (ordinal() & that.ordinal()) == that.ordinal();
    }
    return false;
  }

  /** AND operation. */
  public FsAction and(FsAction that) {
    return vals[ordinal() & that.ordinal()];
  }
  /** OR operation. */
  public FsAction or(FsAction that) {
    return vals[ordinal() | that.ordinal()];
  }
  /** NOT operation. */
  public FsAction not() {
    return vals[7 - ordinal()];
  }
}
