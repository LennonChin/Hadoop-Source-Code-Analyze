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

import java.io.*;

import org.apache.hadoop.ipc.VersionedProtocol;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.hdfs.protocol.FSConstants.UpgradeAction;
import org.apache.hadoop.hdfs.protocol.HdfsFileStatus;
import org.apache.hadoop.hdfs.server.common.UpgradeStatusReport;
import org.apache.hadoop.fs.permission.*;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.security.KerberosInfo;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenInfo;
import org.apache.hadoop.hdfs.security.token.delegation.DelegationTokenIdentifier;
import org.apache.hadoop.hdfs.security.token.delegation.DelegationTokenSelector;

/**********************************************************************
 * ClientProtocol is used by user code via 
 * {@link org.apache.hadoop.hdfs.DistributedFileSystem} class to communicate 
 * with the NameNode.  User code can manipulate the directory namespace, 
 * as well as open/close file streams, etc.
 * 客户端与名字节点间的接口
 * 错误发生时，客户端需要数据节点配合进行恢复
 * 或者当客户端进行本地文件读优化时，需要通过IPC接口获取一些信息
 **********************************************************************/
@KerberosInfo(
    serverPrincipal = DFSConfigKeys.DFS_NAMENODE_USER_NAME_KEY)
@TokenInfo(DelegationTokenSelector.class)
public interface ClientProtocol extends VersionedProtocol {

  /**
   * Compared to the previous version the following changes have been introduced:
   * (Only the latest change is reflected.
   * The log of historical changes can be retrieved from the svn).
   * 61: Serialized format of BlockTokenIdentifier changed to contain
   *     multiple blocks within a single BlockTokenIdentifier 
   *     
   *     (bumped to 61 to bring in line with trunk)
   */
  public static final long versionID = 61L;
  
  ///////////////////////////////////////
  // File contents
  ///////////////////////////////////////
  /**
   * Get locations of the blocks of the specified file within the specified range.
   * DataNode locations for each block are sorted by
   * the proximity to the client.
   * <p>
   * Return {@link LocatedBlocks} which contains
   * file length, blocks and their locations.
   * DataNode locations for each block are sorted by
   * the distance to the client's address.
   * <p>
   * The client will then have to contact 
   * one of the indicated DataNodes to obtain the actual data.
   * 
   * @param src 指定文件
   * @param offset 数据开始的偏移量
   * @param length 数据长度
   * @return file length and array of blocks with their locations
   * @throws IOException
   * 获取指定文件指定数据区间所在的数据块的信息
   * 由于可能指定的数据区间跨越了多个数据块，所以返回值是一个LocatedBlocks实例
   */
  public LocatedBlocks  getBlockLocations(String src,
                                          long offset,
                                          long length) throws IOException;

  /**
   * Create a new file entry in the namespace.
   * <p>
   * This will create an empty file specified by the source path.
   * The path should reflect a full path originated at the root.
   * The name-node does not have a notion of "current" directory for a client.
   * <p>
   * Once created, the file is visible and available for read to other clients.
   * Although, other clients cannot {@link #delete(String)}, re-create or 
   * {@link #rename(String, String)} it until the file is completed
   * or explicitly as a result of lease expiration.
   * <p>
   * Blocks have a maximum size.  Clients that intend to
   * create multi-block files must also use {@link #addBlock(String, String)}.
   *
   * @param src 创建文件的路径，需要注意的是，必须使用绝对路径
   * @param masked 文件权限
   * @param clientName 当前客户端名称
   * @param overwrite 是否覆盖已有文件
   * @param createParent 是否递归创建目录
   * @param replication 副本数
   * @param blockSize 数据块最大大小
   * 
   * @throws AccessControlException if permission to create file is 
   * denied by the system. As usually on the client side the exception will 
   * be wrapped into {@link org.apache.hadoop.ipc.RemoteException}.
   * @throws QuotaExceededException if the file creation violates 
   *                                any quota restriction
   * @throws IOException if other errors occur.
   * 创建文件
   */
  public void create(String src, 
                     FsPermission masked,
                             String clientName, 
                             boolean overwrite, 
                             boolean createParent,
                             short replication,
                             long blockSize
                             ) throws IOException;

  /**
   * Create a new file entry in the namespace.
   * 
   */
  public void create(String src, 
                     FsPermission masked,
                             String clientName, 
                             boolean overwrite, 
                             short replication,
                             long blockSize
                             ) throws IOException;
  /**
   * Append to the end of the file. 
   * @param src 需要操作的文件的路径
   * @param clientName 当前客户端名称
   * @return information about the last partial block if any.
   * @throws AccessControlException if permission to append file is 
   * denied by the system. As usually on the client side the exception will 
   * be wrapped into {@link org.apache.hadoop.ipc.RemoteException}.
   * Allows appending to an existing file if the server is
   * configured with the parameter dfs.support.append set to true, otherwise
   * throws an IOException.
   * @throws IOException if other errors occur.
   */
  public LocatedBlock append(String src, String clientName) throws IOException;
  
  /**
   * 恢复租约
   * 
   * @param src 需要恢复租约的文件
   * @param clientName 当前客户端名称
   * @return true if the file is already closed
   * @throws IOException
   */
  public boolean recoverLease(String src, String clientName) throws IOException;

  /**
   * 设置指定文件的副本数
   * <p>
   * The NameNode sets replication to the new value and returns.
   * The actual block replication is not expected to be performed during  
   * this method call. The blocks will be populated or removed in the 
   * background as the result of the routine block maintenance procedures.
   * 
   * @param src file name
   * @param replication new replication
   * @throws IOException
   * @return true if successful;
   *         false if file does not exist or is a directory
   */
  public boolean setReplication(String src, 
                                short replication
                                ) throws IOException;

  /**
   * 设置指定的已存在文件或目录的权限信息
   */
  public void setPermission(String src, FsPermission permission
      ) throws IOException;

  /**
   * 设置指定文件或目录的所有者信息，username和groupname不能全为空
   * @param src
   * @param username If it is null, the original username remains unchanged.
   * @param groupname If it is null, the original groupname remains unchanged.
   */
  public void setOwner(String src, String username, String groupname
      ) throws IOException;

  /**
   * 丢弃数据块
   * 当客户端无法连接名字节点返回的数据节点时，需要调用该方法明确放弃该数据块
   * 然后获取新的数据块
   * 所有在该数据块上的部分写操作都会被丢弃
   * The client can give up on a blcok by calling abandonBlock().
   * The client can then
   * either obtain a new block, or complete or abandon the file.
   * Any partial writes to the block will be discarded.
   */
  public void abandonBlock(Block b, String src, String holder
      ) throws IOException;

  /**
   * 添加数据块
   * 当创建文件，或写满一个数据块之后，客户端会调用该方法申请添加新的数据块
   * 该方法已被废弃官方推荐使用三个参数的重载方法
   * A client that wants to write an additional block to the 
   * indicated filename (which must currently be open for writing)
   * should call addBlock().  
   *
   * addBlock() allocates a new block and datanodes the block data
   * should be replicated to.
   * 
   * @deprecated use the 3-arg form below
   * @return LocatedBlock allocated block information.
   */
  public LocatedBlock addBlock(String src, String clientName) throws IOException;

  /**
   * 添加数据块
   * 官方推荐使用的方法
   * 添加了excludedNodes参数，用于指定排除某些数据节点，以避免再次被分配到无法连接的节点
   * A client that wants to write an additional block to the 
   * indicated filename (which must currently be open for writing)
   * should call addBlock().  
   *
   * addBlock() allocates a new block and datanodes the block data
   * should be replicated to.
   *
   * @param excludedNodes a list of nodes that should not be allocated
   * 
   * @return LocatedBlock allocated block information.
   */
  public LocatedBlock addBlock(String src, String clientName,
                               DatanodeInfo[] excludedNodes) throws IOException;

  /**
   * The client is done writing data to the given filename, and would 
   * like to complete it.  
   *
   * The function returns whether the file has been closed successfully.
   * If the function returns false, the caller should try again.
   *
   * A call to complete() will not return true until all the file's
   * blocks have been replicated the minimum number of times.  Thus,
   * DataNode failures may cause a client to call complete() several
   * times before succeeding.
   */
  public boolean complete(String src, String clientName) throws IOException;

  /**
   * 客户端用于向名字节点报告坏的数据块
   * The client wants to report corrupted blocks (blocks with specified
   * locations on datanodes).
   * @param blocks Array of located blocks to report
   */
  public void reportBadBlocks(LocatedBlock[] blocks) throws IOException;

  ///////////////////////////////////////
  // Namespace management
  ///////////////////////////////////////
  /**
   * Rename an item in the file system namespace.
   * 重命名文件或目录
   * @param src existing file or directory name.
   * @param dst new name.
   * @return true if successful, or false if the old name does not exist
   * or if the new name already belongs to the namespace.
   * @throws IOException if the new name is invalid.
   * @throws QuotaExceededException if the rename would violate 
   *                                any quota restriction
   */
  public boolean rename(String src, String dst) throws IOException;

  /**
   * Delete the given file or directory from the file system.
   * 删除文件或目录
   * <p>
   * Any blocks belonging to the deleted files will be garbage-collected.
   * 
   * @param src existing name.
   * @return true only if the existing file or directory was actually removed 
   * from the file system. 
   */
  public boolean delete(String src) throws IOException;

  /**
   * Delete the given file or directory from the file system.
   * 删除文件或目录，指定是否递归
   * <p>
   * same as delete but provides a way to avoid accidentally 
   * deleting non empty directories programmatically. 
   * @param src existing name
   * @param recursive if true deletes a non empty directory recursively,
   * else throws an exception.
   * @return true only if the existing file or directory was actually removed 
   * from the file system. 
   */
  public boolean delete(String src, boolean recursive) throws IOException;
  
  /**
   * Create a directory (or hierarchy of directories) with the given
   * name and permission.
   * 创建目录
   *
   * @param src The path of the directory being created
   * @param masked The masked permission of the directory being created
   * @return True if the operation success.
   * @throws {@link AccessControlException} if permission to create file is 
   * denied by the system. As usually on the client side the exception will 
   * be wraped into {@link org.apache.hadoop.ipc.RemoteException}.
   * @throws QuotaExceededException if the operation would violate 
   *                                any quota restriction.
   */
  public boolean mkdirs(String src, FsPermission masked) throws IOException;

  /**
   * Get a partial listing of the indicated directory
   * 获取指定目录的部分子项列表
   * 
   * @param src the directory name
   * @param startAfter the name of the last entry received by the client
   * @return a partial listing starting after startAfter 
   */
  public DirectoryListing getListing(String src, byte[] startAfter)
  throws IOException;

  ///////////////////////////////////////
  // System issues and management
  ///////////////////////////////////////

  /**
   * Client programs can cause stateful changes in the NameNode
   * that affect other clients.  A client may obtain a file and 
   * neither abandon nor complete it.  A client might hold a series
   * of locks that prevent other clients from proceeding.
   * Clearly, it would be bad if a client held a bunch of locks
   * that it never gave up.  This can happen easily if the client
   * dies unexpectedly.
   * <p>
   * So, the NameNode will revoke the locks and live file-creates
   * for clients that it thinks have died.  A client tells the
   * NameNode that it is still alive by periodically calling
   * renewLease().  If a certain amount of time passes since
   * the last call to renewLease(), the NameNode assumes the
   * client has died.
   * 更新租约信息
   * 客户端通过这个方法来告知名字节点自己是否还依然存活
   * 相当于心跳方法
   */
  public void renewLease(String clientName) throws IOException;

  public int GET_STATS_CAPACITY_IDX = 0;
  public int GET_STATS_USED_IDX = 1;
  public int GET_STATS_REMAINING_IDX = 2;
  public int GET_STATS_UNDER_REPLICATED_IDX = 3;
  public int GET_STATS_CORRUPT_BLOCKS_IDX = 4;
  public int GET_STATS_MISSING_BLOCKS_IDX = 5;
  
  /**
   * Get a set of statistics about the filesystem.
   * Right now, only three values are returned.
   * 获取文件系统的统计信息，包含以下内容：
   *    - 文件系统总存储空间大小
   *    - 文件系统总的已使用空间大小
   *    - 文件系统可用空间大小
   *    - 副本数不足的数据块数量
   *    - 包含损坏副本的数据块数量
   *    - 所有副本全都损坏的数据块数量
   * <ul>
   * <li> [0] contains the total storage capacity of the system, in bytes.</li>
   * <li> [1] contains the total used space of the system, in bytes.</li>
   * <li> [2] contains the available storage of the system, in bytes.</li>
   * <li> [3] contains number of under replicated blocks in the system.</li>
   * <li> [4] contains number of blocks with a corrupt replica. </li>
   * <li> [5] contains number of blocks without any good replicas left. </li>
   * </ul>
   * Use public constants like {@link #GET_STATS_CAPACITY_IDX} in place of 
   * actual numbers to index into the array.
   */
  public long[] getStats() throws IOException;

  /**
   * Get a report on the system's current datanodes.
   * One DatanodeInfo object is returned for each DataNode.
   * Return live datanodes if type is LIVE; dead datanodes if type is DEAD;
   * otherwise all datanodes if type is ALL.
   * 获取数据结点的报告信息
   */
  public DatanodeInfo[] getDatanodeReport(FSConstants.DatanodeReportType type)
  throws IOException;

  /**
   * Get the block size for the given file.
   * 获取指定文件的数据块大小
   * @param filename The name of the file
   * @return The number of bytes in each block
   * @throws IOException
   */
  public long getPreferredBlockSize(String filename) throws IOException;

  /**
   * Enter, leave or get safe mode.
   * 进入或退出安全模式
   * 也可以使用更该方法获取安全模式当前的状态
   * <p>
   * Safe mode is a name node state when it
   * <ol><li>does not accept changes to name space (read-only), and</li>
   * <li>does not replicate or delete blocks.</li></ol>
   * 
   * <p>
   * Safe mode is entered automatically at name node startup.
   * Safe mode can also be entered manually using
   * {@link #setSafeMode(FSConstants.SafeModeAction) setSafeMode(SafeModeAction.SAFEMODE_GET)}.
   * <p>
   * At startup the name node accepts data node reports collecting
   * information about block locations.
   * In order to leave safe mode it needs to collect a configurable
   * percentage called threshold of blocks, which satisfy the minimal 
   * replication condition.
   * The minimal replication condition is that each block must have at least
   * <tt>dfs.replication.min</tt> replicas.
   * When the threshold is reached the name node extends safe mode
   * for a configurable amount of time
   * to let the remaining data nodes to check in before it
   * will start replicating missing blocks.
   * Then the name node leaves safe mode.
   * <p>
   * If safe mode is turned on manually using
   * {@link #setSafeMode(FSConstants.SafeModeAction) setSafeMode(SafeModeAction.SAFEMODE_ENTER)}
   * then the name node stays in safe mode until it is manually turned off
   * using {@link #setSafeMode(FSConstants.SafeModeAction) setSafeMode(SafeModeAction.SAFEMODE_LEAVE)}.
   * Current state of the name node can be verified using
   * {@link #setSafeMode(FSConstants.SafeModeAction) setSafeMode(SafeModeAction.SAFEMODE_GET)}
   * <h4>Configuration parameters:</h4>
   * <tt>dfs.safemode.threshold.pct</tt> is the threshold parameter.<br>
   * <tt>dfs.safemode.extension</tt> is the safe mode extension parameter.<br>
   * <tt>dfs.replication.min</tt> is the minimal replication parameter.
   * 
   * <h4>Special cases:</h4>
   * The name node does not enter safe mode at startup if the threshold is 
   * set to 0 or if the name space is empty.<br>
   * If the threshold is set to 1 then all blocks need to have at least 
   * minimal replication.<br>
   * If the threshold value is greater than 1 then the name node will not be 
   * able to turn off safe mode automatically.<br>
   * Safe mode can always be turned off manually.
   * 
   * @param action  <ul> <li>0 leave safe mode;</li>
   *                <li>1 enter safe mode;</li>
   *                <li>2 get safe mode state.</li></ul>
   * @return <ul><li>0 if the safe mode is OFF or</li> 
   *         <li>1 if the safe mode is ON.</li></ul>
   * @throws IOException
   */
  public boolean setSafeMode(FSConstants.SafeModeAction action) throws IOException;

  /**
   * Save namespace image.
   * 手动进行镜像融合操作，需要在安全模式下进行
   * <p>
   * Saves current namespace into storage directories and reset edits log.
   * Requires superuser privilege and safe mode.
   * 
   * @throws AccessControlException if the superuser privilege is violated.
   * @throws IOException if image creation failed.
   */
  public void saveNamespace() throws IOException;

  /**
   * 告诉名字节点重新读取数据结点的信息
   * 无参数，需要的信息保存在include文件（一般在配置目录下）和exclude文件中
   * @throws IOException
   */
  public void refreshNodes() throws IOException;

  /**
   * 提交升级
   * @throws IOException
   */
  public void finalizeUpgrade() throws IOException;

  /**
   * 获取升级进度信息，或者进行强制升级操作
   * 
   * @param action {@link FSConstants.UpgradeAction} to perform
   * @return upgrade status information or null if no upgrades are in progress
   * @throws IOException
   */
  public UpgradeStatusReport distributedUpgradeProgress(UpgradeAction action) 
  throws IOException;

  /**
   * 将名字节点中的主要数据结构保存到Hadoop日志目录的指定文件中
   * 该文件包含名字节点收到的数据结点的心跳信息、等待被复制的数据块、正在被复制的数据块、等待被删除的数据块等信息
   * 调用该方法不用处于安全模式
   * @throws IOException
   */
  public void metaSave(String filename) throws IOException;

  /**
   * Tell all datanodes to use a new, non-persistent bandwidth value for
   * dfs.balance.bandwidthPerSec.
   *
   * @param bandwidth Blanacer bandwidth in bytes per second for this datanode.
   * @throws IOException
   */
  public void setBalancerBandwidth(long bandwidth) throws IOException;

  /**
   * Get the file info for a specific file or directory.
   * @param src The string representation of the path to the file
   * @throws IOException if permission to access file is denied by the system 
   * @return object containing information regarding the file
   *         or null if file not found
   */
  public HdfsFileStatus getFileInfo(String src) throws IOException;

  /**
   * Get {@link ContentSummary} rooted at the specified directory.
   * @param path The string representation of the path
   */
  public ContentSummary getContentSummary(String path) throws IOException;

  /**
   * Set the quota for a directory.
   * 设置配额，第二个参数为目录配额，第三个参数为空间配置信息
   *    - 目录配额：目录树中文件或者目录的数量限制，用于防止用户创建大量的小文件
   *    - 空间配额：可在目录树中存储的文件大小的限制。
   * @param path  The string representation of the path to the directory
   * @param namespaceQuota Limit on the number of names in the tree rooted 
   *                       at the directory
   * @param diskspaceQuota Limit on disk space occupied all the files under
   *                       this directory. 
   * <br><br>
   *                       
   * The quota can have three types of values : (1) 0 or more will set 
   * the quota to that value, (2) {@link FSConstants#QUOTA_DONT_SET}  implies 
   * the quota will not be changed, and (3) {@link FSConstants#QUOTA_RESET} 
   * implies the quota will be reset. Any other value is a runtime error.
   *                        
   * @throws FileNotFoundException if the path is a file or 
   *                               does not exist 
   * @throws QuotaExceededException if the directory size 
   *                                is greater than the given quota
   */
  public void setQuota(String path, long namespaceQuota, long diskspaceQuota)
                      throws IOException;
  
  /**
   * Write all metadata for this file into persistent storage.
   * The file must be currently open for writing.
   * 持久化文件的元数据信息
   * @param src The string representation of the path
   * @param client The string representation of the client
   */
  public void fsync(String src, String client) throws IOException;

  /**
   * Sets the modification and access time of the file to the specified time.
   * @param src The string representation of the path
   * @param mtime The number of milliseconds since Jan 1, 1970.
   *              Setting mtime to -1 means that modification time should not be set
   *              by this call.
   * @param atime The number of milliseconds since Jan 1, 1970.
   *              Setting atime to -1 means that access time should not be set
   *              by this call.
   */
  public void setTimes(String src, long mtime, long atime) throws IOException;

  /**
   * Get a valid Delegation Token.
   * 获取一个合法的代理令牌
   *
   * @param renewer the designated renewer for the token
   * @return Token<DelegationTokenIdentifier>
   * @throws IOException
   */
  public Token<DelegationTokenIdentifier> getDelegationToken(Text renewer) throws IOException;

  /**
   * Renew an existing delegation token.
   * 更新已有的代理令牌
   *
   * @param token delegation token obtained earlier
   * @return the new expiration time
   * @throws IOException
   */
  public long renewDelegationToken(Token<DelegationTokenIdentifier> token)
      throws IOException;

  /**
   * Cancel an existing delegation token.
   * 取消一个合法的代理令牌
   *
   * @param token delegation token
   * @throws IOException
   */
  public void cancelDelegationToken(Token<DelegationTokenIdentifier> token)
      throws IOException;
}
