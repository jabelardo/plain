package com.ibm.plain

package lib

import config.CheckedConfig

/**
 * Very small math helpers.
 */
package object math

  extends CheckedConfig {

  /**
   * If it is a power of 2 only one bit can be set. This is so clever ...
   */
  def isPowerOfTwo(i: Int) = 1 == Integer.bitCount(i)

  def nextPowerOfTwo(i: Int): Int = {
    var x = i - 1
    x |= x >> 1
    x |= x >> 2
    x |= x >> 4
    x |= x >> 8
    x |= x >> 16
    x + 1
  }

  def nextPowerOfTwo(l: Long): Long = {
    var x = l - 1
    x |= x >> 1
    x |= x >> 2
    x |= x >> 4
    x |= x >> 8
    x |= x >> 16
    x |= x >> 32
    x + 1
  }

}
