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

import org.apache.hadoop.conf.Configuration;

/**
 * Specification of a stream-based 'compressor' which can be  
 * plugged into a {@link CompressionOutputStream} to compress data.
 * This is modelled after {@link java.util.zip.Deflater}
 * 
 */
public interface Compressor {
  /**
   * Sets input data for compression. 
   * This should be called whenever #needsInput() returns 
   * <code>true</code> indicating that more input data is required.
   *
   * Compressor通过这个方法接受数据，只有在 #needsInput() 返回值为true的时候才被调用
   * 
   * @param b Input data
   * @param off Start offset
   * @param len Length
   */
  public void setInput(byte[] b, int off, int len);
  
  /**
   * Returns true if the input data buffer is empty and 
   * #setInput() should be called to provide more input.
   *
   * 可以通过这个方法判断缓冲区是否写满
   * 
   * @return <code>true</code> if the input data buffer is empty and 
   * #setInput() should be called in order to provide more input.
   */
  public boolean needsInput();
  
  /**
   * Sets preset dictionary for compression. A preset dictionary 
   * is used when the history buffer can be predetermined. 
   *
   * @param b Dictionary data bytes
   * @param off Start offset
   * @param len Length
   */
  public void setDictionary(byte[] b, int off, int len);

  /**
   * Return number of uncompressed bytes input so far.
   */
  public long getBytesRead();

  /**
   * Return number of compressed bytes output so far.
   */
  public long getBytesWritten();

  /**
   * When called, indicates that compression should end
   * with the current contents of the input buffer.
   *
   * 这个方法用于使用者调用，以告知压缩器所有数据已经写入
   *
   */
  public void finish();
  
  /**
   * Returns true if the end of the compressed 
   * data output stream has been reached.
   *
   * 通过这个方法判断压缩器中是否还有未读取的压缩数据
   *
   * @return <code>true</code> if the end of the compressed
   * data output stream has been reached.
   */
  public boolean finished();
  
  /**
   * Fills specified buffer with compressed data. Returns actual number
   * of bytes of compressed data. A return value of 0 indicates that
   * needsInput() should be called in order to determine if more input
   * data is required.
   *
   * 这个方法用于压缩的数据，返回实际压缩的字节数
   * 
   * @param b Buffer for the compressed data 装载压缩后数据的缓冲区
   * @param off Start offset of the data 开始压缩的偏移量
   * @param len Size of the buffer 压缩的长度
   * @return The actual number of bytes of compressed data. 实际压缩数据的字节数
   */
  public int compress(byte[] b, int off, int len) throws IOException;
  
  /**
   * Resets compressor so that a new set of input data can be processed.
   */
  public void reset();
  
  /**
   * Closes the compressor and discards any unprocessed input.
   */
  public void end();

  /**
   * Prepare the compressor to be used in a new stream with settings defined in
   * the given Configuration
   * 
   * @param conf Configuration from which new setting are fetched
   */
  public void reinit(Configuration conf);
}
