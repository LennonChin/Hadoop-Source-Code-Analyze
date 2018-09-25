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
package org.apache.hadoop.hdfs.protocol;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

/**
 * 
 * The Client transfers data to/from datanode using a streaming protocol.
 *
 */
public interface DataTransferProtocol {
  
  
  /** Version for data transfers between clients and datanodes
   * This should change when serialization of DatanodeInfo, not just
   * when protocol changes. It is not very obvious. 
   */
  /**
   * 版本标识
   * Version 18:
   *    Change the block packet ack protocol to include seqno,
   *    numberOfReplies, reply0, reply1, ...
   */
  public static final int DATA_TRANSFER_VERSION = 17;

  // 操作码
  // 写数据块
  public static final byte OP_WRITE_BLOCK = (byte) 80;
  // 读数据块
  public static final byte OP_READ_BLOCK = (byte) 81;
  /**
   * 读元数据
   * @deprecated 从version 15开始，该操作码已不支持
   */
  @Deprecated public static final byte OP_READ_METADATA = (byte) 82;
  // 数据块替换
  public static final byte OP_REPLACE_BLOCK = (byte) 83;
  // 数据块复制
  public static final byte OP_COPY_BLOCK = (byte) 84;
  // 块校验
  public static final byte OP_BLOCK_CHECKSUM = (byte) 85;
  
  public static final int OP_STATUS_SUCCESS = 0;  
  public static final int OP_STATUS_ERROR = 1;  
  public static final int OP_STATUS_ERROR_CHECKSUM = 2;  
  public static final int OP_STATUS_ERROR_INVALID = 3;  
  public static final int OP_STATUS_ERROR_EXISTS = 4;  
  public static final int OP_STATUS_ERROR_ACCESS_TOKEN = 5;
  public static final int OP_STATUS_CHECKSUM_OK = 6;

  /**
   * 管道流确认包，每次写请求会产生多个确认包，每个确认包从最后一个节点依次向前传递
   */
  public static class PipelineAck implements Writable {
    // 该确认对应的写请求数据包，也就是数据包中的顺序号
    private long seqno;
    /**
     * 包含了管道上的各个节点对写请求数据包的处理结果。
     * 如果数据节点顺利地将数据写入磁盘，则结果为DataTransferProtocol.OP_STATUS_SUCCESS，否则为DataTransferProtocol.OP_STATUS_ERROR。
     * 随着确认包逆流而上，该字段的大小会不断增长。
     */
    private short replies[];
    final public static long UNKOWN_SEQNO = -2; 

    /** default constructor **/
    public PipelineAck() {
    }

    /**
     * Constructor
     * @param seqno sequence number
     * @param replies an array of replies
     */
    public PipelineAck(long seqno, short[] replies) {
      this.seqno = seqno;
      this.replies = replies;
    }

    /**
     * Get the sequence number
     * @return the sequence number
     */
    public long getSeqno() {
      return seqno;
    }

    /**
     * Get the number of replies
     * @return the number of replies
     */
    public short getNumOfReplies() {
      return (short)replies.length;
    }

    /**
     * get the ith reply
     * @return the the ith reply
     */
    public short getReply(int i) {
      return replies[i];
    }

    /**
     * Check if this ack contains error status
     * @return true if all statuses are SUCCESS
     */
    public boolean isSuccess() {
      for (short reply : replies) {
        if (reply != OP_STATUS_SUCCESS) {
          return false;
        }
      }
      return true;
    }

    /**** Writable interface ****/
    @Override // Writable
    public void readFields(DataInput in) throws IOException {
      seqno = in.readLong();
      short numOfReplies = in.readShort();
      replies = new short[numOfReplies];
      for (int i=0; i<numOfReplies; i++) {
        replies[i] = in.readShort();
      }
    }

    @Override // Writable
    public void write(DataOutput out) throws IOException {
      //WritableUtils.writeVLong(out, seqno);
      out.writeLong(seqno);
      out.writeShort((short)replies.length);
      for(short reply : replies) {
        out.writeShort(reply);
      }
    }

    @Override //Object
    public String toString() {
      StringBuilder ack = new StringBuilder("Replies for seqno ");
      ack.append( seqno ).append( " are" );
      for(short reply : replies) {
        ack.append(" ");
        if (reply == OP_STATUS_SUCCESS) {
          ack.append("SUCCESS");
        } else {
          ack.append("FAILED");
        }
      }
      return ack.toString();
    }
  }
}
