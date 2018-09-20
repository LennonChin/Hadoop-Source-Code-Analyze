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

package org.apache.hadoop.hdfs.server.protocol;

import java.io.*;

import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.ipc.VersionedProtocol;

import org.apache.hadoop.security.KerberosInfo;

/**********************************************************************
 * Protocol that a DFS datanode uses to communicate with the NameNode.
 * It's used to upload current load information and block reports.
 *
 * The only way a NameNode can communicate with a DataNode is by
 * returning values from these functions.
 * 数据节点与名字节点间的接口
 * 数据节点向名称节点汇报信息等
 **********************************************************************/
@KerberosInfo(
    serverPrincipal = DFSConfigKeys.DFS_NAMENODE_USER_NAME_KEY, 
    clientPrincipal = DFSConfigKeys.DFS_DATANODE_USER_NAME_KEY)
public interface DatanodeProtocol extends VersionedProtocol {
  /**
   * 25: Serialized format of BlockTokenIdentifier changed to contain
   *     multiple blocks within a single BlockTokenIdentifier
   *     
   *     (bumped to 25 to bring in line with trunk)
   */
  public static final long versionID = 25L;
  
  // error code
  final static int NOTIFY = 0;
  final static int DISK_ERROR = 1; // there are still valid volumes on DN
  final static int INVALID_BLOCK = 2;
  final static int FATAL_DISK_ERROR = 3; // no valid volumes left on DN

  /**
   * Determines actions that data node should perform 
   * when receiving a datanode command.
   * 命令编号的常量表示
   */
  // 未定义
  final static int DNA_UNKNOWN = 0;    // unknown action
  // 数据块传输
  final static int DNA_TRANSFER = 1;   // transfer blocks to another datanode
  // 数据块删除
  final static int DNA_INVALIDATE = 2; // invalidate blocks
  // 关闭数据节点
  final static int DNA_SHUTDOWN = 3;   // shutdown node
  // 数据节点重新注册
  final static int DNA_REGISTER = 4;   // re-register
  // 提交上一次升级
  final static int DNA_FINALIZE = 5;   // finalize previous upgrade
  // 数据块恢复
  final static int DNA_RECOVERBLOCK = 6;  // request a block recovery
  // 安全相关
  final static int DNA_ACCESSKEYUPDATE = 7;  // update access key
  // 更新平衡器可用带宽
  final static int DNA_BALANCERBANDWIDTHUPDATE = 8; // update balancer bandwidth
  
  /**
   * 数据节点主要操作（2）：
   * 注册数据节点
   * 参考：
   * - 步骤一：{@link #versionRequest}
   * - 步骤二：{@link #register}
   * - 步骤三：{@link #blockReport}
   * - 步骤四：{@link #sendHeartbeat}
   *
   * @see org.apache.hadoop.hdfs.server.datanode.DataNode#dnRegistration
   * @see org.apache.hadoop.hdfs.server.namenode.FSNamesystem#registerDatanode(DatanodeRegistration)
   * 
   * @return updated {@link org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration}, which contains 
   * new storageID if the datanode did not have one and
   * registration ID for further communication.
   */
  public DatanodeRegistration register(DatanodeRegistration registration
                                       ) throws IOException;
  /**
   * 数据节点主要操作（4）：
   * 用于发送心跳信息，包括：
   * @param capacity 数据节点容量
   * @param dfsUsed 已使用容量
   * @param remaining 剩余容量
   * @param xmitsInProgress 正在写文件数据的连接数
   * @param xceiverCount 读写数据使用的线程数
   * 参考：
   *    1. {@link #versionRequest}
   *    2. {@link #register}
   *    3. {@link #blockReport}
   *    4. {@link #sendHeartbeat}
   * sendHeartbeat() tells the NameNode that the DataNode is still
   * alive and well.  Includes some status info, too. 
   * It also gives the NameNode a chance to return 
   * an array of "DatanodeCommand" objects.
   * A DatanodeCommand tells the DataNode to invalidate local block(s), 
   * or to copy them to other DataNodes, etc.
   */
  public DatanodeCommand[] sendHeartbeat(DatanodeRegistration registration,
                                       long capacity,
                                       long dfsUsed, long remaining,
                                       int xmitsInProgress,
                                       int xceiverCount) throws IOException;
  
  /**
   * 数据节点主要操作（3）：
   * 上报数据节点所管理的全部数据块信息，
   * 帮助名字节点建立数据块和数据节点的映射关系，
   * 一般只在数据节点启动的时候使用
   * 参考：
   *    1. {@link #versionRequest}
   *    2. {@link #register}
   *    3. {@link #blockReport}
   *    4. {@link #sendHeartbeat}
   * blockReport() tells the NameNode about all the locally-stored blocks.
   * The NameNode returns an array of Blocks that have become obsolete
   * and should be deleted.  This function is meant to upload *all*
   * the locally-stored blocks.  It's invoked upon startup and then
   * infrequently afterwards.
   * @param registration
   * @param blocks - the block list as an array of longs.
   *     Each block is represented as 2 longs.
   *     This is done instead of Block[] to reduce memory used by block reports.
   *     
   * @return - the next command for DN to process.
   * @throws IOException
   */
  public DatanodeCommand blockReport(DatanodeRegistration registration,
                                     long[] blocks) throws IOException;
  
  /**
   * blocksBeingWrittenReport() tells the NameNode about the blocks-being-
   * written information
   * 
   * @param registration
   * @param blocks
   * @throws IOException
   */
  public void blocksBeingWrittenReport(DatanodeRegistration registration,
      long[] blocks) throws IOException;
    
  /**
   * blockReceived() allows the DataNode to tell the NameNode about
   * recently-received block data, with a hint for pereferred replica
   * to be deleted when there is any excessive blocks.
   * For example, whenever client code
   * writes a new Block here, or another DataNode copies a Block to
   * this DataNode, it will call blockReceived().
   * 数据节点用于向名字节点报告自己已经完整接收了一些数据块
   * 接收数据块的来源可以是客户端写入的数据，也可以是其他节点的复制数据
   * @param delHints 这个参数由平衡器使用
   */
  public void blockReceived(DatanodeRegistration registration,
                            Block blocks[],
                            String[] delHints) throws IOException;

  /**
   * errorReport() tells the NameNode about something that has gone
   * awry.  Useful for debugging.
   */
  public void errorReport(DatanodeRegistration registration,
                          int errorCode, 
                          String msg) throws IOException;
  
  /**
   * 数据节点主要操作（1）：
   * 用于数据节点与名字节点握手
   * 会检查buildVersion版本号，版本号不同的节点会被退出
   * 参考：
   *    1. {@link #versionRequest}
   *    2. {@link #register}
   *    3. {@link #blockReport}
   *    4. {@link #sendHeartbeat}
   */
  public NamespaceInfo versionRequest() throws IOException;

  /**
   * This is a very general way to send a command to the name-node during
   * distributed upgrade process.
   * 
   * The generosity is because the variety of upgrade commands is unpredictable.
   * The reply from the name-node is also received in the form of an upgrade 
   * command. 
   * 
   * @return a reply in the form of an upgrade command
   */
  UpgradeCommand processUpgradeCommand(UpgradeCommand comm) throws IOException;
  
  /**
   * same as {@link org.apache.hadoop.hdfs.protocol.ClientProtocol#reportBadBlocks(LocatedBlock[])}
   * 报告坏数据块
   * LocatedBlock对象中提供了数据块及其位置信息
   */
  public void reportBadBlocks(LocatedBlock[] blocks) throws IOException;
  
  /**
   * Get the next GenerationStamp to be associated with the specified
   * block.
   * 用于获取一个新的数据块版本号
   * 
   * @param block block
   * @param fromNN if it is for lease recovery initiated by NameNode
   * @return a new generation stamp
   */
  public long nextGenerationStamp(Block block, boolean fromNN) throws IOException;
  
  /**
   * Commit block synchronization in lease recovery
   * 用于告知数据块恢复的执行情况
   * @param block 进行恢复的数据块
   * @param newgenerationstamp 通过 {@link #nextGenerationStamp} 获取的新的版本号
   * @param newlength 数据块恢复后的新长度
   * @param closeFile 所属文件是否由名字节点关闭
   * @param deleteblock 是否删除名字节点上的数据块信息
   * @param newtargets 成功参与数据块恢复的数据节点列表
   * @throws IOException
   */
  public void commitBlockSynchronization(Block block,
      long newgenerationstamp, long newlength,
      boolean closeFile, boolean deleteblock, DatanodeID[] newtargets
      ) throws IOException;
}
