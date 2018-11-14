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
package org.apache.hadoop.hdfs.server.datanode;

/** 
 * a class to throttle the block transfers.
 * This class is thread safe. It can be shared by multiple threads.
 * The parameter bandwidthPerSec specifies the total bandwidth shared by
 * threads.
 * 传输节流器，用于控制发送数据的速度
 */
class BlockTransferThrottler {
  private long period;          // period over which bw is imposed
  private long periodExtension; // Max period over which bw accumulates.
  private long bytesPerPeriod; // total number of bytes can be sent in each period
  private long curPeriodStart; // current period starting time
  private long curReserve;     // remaining bytes can be sent in the period
  private long bytesAlreadyUsed;

  /** Constructor 
   * @param bandwidthPerSec bandwidth allowed in bytes per second. 
   */
  BlockTransferThrottler(long bandwidthPerSec) {
    this(500, bandwidthPerSec);  // by default throttling period is 500ms 
  }

  /**
   * Constructor
   * @param period in milliseconds. Bandwidth is enforced over this
   *        period.
   * @param bandwidthPerSec bandwidth allowed in bytes per second. 
   */
  BlockTransferThrottler(long period, long bandwidthPerSec) {
    this.curPeriodStart = System.currentTimeMillis();
    this.period = period;
    this.curReserve = this.bytesPerPeriod = bandwidthPerSec*period/1000;
    this.periodExtension = period*3;
  }

  /**
   * @return current throttle bandwidth in bytes per second.
   */
  synchronized long getBandwidth() {
    return bytesPerPeriod*1000/period;
  }
  
  /**
   * Sets throttle bandwidth. This takes affect latest by the end of current
   * period.
   * 
   * @param bytesPerSecond 
   */
  synchronized void setBandwidth(long bytesPerSecond) {
    if ( bytesPerSecond <= 0 ) {
      throw new IllegalArgumentException("" + bytesPerSecond);
    }
    bytesPerPeriod = bytesPerSecond*period/1000;
  }
  
  /** Given the numOfBytes sent/received since last time throttle was called,
   * make the current thread sleep if I/O rate is too fast
   * compared to the given bandwidth.
   *
   * 调用这个方法可以控制发送速度
   *
   * @param numOfBytes
   *     number of bytes sent/received since last time throttle was called
   */
  synchronized void throttle(long numOfBytes) {
    if ( numOfBytes <= 0 ) {
      return;
    }

    curReserve -= numOfBytes;
    bytesAlreadyUsed += numOfBytes;

    while (curReserve <= 0) {
      long now = System.currentTimeMillis();
      long curPeriodEnd = curPeriodStart + period;

      if ( now < curPeriodEnd ) {
        // Wait for next period so that curReserve can be increased.
        // 等待检查周期结束，curReserve在结束时会增加
        try {
          // 调用wait操作实现节流
          wait( curPeriodEnd - now );
        } catch (InterruptedException ignored) {}
      } else if ( now <  (curPeriodStart + periodExtension)) {
        curPeriodStart = curPeriodEnd;
        // 增加剩余请求量
        curReserve += bytesPerPeriod;
      } else {
        // discard the prev period. Throttler might not have
        // been used for a long time.
        // 丢弃前一个周期，重新启动节流器
        curPeriodStart = now;
        curReserve = bytesPerPeriod - bytesAlreadyUsed;
      }
    }
    // 减去已使用的字节
    bytesAlreadyUsed -= numOfBytes;
  }
}
