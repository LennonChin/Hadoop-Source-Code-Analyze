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

/****************************************************************
 * FSInputStream is a generic old InputStream with a little bit
 * of RAF-style seek ability.
 *
 *****************************************************************/
public abstract class FSInputStream extends InputStream
    implements Seekable, PositionedReadable {
  /**
   * Seek to the given offset from the start of the file.
   * The next read() will be from that location.  Can't
   * seek past the end of the file.
   */
  public abstract void seek(long pos) throws IOException;

  /**
   * Return the current offset from the start of the file
   */
  public abstract long getPos() throws IOException;

  /**
   * Seeks a different copy of the data.  Returns true if 
   * found a new source, false otherwise.
   */
  public abstract boolean seekToNewSource(long targetPos) throws IOException;
  
  /**
   * 这个方法中通过 {@link Seekable#seek} 实现了 {@link PositionedReadable#read} 方法
   * @param position
   * @param buffer
   * @param offset
   * @param length
   * @return
   * @throws IOException
   */
  public int read(long position, byte[] buffer, int offset, int length)
    throws IOException {
    /**
     * 由于PositionedReadable中的方法是线程安全的，所以在这里需要使用同步锁
     * 以保证在操作过程中不会有其他线程修改了记录好的旧的position
     */
    synchronized (this) {
      // 记录旧的position（即流的位置）
      long oldPos = getPos();
      int nread = -1;
      try {
        // 设置到新的position
        seek(position);
        // 进行读取
        nread = read(buffer, offset, length);
      } finally {
        // 将position复位为旧的position，保持position不被改变
        seek(oldPos);
      }
      return nread;
    }
  }
    
  public void readFully(long position, byte[] buffer, int offset, int length)
    throws IOException {
    int nread = 0;
    while (nread < length) {
      int nbytes = read(position+nread, buffer, offset+nread, length-nread);
      if (nbytes < 0) {
        throw new EOFException("End of file reached before reading fully.");
      }
      nread += nbytes;
    }
  }
    
  public void readFully(long position, byte[] buffer)
    throws IOException {
    readFully(position, buffer, 0, buffer.length);
  }
}
