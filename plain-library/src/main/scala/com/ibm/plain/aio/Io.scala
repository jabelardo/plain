package com.ibm

package plain

package aio

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, AsynchronousServerSocketChannel ⇒ ServerChannel, AsynchronousSocketChannel ⇒ SocketChannel, CompletionHandler ⇒ Handler, InterruptedByTimeoutException }
import java.nio.charset.Charset

import scala.concurrent.duration.Duration
import scala.math.min

import com.ibm.plain.aio.Iteratee.Error

import Input.{ Elem, Empty, Eof }
import Iteratee.{ Cont, Done, Error }
import concurrent.{ OnlyOnce, scheduleOnce }
import logging.HasLogger

/**
 * Io represents the context of an asynchronous i/o operation.
 */
final class Io private (

  var channel: Channel,

  var buffer: ByteBuffer,

  var iteratee: Iteratee[Io, _],

  var renderable: RenderableRoot,

  var encoder: Encoder,

  var transfer: Transfer,

  var message: Message,

  var keepalive: Boolean) {

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
    val b = ByteBuffer.allocate(this.length + that.length)
    b.put(this.readBytes(this.buffer.remaining))
    b.put(that.buffer)
    this.releaseBuffer
    that.releaseBuffer
    b.flip
    that + b
  }

  @inline final def ++(channel: Channel) = new Io(channel, buffer, iteratee, renderable, encoder, transfer, message, keepalive)

  @inline final def ++(iteratee: Iteratee[Io, _]) = { this.iteratee = iteratee; this }

  @inline final def ++(renderable: RenderableRoot) = { this.renderable = renderable; this }

  @inline final def ++(encoder: Encoder) = { this.encoder = encoder; this }

  @inline final def ++(transfer: Transfer) = { this.transfer = transfer; this }

  @inline final def ++(message: Message) = { this.message = message; this }

  @inline final def ++(keepalive: Boolean) = { if (this.keepalive) this.keepalive = keepalive; this }

  @inline final def ++(buffer: ByteBuffer) = if (0 < this.buffer.remaining) {
    new Io(channel, buffer, iteratee, renderable, encoder, transfer, message, keepalive)
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

  @inline private[aio] final def release = {
    releaseBuffer
    if (channel.isOpen) channel.close
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
      case n ⇒ readBytes(n) match {
        case a if a eq array ⇒ StringPool.get(a, n)
        case a ⇒ new String(a, 0, n, cset)
      }
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

  @inline private[this] final def readBytes(n: Int): Array[Byte] = if (StringPool.arraySize >= n) { buffer.get(array, 0, n); array } else Array.fill(n)(buffer.get)

  @inline private[this] final def markLimit = limitmark = buffer.limit

  @inline private[this] final def markPosition = positionmark = buffer.position

  @inline private[this] final def advanceBuffer[A](a: A): A = {
    buffer.limit(limitmark)
    if (-1 < positionmark) { buffer.position(positionmark); positionmark = -1 }
    a
  }

  private[this] final var limitmark = -1

  private[this] final var positionmark = -1

  private[this] final val array = new Array[Byte](StringPool.arraySize)

}

/**
 * The Io object contains all the complex continuations stuff, it is sort of an 'Io' monad. Not anymore, back to callbacks.
 */
object Io

  extends HasLogger

  with OnlyOnce {

  import Iteratee._

  final private[aio] val emptyArray = new Array[Byte](0)

  final private[aio] val emptyBuffer = ByteBuffer.wrap(emptyArray)

  final private[aio] val emptyString = new String

  final private[aio] val empty = new Io(null, emptyBuffer, null, null, null, null, null, false)

  final private[aio] def apply(iteratee: Iteratee[Io, _]): Io = new Io(null, emptyBuffer, iteratee, null, null, null, null, true)

  final private def warnOnce = onlyonce { warning("Chunked input found. Enlarge aio.default-buffer-size : " + defaultBufferSize) }

  final private val logger = log

  /**
   * Io handlers
   */

  final def loop[E, A <: RenderableRoot](server: ServerChannel, readiteratee: Iteratee[Io, _], processor: Processor[A]): Unit = {

    object AcceptHandler

      extends Handler[SocketChannel, Io] {

      @inline final def completed(ch: SocketChannel, io: Io) = {
        accept
        read(io ++ SocketChannelWithTimeout(ch) ++ defaultByteBuffer)
      }

      @inline final def failed(e: Throwable, io: Io) = {
        if (server.isOpen) {
          accept
          e match {
            case _: IOException ⇒ debug("Accept failed : " + e)
            case e: Throwable ⇒ warning("Accept failed : " + e)
          }
        }
      }

    }

    object ReadHandler

      extends Handler[Integer, Io] {

      @inline final def completed(processed: Integer, io: Io) = {
        if (0 > processed) {
          io.release
        } else {
          io.buffer.flip
          if (0 == processed) io.iteratee(Eof) else io.iteratee(Elem(io)) match {
            case (cont @ Cont(_), Empty) ⇒
              read(io ++ cont ++ defaultByteBuffer)
            case (e @ Done(r), Eof) ⇒
              process(io ++ e)
            case (e @ Error(_), Elem(io)) ⇒
              io.buffer.clear
              process(io ++ e)
            case (_, Eof) ⇒
              ignore
            case e ⇒
              unhandled(e)
          }
        }
      }

      @inline final def failed(e: Throwable, io: Io) = io.release

    }

    object ProcessHandler

      extends Handler[Null, Io] {

      @inline final def completed(processed: Null, io: Io) = {
        io.iteratee match {
          case Done(renderable: RenderableRoot) ⇒
            renderable.renderHeader(io ++ renderable).transfer match {
              case Transfer(_, _, _) ⇒ TransferHandler.read(io)
              case _ ⇒ write(io)
            }
          case Error(e: InterruptedByTimeoutException) ⇒
            ignore
          case Error(e: IOException) ⇒
            io.error(e)
          case Error(e) ⇒
            info("process failed " + e.toString)
            io.error(e)
          case e ⇒
            unhandled(e)
        }
      }

      @inline final def failed(e: Throwable, io: Io) = completed(null, io)

    }

    object WriteHandler

      extends Handler[Integer, Io] {

      @inline final def completed(processed: Integer, io: Io) = {
        if (0 > processed) {
          io.release
        } else if (0 == io.buffer.remaining || io.isError) {
          io.iteratee match {
            case Done(keepalive: Boolean) ⇒
              if (keepalive) {
                read(io.renderable.renderFooter(io) ++ readiteratee)
              } else {
                io.release
              }
            case Cont(_) ⇒
              io.renderable.renderBody(io).transfer match {
                case Transfer(_, _, _) ⇒ TransferHandler.read(io)
                case _ ⇒ write(io)
              }
            case Error(e: IOException) ⇒
              io.release
              ignore
            case Error(e) ⇒
              info("write failed " + e.toString)
            case e ⇒
              unhandled(e)
          }
        } else {
          write(io)
        }
      }

      @inline final def failed(e: Throwable, io: Io) = io.release

    }

    object TransferHandler

      extends Handler[Integer, Io] {

      @inline def read(io: Io): Unit = if (0 < io.buffer.remaining) {
        write(io)
      } else {
        io.buffer.clear
        io.transfer.source.read(io.buffer, io, this)
      }

      @inline def write(io: Io) = io.transfer.destination.write(io.buffer, io, TransferWriteHandler)

      @inline def completed(processed: Integer, io: Io) = {
        io.buffer.flip
        if (0 < processed) {
          if (io.transfer.encoder.isDefined) {
            io.transfer.encoder.get.encode(io.buffer)
            io.buffer.flip
          }
          write(io)
        } else {
          if (io.transfer.encoder.isDefined) {
            io.transfer.encoder.get.finish(io.buffer)
          }
          io.transfer.source match { case f: FileByteChannel ⇒ f.close case _ ⇒ }
          io.transfer.destination match { case f: FileByteChannel ⇒ f.close case _ ⇒ }
          io.transfer = null
          WriteHandler.completed(0, io)
        }
      }

      @inline def failed(e: Throwable, io: Io) = {
        io.transfer = null
        WriteHandler.failed(e, io)
      }

      object TransferWriteHandler extends Handler[Integer, Io] {

        @inline def completed(processed: Integer, io: Io) = read(io)

        @inline def failed(e: Throwable, io: Io) = TransferHandler.failed(e, io)

      }

    }

    /**
     * Io methods.
     */

    @inline def accept: Unit = {
      server.accept(Io(readiteratee), AcceptHandler)
    }

    @inline def read(io: Io): Unit = {
      io.buffer.clear
      io.channel.read(io.buffer, io, ReadHandler)
    }

    @inline def process(io: Io): Unit = {
      processor.doProcess(io, ProcessHandler)
    }

    @inline def write(io: Io): Unit = {
      io.channel.write(io.buffer, io, WriteHandler)
    }

    @inline def unhandled(e: Any) = error("unhandled " + e)

    @inline def ignore = ()

    accept

  }

}
