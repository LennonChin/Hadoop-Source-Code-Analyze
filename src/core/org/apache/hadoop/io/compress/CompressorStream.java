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

import org.apache.hadoop.io.compress.CompressionOutputStream;
import org.apache.hadoop.io.compress.Compressor;

public class CompressorStream extends CompressionOutputStream {
  protected Compressor compressor;
  protected byte[] buffer;
  protected boolean closed = false;
  
  /**
   * 该类主要用于压缩数据，需要传入压缩器和输出流
   *
   * 数据通过write方法写出，在写出之前会先进行压缩，然后使用输出流进行输出
   *
   * @param out
   * @param compressor
   * @param bufferSize
   */
  public CompressorStream(OutputStream out, Compressor compressor, int bufferSize) {
    super(out);

    // 要求out和compressor都不为空，且bufferSize大于0
    if (out == null || compressor == null) {
      throw new NullPointerException();
    } else if (bufferSize <= 0) {
      throw new IllegalArgumentException("Illegal bufferSize");
    }

    this.compressor = compressor;
    buffer = new byte[bufferSize];
  }

  public CompressorStream(OutputStream out, Compressor compressor) {
    // 默认的bufferSize是512字节
    this(out, compressor, 512);
  }
  
  /**
   * Allow derived classes to directly set the underlying stream.
   *
   * 允许衍生类直接设置底层流，这个方法是protected保护的，只能被本包或子类调用
   * 
   * @param out Underlying output stream.
   */
  protected CompressorStream(OutputStream out) {
    super(out);
  }

  public void write(byte[] b, int off, int len) throws IOException {
    // Sanity checks
    if (compressor.finished()) {
      throw new IOException("write beyond end of stream");
    }
    // b.length - (off + len) 表示超出了b的长度
    if ((off | len | (off + len) | (b.length - (off + len))) < 0) {
      throw new IndexOutOfBoundsException();
    } else if (len == 0) {
      return;
    }

    compressor.setInput(b, off, len);
    while (!compressor.needsInput()) {
      compress();
    }
  }

  protected void compress() throws IOException {
    // 将流压缩到buffer中，如果压缩的长度大于0，将其使用out直接写出
    int len = compressor.compress(buffer, 0, buffer.length);
    if (len > 0) {
      out.write(buffer, 0, len);
    }
  }

  public void finish() throws IOException {
    if (!compressor.finished()) {
      compressor.finish();
      while (!compressor.finished()) {
        compress();
      }
    }
  }

  public void resetState() throws IOException {
    compressor.reset();
  }
  
  public void close() throws IOException {
    if (!closed) {
      finish();
      out.close();
      closed = true;
    }
  }

  private byte[] oneByte = new byte[1];
  public void write(int b) throws IOException {
    // 去除高位，保留8位（一个字节）
    oneByte[0] = (byte)(b & 0xff);
    // 使用write写出
    write(oneByte, 0, oneByte.length);
  }

}
