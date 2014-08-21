package com.ibm

package plain

package aio

/**
 *
 */
package object conduit

  extends config.CheckedConfig {

  import config._
  import config.settings._

  final val deflaterCompressionLevel = getInt("plain.aio.conduit.deflater-compression-level", 1)

  final val ignoreChecksumForGzipDecoding = getBoolean("plain.aio.conduit.ignore-checksum-for-gzip-decoding", true)

  final val defaultChunkSize = getBytes("plain.aio.conduit.default-chunk-size", 16 * 1024).toInt

  require(4 * 1024 <= defaultChunkSize && defaultChunkSize <= 64 * 1024, "Invalid default-chunk-size")

}
