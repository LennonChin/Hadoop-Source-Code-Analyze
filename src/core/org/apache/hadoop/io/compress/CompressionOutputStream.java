/*
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

package org.apache.hadoop.io.compress;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A compression output stream.
 *
 * {@link CompressorStream} 和 {@link BZip2Codec.BZip2CompressionOutputStream} 是CompressionOutputStream的两个典型实现
 *
 */
public abstract class CompressionOutputStream extends OutputStream {
  /**
   * The output stream to be compressed.
   *
   * 用于输出压缩结果的流
   *
   */
  protected final OutputStream out;
  
  /**
   * Create a compression output stream that writes
   * the compressed bytes to the given stream.
   *
   * 构造方法，压缩流需要外界传入
   *
   * @param out
   */
  protected CompressionOutputStream(OutputStream out) {
    this.out = out;
  }
  
  public void close() throws IOException {
    finish();
    out.close();
  }
  
  public void flush() throws IOException {
    out.flush();
  }
  
  /**
   * Write compressed bytes to the stream.
   * Made abstract to prevent leakage to underlying stream.
   */
  public abstract void write(byte[] b, int off, int len) throws IOException;

  /**
   * Finishes writing compressed data to the output stream 
   * without closing the underlying stream.
   */
  public abstract void finish() throws IOException;
  
  /**
   * Reset the compression to the initial state. 
   * Does not reset the underlying stream.
   */
  public abstract void resetState() throws IOException;

}
