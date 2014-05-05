package com.ibm

package plain

package aio

/**
 *
 */
package object conduits

  extends config.CheckedConfig {

  import config._
  import config.settings._

  final val ignoreChecksumForGzipDecoding = getBoolean("plain.aio.conduits.ignore-checksum-for-gzip-decoding", true)

}
