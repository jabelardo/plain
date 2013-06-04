package com.ibm

package plain

package aio

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, AsynchronousServerSocketChannel ⇒ ServerChannel, AsynchronousSocketChannel ⇒ SocketChannel, CompletionHandler ⇒ Handler, InterruptedByTimeoutException }
import java.nio.charset.Charset

import scala.concurrent.duration.Duration
import scala.math.min
import scala.util.continuations.{ shift, suspendable }

import com.ibm.plain.aio.Iteratee.Error

import Input.{ Elem, Empty, Eof }
import Iteratee.{ Cont, Done, Error }
import concurrent.{ OnlyOnce, scheduleOnce }
import logging.HasLogger

/**
 * Io represents the context of an asynchronous i/o operation.
 */
final case class Io private (

  var server: ServerChannel,

  var channel: Channel,

  var buffer: ByteBuffer,

  var iteratee: Iteratee[Io, _],

  var renderable: RenderableRoot,

  var k: Io.IoCont,

  var readwritten: Int,

  var keepalive: Boolean,

  var roundtrips: Long,

  var payload: Any) {

  import Io._

  @inline final def isError = iteratee.isInstanceOf[Error[_]]

  /**
   * The trick method of the entire algorithm, it should be called only when the buffer is too small and on start with Io.empty.
   */
  @inline final def ++(that: Io): Io = if (0 == this.length) {
    that
  } else if (0 == that.length) {
    this
  } else {
    warnOnce
    val len = this.length + that.length
    val b = ByteBuffer.allocate(len)
    b.put(this.readBytes(this.buffer.remaining))
    this.releaseBuffer
    b.put(that.buffer)
    that.releaseBuffer
    b.flip
    that + b
  }

  @inline final def ++(channel: Channel) = Io(server, channel, buffer, iteratee, renderable, k, readwritten, keepalive, roundtrips, payload)

  @inline final def ++(iteratee: Iteratee[Io, _]) = { this.iteratee = iteratee; this }

  @inline final def ++(renderable: RenderableRoot) = { this.renderable = renderable; this }

  @inline final def ++(k: IoCont) = { this.k = k; this }

  @inline final def ++(readwritten: Int) = { this.readwritten = readwritten; this }

  @inline final def ++(roundtrips: Long) = { this.roundtrips = roundtrips; this }

  @inline final def ++(keepalive: Boolean) = { if (this.keepalive) this.keepalive = keepalive; this }

  @inline final def +++(payload: Any) = { this.payload = payload; this }

  @inline final def ++(buffer: ByteBuffer) = if (0 < this.buffer.remaining) {
    Io(server, channel, buffer, iteratee, renderable, k, readwritten, keepalive, roundtrips, payload)
  } else {
    this + buffer
  }

  @inline private final def +(buffer: ByteBuffer) = {
    if (this.buffer ne emptyBuffer) releaseByteBuffer(this.buffer)
    this.buffer = buffer
    this
  }

  @inline private final def releaseBuffer = if (buffer ne emptyBuffer) {
    releaseByteBuffer(buffer)
    buffer = emptyBuffer
  }

  @inline private final def clear = buffer.clear

  @inline private final def release = {
    releaseBuffer
    server = null
    if (channel.isOpen) channel.close
    channel = null
    iteratee = null
    renderable = null
    k = null
    readwritten = -1
    keepalive = true
    roundtrips = 0L
    payload = null
  }

  @inline private final def error(e: Throwable) = {
    e match {
      case _: IOException ⇒
      case e ⇒ logger.debug("Io.error " + e.toString)
    }
    releaseBuffer
  }

  final def decode(implicit cset: Charset): String = advanceBuffer(
    buffer.remaining match {
      case 0 ⇒ Io.emptyString
      case n ⇒ new String(readBytes(n), cset).intern
    })

  final def consume: Array[Byte] = advanceBuffer(
    buffer.remaining match {
      case 0 ⇒ Io.emptyArray
      case n ⇒ readBytes(n)
    })

  @inline final def length: Int = buffer.remaining

  final def take(n: Int): Io = {
    markLimit
    buffer.limit(min(buffer.limit, buffer.position + n))
    this
  }

  final def peek(n: Int): Io = {
    markPosition
    take(n)
  }

  @inline final def peek: Byte = buffer.get(buffer.position)

  final def drop(n: Int): Io = {
    buffer.position(min(buffer.limit, buffer.position + n))
    this
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

  @inline private[this] final def readBytes(n: Int): Array[Byte] = if (arraysize > n) { buffer.get(array, 0, n); array } else Array.fill(n)(buffer.get)

  @inline private[this] final def markLimit = limitmark = buffer.limit

  @inline private[this] final def markPosition = positionmark = buffer.position

  @inline private[this] final def advanceBuffer[A](a: A): A = {
    buffer.limit(limitmark)
    if (-1 < positionmark) { buffer.position(positionmark); positionmark = -1 }
    a
  }

  private[this] final var limitmark = -1

  private[this] final var positionmark = -1

  private[this] final val arraysize = 80

  private[this] final val array = new Array[Byte](arraysize)

}

/**
 * The Io object contains all the complex continuations stuff, it is sort of an 'Io' monad.
 */
object Io

  extends HasLogger

  with OnlyOnce {

  import Iteratee._

  var c1 = 0L
  var c2 = 0L

  final type IoCont = Io ⇒ Unit

  @inline private[aio] final def apply(server: ServerChannel): Io = Io(server, null, emptyBuffer, null, null, null, -1, true, 0L, null)

  final private[aio] val emptyArray = new Array[Byte](0)

  final private[aio] val emptyBuffer = ByteBuffer.wrap(emptyArray)

  final private[aio] val emptyString = new String

  final private[aio] val empty = Io(null, null, emptyBuffer, null, null, null, -1, true, 0L, null)

  final private def warnOnce = onlyonce { warning("Chunked input found. Enlarge aio.default-buffer-size : " + defaultBufferSize) }

  final private val logger = log

  /**
   * Accept.
   */
  private[this] object AcceptHandler

    extends Handler[SocketChannel, Io] {

    @inline final def completed(ch: SocketChannel, io: Io) = {
      import io._
      server.accept(io, this)
      k(io ++ SocketChannelWithTimeout(ch) ++ defaultByteBuffer)
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
  private[this] object IoHandler

    extends Handler[Integer, Io] {

    @inline final def completed(processed: Integer, io: Io) = {
      import io._
      k(io ++ processed)
    }

    @inline final def failed(e: Throwable, io: Io) = {
      import io._
      k(io ++ Error[Io](e))
    }

  }

  final def accept(server: ServerChannel, pausebetweenaccepts: Duration): Io @suspendable = {
    shift { k: IoCont ⇒ server.accept(Io(server) ++ k, pausebetweenaccepts.toMillis match { case m if 0 < m ⇒ new PausingAcceptHandler(m) case _ ⇒ AcceptHandler }) }
  }

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

  final def loop[E, A <: RenderableRoot](io: Io, processor: Processor[A]): Unit @suspendable = {

    val readiteratee = io.iteratee

    def readloop(io: Io): Unit @suspendable = {
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

    def processloop(io: Io): Unit @suspendable = {
      (processor.doProcess(io) match {
        case io ⇒ io.iteratee
      }) match {
        case Done(renderable: RenderableRoot) ⇒
          io.payload match {
            case (length: Long, source: Channel, destination: Channel) ⇒
              ChannelTransfer(source, destination, io).transfer
              writeloop(renderable.renderHeader(io ++ renderable))
            case _ ⇒
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

    def writeloop(io: Io): Unit @suspendable = {
      (write(io) match {
        case io ⇒ io.iteratee
      }) match {
        case Done(keepalive: Boolean) ⇒
          if (keepalive) readloop(io.renderable.renderFooter(io) ++ readiteratee ++ (io.roundtrips + 1L))
        case Cont(_) ⇒
          writeloop(io.renderable.renderBody(io))
        case Error(e: IOException) ⇒
          ignored
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
