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
package org.apache.hadoop.fs;

import java.io.*;

/**
 * Utility that wraps a {@link OutputStream} in a {@link DataOutputStream},
 * buffers output through a {@link BufferedOutputStream} and creates a checksum
 * file.
 *
 * 不支持随机写，即不能在文件中重新定位写位置，覆盖文件原有内容
 * 但可以使用 {@link #getPos()} 来获取当前流的写位置，这个方法是通过内部类 {@link PositionCache} 实现的
 * */
public class FSDataOutputStream extends DataOutputStream implements Syncable {
  private OutputStream wrappedStream;

  private static class PositionCache extends FilterOutputStream {
    // 读写操作的统计信息
    private FileSystem.Statistics statistics;
    long position;

    public PositionCache(OutputStream out, 
                         FileSystem.Statistics stats,
                         long pos) throws IOException {
      super(out);
      statistics = stats;
      position = pos;
    }

    public void write(int b) throws IOException {
      out.write(b);
      // 跟踪流的位置
      position++;
      if (statistics != null) {
        statistics.incrementBytesWritten(1);
      }
    }
    
    public void write(byte b[], int off, int len) throws IOException {
      out.write(b, off, len);
      // 跟踪流的位置
      position += len;                            // update position
      if (statistics != null) {
        statistics.incrementBytesWritten(len);
      }
    }
      
    public long getPos() throws IOException {
      // 直接返回在写操作中实时保证更新的流的位置
      return position;                            // return cached position
    }
    
    public void close() throws IOException {
      out.close();
    }
  }

  @Deprecated
  public FSDataOutputStream(OutputStream out) throws IOException {
    this(out, null);
  }

  // 构造方法表明，我们需要提供一个具体的能够发送数据的流来构造FSDataOutputStream
  public FSDataOutputStream(OutputStream out, FileSystem.Statistics stats)
    throws IOException {
    this(out, stats, 0);
  }

  public FSDataOutputStream(OutputStream out, FileSystem.Statistics stats,
                            long startPosition) throws IOException {
    // 在父类中这个PositionCache对象会被记录在out属性中
    super(new PositionCache(out, stats, startPosition));
    // 然后用wrappedStream来引用这个out
    wrappedStream = out;
  }
  
  public long getPos() throws IOException {
    /**
     * 直接调用 {@link PositionCache#getPos()} 返回实时更新的流的写位置
     */
    return ((PositionCache)out).getPos();
  }

  public void close() throws IOException {
    out.close();         // This invokes PositionCache.close()
  }

  // Returns the underlying output stream. This is used by unit tests.
  public OutputStream getWrappedStream() {
    return wrappedStream;
  }

  /**
   * {@inheritDoc}
   *
   * 用于将流中保存的数据同步到设备中
   * */
  public void sync() throws IOException {
    if (wrappedStream instanceof Syncable) {
      ((Syncable)wrappedStream).sync();
    }
  }
}
