package com.ibm

package plain

package aio

import java.io.IOException
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousServerSocketChannel ⇒ ServerChannel, AsynchronousSocketChannel ⇒ SocketChannel, AsynchronousByteChannel ⇒ Channel, CompletionHandler ⇒ Handler, InterruptedByTimeoutException }
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

import scala.math.min
import scala.util.continuations.{ reset, shift, suspendable }
import scala.concurrent.duration.Duration

import io.gzip
import Iteratee.{ Cont, Done, Error }
import Input.{ Elem, Empty, Eof }
import concurrent.{ OnlyOnce, scheduleOnce }
import logging.HasLogger

/**
 * Helper for Io with all low-level ByteBuffer methods.
 */
abstract sealed class IoHelper[E <: Io] {

  private[this] val self: E = this.asInstanceOf[E]

  import self._

  final def decode(implicit cset: Charset): String = resetBuffer(
    buffer.remaining match {
      case 0 ⇒ Io.emptyString
      case 1 ⇒ String.valueOf(buffer.get.toChar)
      case _ ⇒ new String(readBytes, cset)
    })

  final def length: Int = buffer.remaining

  final def take(n: Int): Io = {
    markLimit
    buffer.limit(min(buffer.limit, buffer.position + n))
    self
  }

  final def peek(n: Int): Io = {
    markPosition
    take(n)
  }

  final def drop(n: Int): Io = {
    buffer.position(min(buffer.limit, buffer.position + n))
    self
  }

  final def indexOf(b: Byte): Int = {
    val p = buffer.position
    val l = buffer.limit
    var i = p
    while (i < l && b != buffer.get(i)) i += 1
    if (i == l) -1 else i - p
  }

  final def span(p: Int ⇒ Boolean): (Int, Int) = {
    val pos = buffer.position
    val l = buffer.limit
    var i = pos
    while (i < l && p(buffer.get(i))) i += 1
    (i - pos, l - i)
  }

  final def readAllBytes = readBytes

  @inline private[this] final def readBytes: Array[Byte] = buffer.remaining match {
    case 0 ⇒ Io.emptyArray
    case 1 ⇒ Array.fill(1)(buffer.get)
    case n ⇒ val a = new Array[Byte](n); buffer.get(a); a
  }

  @inline private[this] final def markLimit = limitmark = buffer.limit

  @inline private[this] final def markPosition = positionmark = buffer.position

  @inline private[this] final def resetBuffer[A](a: A): A = {
    buffer.limit(limitmark)
    if (-1 < positionmark) { buffer.position(positionmark); positionmark = -1 }
    a
  }

  private[this] final var limitmark = -1

  private[this] final var positionmark = -1

}

/**
 *
 */
private final class SocketChannelWithTimeout private (

  channel: SocketChannel)

  extends Channel {

  @inline final def close = channel.close

  @inline final def isOpen = channel.isOpen

  @inline final def read[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = {
    channel.read(buffer, readWriteTimeout, TimeUnit.MILLISECONDS, attachment, handler)
  }

  @inline final def write[A](buffer: ByteBuffer, attachment: A, handler: Handler[Integer, _ >: A]) = {
    channel.write(buffer, readWriteTimeout, TimeUnit.MILLISECONDS, attachment, handler)
  }

  def read(buffer: ByteBuffer) = throw FutureNotSupported

  def write(buffer: ByteBuffer) = throw FutureNotSupported

}

private final object SocketChannelWithTimeout {

  final def apply(channel: SocketChannel) = new SocketChannelWithTimeout(tweak(channel))

  private[this] final def tweak(channel: SocketChannel): SocketChannel = {
    import StandardSocketOptions._
    tcpNoDelay match {
      case 1 ⇒ channel.setOption(TCP_NODELAY, Boolean.box(true))
      case -1 ⇒ channel.setOption(TCP_NODELAY, Boolean.box(false))
      case _ ⇒
    }
    channel.setOption(SO_REUSEADDR, Boolean.box(true))
    channel.setOption(SO_KEEPALIVE, Boolean.box(false))
    channel.setOption(SO_RCVBUF, Integer.valueOf(sendReceiveBufferSize))
    channel.setOption(SO_SNDBUF, Integer.valueOf(sendReceiveBufferSize))
  }

}

/**
 * Io represents the context of an asynchronous i/o operation.
 */
final class Io private (

  var server: ServerChannel,

  val channel: Channel,

  var buffer: ByteBuffer,

  var iteratee: Iteratee[Io, _],

  var renderable: RenderableRoot,

  var k: Io.IoCont,

  var readwritten: Int,

  var keepalive: Boolean,

  var roundtrips: Long,

  var payload: Any)

  extends IoHelper[Io] {

  import Io._

  /**
   * The trick method of the entire algorithm, it should be called only when the buffer is too small and on start with Io.empty.
   */
  final def ++(that: Io): Io = if (0 == this.length) {
    that
  } else if (0 == that.length) {
    this
  } else {
    warnOnce
    val len = this.length + that.length
    val b = ByteBuffer.allocate(len)
    b.put(this.readAllBytes)
    this.releaseBuffer
    b.put(that.buffer)
    that.releaseBuffer
    b.flip
    that + b
  }

  @inline def ++(server: ServerChannel) = { this.server = server; this }

  @inline def ++(channel: Channel) = new Io(server, channel, buffer, iteratee, renderable, k, readwritten, keepalive, roundtrips, payload)

  @inline def ++(iteratee: Iteratee[Io, _]) = { this.iteratee = iteratee; this }

  @inline def ++(renderable: RenderableRoot) = { this.renderable = renderable; this }

  @inline def ++(k: IoCont) = { this.k = k; this }

  @inline def ++(readwritten: Int) = { this.readwritten = readwritten; this }

  @inline def ++(roundtrips: Long) = { this.roundtrips = roundtrips; this }

  @inline def ++(keepalive: Boolean) = { if (this.keepalive) this.keepalive = keepalive; this }

  @inline def +++(payload: Any) = { this.payload = payload; this }

  @inline def ++(buffer: ByteBuffer) = if (0 < this.buffer.remaining) {
    new Io(server, channel, buffer, iteratee, renderable, k, readwritten, keepalive, roundtrips, payload)
  } else {
    this + buffer
  }

  @inline def isError = iteratee.isInstanceOf[Error[_]]

  @inline private def +(buffer: ByteBuffer) = {
    if (this.buffer ne emptyBuffer) releaseByteBuffer(this.buffer)
    this.buffer = buffer
    this
  }

  @inline private def releaseBuffer = if (buffer ne emptyBuffer) {
    releaseByteBuffer(buffer)
    buffer = emptyBuffer
  }

  @inline private def clear = buffer.clear

  @inline private def release = {
    releaseBuffer
    if (channel.isOpen) channel.close
    payload = null
  }

  @inline private def error(e: Throwable) = {
    e match {
      case _: IOException ⇒
      case e ⇒ logger.debug("Io.error " + e.toString)
    }
    releaseBuffer
  }

}

/**
 * The Io object contains all the complex continuations stuff, it is sort of an 'Io' monad.
 */
object Io

  extends HasLogger

  with OnlyOnce {

  import Iteratee._

  final type IoCont = Io ⇒ Unit

  @inline private[aio] final def empty = new Io(null, null, emptyBuffer, null, null, null, -1, true, 0L, null)

  final private[aio] val emptyArray = new Array[Byte](0)

  final private[aio] val emptyBuffer = ByteBuffer.wrap(emptyArray)

  final private[aio] val emptyString = new String

  final private def warnOnce = onlyonce { warning("Chunked input found. Enlarge aio.default-buffer-size : " + defaultBufferSize) }

  final private val logger = log

  /**
   * Accept.
   */
  private[this] final class AcceptHandler

    extends Handler[SocketChannel, Io] {

    @inline final def completed(c: SocketChannel, io: Io) = {
      import io._
      server.accept(io, this)
      k(io ++ SocketChannelWithTimeout(c) ++ defaultByteBuffer)
    }

    @inline final def failed(e: Throwable, io: Io) = {
      import io._
      if (server.isOpen) {
        server.accept(io, this)
        e match {
          case _: IOException ⇒
          case e: Throwable ⇒ warning("accept failed : " + io + " " + e)
        }
      }
    }

  }

  /**
   * Just an idea to avoid DNSA.
   */
  private[this] final class PausingAcceptHandler(pauseinmilliseconds: Long)

    extends Handler[SocketChannel, Io] {

    @inline final def completed(c: SocketChannel, io: Io) = {
      import io._
      if (0 == pauseinmilliseconds)
        server.accept(io, this)
      else
        scheduleOnce(pauseinmilliseconds)(server.accept(io, this))
      k(io ++ SocketChannelWithTimeout(c) ++ defaultByteBuffer)
    }

    @inline final def failed(e: Throwable, io: Io) = {
      import io._
      if (server.isOpen) {
        /**
         * Do not pause here, in case of failure we want to be back online asap.
         */
        server.accept(io, this)
        e match {
          case _: IOException ⇒
          case e: Throwable ⇒ warning("accept failed : " + io + " " + e)
        }
      }
    }

  }

  /**
   * Read/Write
   */
  private[this] object IoHandler extends Handler[Integer, Io] {

    final def completed(processed: Integer, io: Io) = {
      import io._
      k(io ++ processed)
    }

    @inline final def failed(e: Throwable, io: Io) = {
      import io._
      k(io ++ Error[Io](e))
    }

  }

  final def accept(server: ServerChannel, pausebetweenaccepts: Duration): Io @suspendable =
    shift { k: IoCont ⇒ server.accept(Io.empty ++ server ++ k, pausebetweenaccepts.toMillis match { case m if 0 < m ⇒ new PausingAcceptHandler(m) case _ ⇒ new AcceptHandler }) }

  @inline private[aio] final def read(io: Io): Io @suspendable = {
    import io._
    shift { k: IoCont ⇒ buffer.clear; channel.read(buffer, io ++ k, IoHandler) }
  }

  @inline private[aio] final def write(io: Io): Io @suspendable = {
    import io._
    shift { k: IoCont ⇒ channel.write(buffer, io ++ k, IoHandler) }
    if (0 == buffer.remaining || io.isError) io else write(io)
  }

  @inline private[this] final def unhandled(e: Any) = error("unhandled " + e)

  @inline private[this] final val ignored = ()

  final def loop[E, A <: RenderableRoot](io: Io, processor: Processor[E, A]): Unit @suspendable = {

    val readiteratee = io.iteratee

    import http._
    @inline def readloop(io: Io): Unit @suspendable = {
      (read(io) match {
        case io if -1 < io.readwritten ⇒
          io.buffer.flip
          io.iteratee(Elem(io))
        case io ⇒
          io.iteratee(Eof)
      }) match {
        case (cont @ Cont(_), Empty) ⇒
          readloop(io ++ cont ++ defaultByteBuffer)
        case (e @ Done(_), Elem(io)) ⇒
          processloop(io ++ e)
        case (e @ Error(_), Elem(io)) ⇒
          io.clear
          processloop(io ++ e)
        case (_, Eof) ⇒
          ignored
        case e ⇒
          unhandled(e)
      }
    }

    @inline def processloop(io: Io): Unit @suspendable = {
      (processor.doProcess(io) match {
        case io ⇒ io.iteratee
      }) match {
        case Done(renderable: RenderableRoot) ⇒
          io.payload match {
            case (length: Long, source: Channel, destination: Channel) ⇒
              ChannelTransfer(source, destination, io).transfer
              writeloop(renderable.renderHeader(io ++ renderable))
            case pl ⇒
              writeloop(renderable.renderHeader(io ++ renderable))
          }
        case Error(e: InterruptedByTimeoutException) ⇒
        case Error(e: IOException) ⇒
          io.error(e)
        case Error(e) ⇒
          info("processloop " + e.toString)
          io.error(e)
        case e ⇒
          unhandled(e)
      }
    }

    @inline def writeloop(io: Io): Unit @suspendable = {
      (write(io) match {
        case io ⇒ io.iteratee
      }) match {
        case Done(keepalive: Boolean) ⇒
          if (keepalive) readloop(io.renderable.renderFooter(io) ++ readiteratee ++ (io.roundtrips + 1L))
        case Cont(_) ⇒
          writeloop(io.renderable.renderBody(io))
        case Error(e: IOException) ⇒
        case Error(e) ⇒
          info("writeloop " + e.toString)
        case e ⇒
          unhandled(e)
      }
    }

    readloop(io)
    io.release
  }

}