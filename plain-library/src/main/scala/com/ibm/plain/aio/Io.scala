package com.ibm

package plain

package aio

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, AsynchronousServerSocketChannel ⇒ ServerChannel, AsynchronousSocketChannel ⇒ SocketChannel, CompletionHandler ⇒ Handler, InterruptedByTimeoutException }
import java.nio.charset.Charset
import java.util.Arrays

import scala.concurrent.duration.Duration
import scala.math.min

import io.PrintWriter
import concurrent.OnlyOnce
import Input.{ Elem, Empty, Eof }
import Iteratee.{ Cont, Done, Error }
import logging.HasLogger

/**
 * Io represents the context of an asynchronous i/o operation.
 */
final class Io private (

  var channel: Channel,

  var readbuffer: ByteBuffer,

  var writebuffer: ByteBuffer,

  var iteratee: Iteratee[Io, Any],

  var renderable: RenderableRoot,

  var encoder: Encoder,

  var transfer: Transfer,

  var message: Message,

  var keepalive: Boolean,

  var elementarray: Array[Byte],

  var peekarray: Array[Byte],

  var element: Iteratee[Io, Any],

  var printwriter: PrintWriter) {

  import Io._

  @inline final def isError = iteratee.isInstanceOf[Error[_]]

  /**
   * The trick method of the entire algorithm, it should be called only when the buffer is too small and on start with Io.empty.
   */
  @noinline final def ++(that: Io): Io = if (0 == this.length) {
    that
  } else if (0 == that.length) {
    this
  } else {
    if (defaultBufferSize < this.length + that.length) warnOnce
    val b = bestFitByteBuffer(this.length + that.length)
    b.put(this.readBytes(this.readbuffer.remaining), 0, this.readbuffer.remaining)
    b.put(that.readbuffer)
    this.releaseReadBuffer
    that.releaseReadBuffer
    b.flip
    that + b
  }

  @inline final def ++(channel: Channel) = new Io(channel, readbuffer, writebuffer, iteratee, renderable, encoder, transfer, message, keepalive, elementarray, peekarray, element, printwriter)

  @inline final def ++(iteratee: Iteratee[Io, _]) = { this.iteratee = iteratee; this }

  @inline final def ++(renderable: RenderableRoot) = { this.renderable = renderable; this }

  @inline final def ++(encoder: Encoder) = { this.encoder = encoder; this }

  @inline final def ++(transfer: Transfer) = { this.transfer = transfer; this }

  @inline final def ++(message: Message) = { this.message = message; this }

  @inline final def ++(keepalive: Boolean) = { if (this.keepalive) this.keepalive = keepalive; this }

  @inline final def ++(printwriter: PrintWriter) = { this.printwriter = printwriter; this }

  @inline final def ++(elementarray: Array[Byte]) = { this.elementarray = elementarray; this.peekarray = new Array[Byte](elementarray.length); this }

  @inline final def ++(readbuffer: ByteBuffer) = if (0 < this.readbuffer.remaining) {
    new Io(channel, readbuffer, writebuffer, iteratee, renderable, encoder, transfer, message, keepalive, elementarray, peekarray, element, printwriter)
  } else {
    this + readbuffer
  }

  @inline private final def +(readbuffer: ByteBuffer) = {
    if (this.readbuffer ne emptyBuffer) releaseByteBuffer(this.readbuffer)
    this.readbuffer = readbuffer
    this
  }

  @inline private final def releaseReadBuffer = if (readbuffer ne emptyBuffer) {
    releaseByteBuffer(readbuffer)
    readbuffer = emptyBuffer
  }

  @inline private final def releaseWriteBuffer = if (writebuffer ne emptyBuffer) {
    releaseByteBuffer(writebuffer)
    writebuffer = emptyBuffer
  }

  @inline private[aio] final def release = {
    releaseReadBuffer
    releaseWriteBuffer
    if (channel.isOpen) channel.close
  }

  @inline private final def error(e: Throwable) = {
    e match {
      case _: IOException ⇒
      case e ⇒ logger.debug("Io.error " + e.toString)
    }
    releaseReadBuffer
  }

  final def decode(implicit cset: Charset, lowercase: Boolean): String = advanceBuffer(
    readbuffer.remaining match {
      case 0 ⇒ Io.emptyString
      case n ⇒ readBytes(n) match {
        case a if a eq array ⇒ StringPool.get(if (lowercase) toLowerCase(a) else a, n)
        case a ⇒ new String(a, 0, n, cset)
      }
    })

  final def consume: Array[Byte] = advanceBuffer(
    readbuffer.remaining match {
      case 0 ⇒ Io.emptyArray
      case n ⇒ readBytes(n)
    })

  @inline final def length: Int = readbuffer.remaining

  final def take(n: Int): Io = {
    markLimit
    readbuffer.limit(min(readbuffer.limit, readbuffer.position + n))
    this
  }

  final def peek(n: Int): Io = {
    markPosition
    take(n)
  }

  @inline final def peek: Byte = readbuffer.get(readbuffer.position)

  final def drop(n: Int): Io = {
    readbuffer.position(min(readbuffer.limit, readbuffer.position + n))
    this
  }

  final def indexOf(b: Byte): Int = {
    val p = readbuffer.position
    val l = readbuffer.limit
    var i = p
    while (i < l && b != readbuffer.get(i)) i += 1
    if (i == l) -1 else i - p
  }

  final def span(p: Int ⇒ Boolean): (Int, Int) = {
    val pos = readbuffer.position
    val l = readbuffer.limit
    var i = pos
    while (i < l && p(readbuffer.get(i))) i += 1
    (i - pos, l - i)
  }

  @inline private[this] final def readBytes(n: Int): Array[Byte] = if (n <= StringPool.arraySize) { readbuffer.get(array, 0, n); array } else Array.fill(n)(readbuffer.get)

  @inline private[this] final def markLimit = limitmark = readbuffer.limit

  @inline private[this] final def markPosition = positionmark = readbuffer.position

  @inline private[this] final def advanceBuffer[A](a: A): A = {
    readbuffer.limit(limitmark)
    if (-1 < positionmark) { readbuffer.position(positionmark); positionmark = -1 }
    a
  }

  @inline private[this] final def toLowerCase(a: Array[Byte]): Array[Byte] = {
    for (i ← 0 until a.length) { val e = a(i); if ('A' <= e && e <= 'Z') a.update(i, (e + 32).toByte) }
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

  final private[aio] val empty = new Io(null, emptyBuffer, emptyBuffer, null, null, null, null, null, false, null, null, null, null)

  final private[aio] def apply(iteratee: Iteratee[Io, _]): Io =
    new Io(null, emptyBuffer, defaultByteBuffer, iteratee, null, null, null, null, true, null, null, null, null)

  final private def warnOnce = onlyonce { warning("Chunked input found. Enlarge aio.default-buffer-size : " + defaultBufferSize) }

  final private val logger = log

  /**
   * Io handlers
   */

  final def loop[E, A <: RenderableRoot](server: ServerChannel, readiteratee: Iteratee[Io, _], processor: Processor[A]): Unit = {

    object AcceptHandler

      extends Handler[SocketChannel, Io] {

      @inline final def completed(ch: SocketChannel, io: Io) = try {
        accept
        read(io ++ SocketChannelWithTimeout(ch) ++ defaultByteBuffer)
      } catch { case _: Throwable ⇒ io.release }

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

      @inline final def completed(processed: Integer, io: Io) = try {
        if (0 > processed) {
          io.release
        } else {
          io.readbuffer.flip
          io.writebuffer.clear
          if (0 == processed)
            io.iteratee(Eof)
          else {
            val usecached = if (null == io.elementarray) {
              io.readbuffer.mark
              false
            } else if (io.readbuffer.remaining >= io.elementarray.length) {
              io.readbuffer.mark
              io.readbuffer.get(io.peekarray)
              if (Arrays.equals(io.peekarray, io.elementarray)) {
                true
              } else {
                io.readbuffer.rewind
                io.elementarray = null
                false
              }
            } else {
              false
            }
            val elem = if (usecached) (io.element, Elem(io)) else io.iteratee(Elem(io))
            elem match {
              case (cont @ Cont(_), Empty) ⇒
                read(io ++ cont ++ defaultByteBuffer)
              case (e @ Done(_), Elem(io)) ⇒
                if (null == io.elementarray) {
                  var len = io.readbuffer.position
                  io.readbuffer.rewind
                  len -= io.readbuffer.position
                  io ++ new Array[Byte](len)
                  io.readbuffer.get(io.elementarray)
                  io.element = e
                }
                process(io ++ e)
              case (e @ Error(_), Elem(io)) ⇒
                io.readbuffer.clear
                process(io ++ e)
              case (_, Eof) ⇒
                ignore
              case e ⇒
                unhandled(e)
            }
          }
        }
      } catch { case _: Throwable ⇒ io.release }

      @inline final def failed(e: Throwable, io: Io) = io.release

    }

    object ProcessHandler

      extends Handler[Null, Io] {

      @inline final def completed(processed: Null, io: Io) = try {
        io.iteratee match {
          case Done(renderable: RenderableRoot) ⇒
            renderable.renderHeader(io ++ renderable).transfer match {
              case Transfer(_, _, _) ⇒ TransferHandler.read(io)
              case _ ⇒ if (0 < io.readbuffer.remaining) {
                io.renderable.renderFooter(io) ++ readiteratee
                val usecached = if (io.readbuffer.remaining >= io.elementarray.length) {
                  io.readbuffer.mark
                  io.readbuffer.get(io.peekarray)
                  if (Arrays.equals(io.peekarray, io.elementarray)) {
                    true
                  } else {
                    io.readbuffer.rewind
                    io.elementarray = null
                    false
                  }
                } else {
                  false
                }
                val elem = if (usecached) (io.element, Elem(io)) else io.iteratee(Elem(io))
                elem match {
                  case (cont @ Cont(_), Empty) ⇒
                    read(io ++ cont ++ defaultByteBuffer)
                  case (e @ Done(_), Elem(io)) ⇒
                    process(io ++ e)
                  case (e @ Error(_), Elem(io)) ⇒
                    io.readbuffer.clear
                    process(io ++ e)
                  case (_, Eof) ⇒
                    ignore
                  case e ⇒
                    unhandled(e)
                }
              } else {
                write(io)
              }
            }
          case Error(e: InterruptedByTimeoutException) ⇒
            ignore
          case Error(e: IOException) ⇒
            io.error(e)
          case Error(e) ⇒
            info("process failed " + e)
            io.error(e)
          case e ⇒
            unhandled(e)
        }
      } catch { case _: Throwable ⇒ io.release }

      @inline final def failed(e: Throwable, io: Io) = completed(null, io)

    }

    object WriteHandler

      extends Handler[Integer, Io] {

      @inline final def completed(processed: Integer, io: Io) = try {
        if (0 > processed) {
          io.release
        } else if (0 == io.writebuffer.remaining || io.isError) {
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
                case _ ⇒ write(io, false)
              }
            case Error(e: IOException) ⇒
              io.release
              ignore
            case Error(e) ⇒
              info("write failed " + e)
            case e ⇒
              unhandled(e)
          }
        } else {
          write(io, false)
        }
      } catch { case _: Throwable ⇒ io.release }

      @inline final def failed(e: Throwable, io: Io) = io.release

    }

    object TransferHandler

      extends Handler[Integer, Io] {

      @inline def read(io: Io) = {
        io.readbuffer.clear
        io.transfer.source.read(io.readbuffer, io, this)
      }

      @inline def write(io: Io) =
        io.transfer.destination.write(io.readbuffer, io, TransferWriteHandler)

      @inline def writeAndClose(io: Io) =
        io.transfer.destination.write(io.readbuffer, io, ClosingTransferWriteHandler)

      @inline def completed(processed: Integer, io: Io) = {
        val encoder = io.transfer.encoder.getOrElse(null)
        io.readbuffer.flip
        if (0 < processed) {
          if (null != encoder) {
            encoder.encode(io.readbuffer)
            io.readbuffer.flip
          }
          write(io)
        } else {
          if (null != encoder) {
            io.readbuffer.clear
            encoder.finish(io.readbuffer)
            writeAndClose(io)
          } else {
            ClosingTransferWriteHandler.completed(processed, io)
          }
        }
      }

      @inline def failed(e: Throwable, io: Io) = {
        cleanup(io)
        WriteHandler.failed(e, io)
      }

      @inline private[this] final def cleanup(io: Io) = {
        io.transfer.source match { case f: FileByteChannel ⇒ f.close case _ ⇒ }
        io.transfer.destination match { case f: FileByteChannel ⇒ f.close case _ ⇒ }
        io.transfer = null
      }

      private[this] object TransferWriteHandler

        extends Handler[Integer, Io] {

        @inline def completed(processed: Integer, io: Io) =
          if (0 < io.readbuffer.remaining) write(io) else read(io)

        @inline def failed(e: Throwable, io: Io) = TransferHandler.failed(e, io)

      }

      private[this] object ClosingTransferWriteHandler

        extends Handler[Integer, Io] {

        @inline def completed(processed: Integer, io: Io) = if (0 < io.readbuffer.remaining) {
          writeAndClose(io)
        } else {
          cleanup(io)
          WriteHandler.completed(0, io)
        }

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
      io.readbuffer.clear
      io.channel.read(io.readbuffer, io, ReadHandler)
    }

    @inline def process(io: Io): Unit = {
      processor.doProcess(io, ProcessHandler)
    }

    @inline def write(io: Io, flip: Boolean = true): Unit = {
      if (flip) io.writebuffer.flip
      io.channel.write(io.writebuffer, io, WriteHandler)
    }

    @inline def unhandled(e: Any) = error("unhandled " + e)

    @inline def ignore = ()

    accept

  }

}
