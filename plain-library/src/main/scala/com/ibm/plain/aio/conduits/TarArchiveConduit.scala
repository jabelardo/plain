package com.ibm

package plain

package aio

package conduits

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel }
import java.util.zip.{ CRC32, Inflater, DataFormatException }
import org.apache.commons.compress.archivers.tar.{ TarArchiveEntry, TarArchiveInputStream }
import io.ByteBufferInputStream
import com.ibm.plain.io.ByteBufferInputStream

/**
 *
 */
final class TarArchiveConduit private (

  protected[this] val underlyingchannel: Channel)

  extends TarArchiveSourceConduit

  with TarArchiveSinkConduit {

}

/**
 *
 */
object TarArchiveConduit {

  final def apply(underlyingchannel: Channel) = new TarArchiveConduit(underlyingchannel)

}

/**
 * Source conduit.
 */
sealed trait TarArchiveSourceConduit

  extends FilterSourceConduit[Channel] {

  protected[this] override def filterIn(processed: Integer, buffer: ByteBuffer): Integer = {
    unsupported
  }

  protected[this] def hasSufficient = {
    unsupported
  }

}

/**
 * Sink conduit.
 */
sealed trait TarArchiveSinkConduit

  extends FilterSinkConduit[Channel] {

  protected[this] def filterOut(processed: Integer, buffer: ByteBuffer): Integer = {
    -1
  }

}

