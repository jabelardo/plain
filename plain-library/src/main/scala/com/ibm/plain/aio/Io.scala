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

  var readbuffer: ByteBuffer,

  var writebuffer: ByteBuffer,

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
    b.put(this.readBytes(this.readbuffer.remaining))
    b.put(that.readbuffer)
    this.releaseReadBuffer
    that.releaseReadBuffer
    b.flip
    that + b
  }

  @inline final def ++(channel: Channel) = new Io(channel, readbuffer, writebuffer, iteratee, renderable, encoder, transfer, message, keepalive)

  @inline final def ++(iteratee: Iteratee[Io, _]) = { this.iteratee = iteratee; this }

  @inline final def ++(renderable: RenderableRoot) = { this.renderable = renderable; this }

  @inline final def ++(encoder: Encoder) = { this.encoder = encoder; this }

  @inline final def ++(transfer: Transfer) = { this.transfer = transfer; this }

  @inline final def ++(message: Message) = { this.message = message; this }

  @inline final def ++(keepalive: Boolean) = { if (this.keepalive) this.keepalive = keepalive; this }

  @inline final def ++(readbuffer: ByteBuffer) = if (0 < this.readbuffer.remaining) {
    new Io(channel, readbuffer, writebuffer, iteratee, renderable, encoder, transfer, message, keepalive)
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

  @inline private[this] final def releaseWriteBuffer = if (writebuffer ne emptyBuffer) {
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

  final def decode(implicit cset: Charset): String = advanceBuffer(
    readbuffer.remaining match {
      case 0 ⇒ Io.emptyString
      case n ⇒ readBytes(n) match {
        case a if a eq array ⇒ StringPool.get(a, n)
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

  @inline private[this] final def readBytes(n: Int): Array[Byte] = if (StringPool.arraySize >= n) { readbuffer.get(array, 0, n); array } else Array.fill(n)(readbuffer.get)

  @inline private[this] final def markLimit = limitmark = readbuffer.limit

  @inline private[this] final def markPosition = positionmark = readbuffer.position

  @inline private[this] final def advanceBuffer[A](a: A): A = {
    readbuffer.limit(limitmark)
    if (-1 < positionmark) { readbuffer.position(positionmark); positionmark = -1 }
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

  final private[aio] val empty = new Io(null, emptyBuffer, emptyBuffer, null, null, null, null, null, false)

  final private[aio] def apply(iteratee: Iteratee[Io, _]): Io = new Io(null, emptyBuffer, defaultByteBuffer, iteratee, null, null, null, null, true)

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

    object Cache {

      import java.util.concurrent.atomic._

      final val elementlength = new AtomicInteger(0)

      final val elementcached = new AtomicBoolean(false)

      final val elementarray = new AtomicReference[Array[Byte]]

      final val element = new AtomicReference[Iteratee[Io, Any]]

    }

    import Cache._

    object ReadHandler

      extends Handler[Integer, Io] {

      @inline final def completed(processed: Integer, io: Io) = {
        if (0 > processed) {
          io.release
        } else {
          io.readbuffer.flip
          if (!elementcached.get) elementlength.set(io.readbuffer.remaining)
          io.writebuffer.clear
          if (0 == processed)
            io.iteratee(Eof)
          else {
            val usecache = if (elementcached.get) {
              val peek = new Array[Byte](elementlength.get)
              io.readbuffer.mark
              try { io.readbuffer.get(peek) } catch { case _: Throwable ⇒ io.readbuffer.rewind }
              if (java.util.Arrays.equals(peek, elementarray.get))
                true
              else {
                io.readbuffer.rewind
                elementcached.set(false)
                elementlength.set(io.readbuffer.remaining)
                false
              }
            } else {
              io.readbuffer.mark
              false
            }
            val elem = if (usecache) (element.get, Elem(io)) else io.iteratee(Elem(io))
            elem match {
              case (cont @ Cont(_), Empty) ⇒
                read(io ++ cont ++ defaultByteBuffer)
              case (e @ Done(_), Elem(io)) ⇒
                if (elementcached.compareAndSet(false, true)) {
                  elementarray.set(new Array[Byte](elementlength.addAndGet(-io.readbuffer.remaining)))
                  io.readbuffer.rewind
                  io.readbuffer.get(elementarray.get)
                  element.set(e)
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
              case _ ⇒ if (0 < io.readbuffer.remaining) {
                io.renderable.renderFooter(io) ++ readiteratee
                val usecache = if (elementcached.get) {
                  val peek = new Array[Byte](elementlength.get)
                  io.readbuffer.mark
                  try { io.readbuffer.get(peek) } catch { case _: Throwable ⇒ io.readbuffer.rewind }
                  if (java.util.Arrays.equals(peek, elementarray.get))
                    true
                  else {
                    io.readbuffer.rewind
                    elementcached.set(false)
                    elementlength.set(io.readbuffer.remaining)
                    false
                  }
                } else {
                  io.readbuffer.mark
                  false
                }
                val elem = if (usecache) (element.get, Elem(io)) else io.iteratee(Elem(io))
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

      @inline def read(io: Io): Unit = if (0 < io.readbuffer.remaining) {
        write(io)
      } else {
        io.readbuffer.clear
        io.transfer.source.read(io.readbuffer, io, this)
      }

      @inline def write(io: Io) = io.transfer.destination.write(io.readbuffer, io, TransferWriteHandler)

      @inline def completed(processed: Integer, io: Io) = {
        io.readbuffer.flip
        if (0 < processed) {
          if (io.transfer.encoder.isDefined) {
            io.transfer.encoder.get.encode(io.readbuffer)
            io.readbuffer.flip
          }
          write(io)
        } else {
          if (io.transfer.encoder.isDefined) {
            io.transfer.encoder.get.finish(io.readbuffer)
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
      io.readbuffer.clear
      io.channel.read(io.readbuffer, io, ReadHandler)
    }

    @inline def process(io: Io): Unit = {
      processor.doProcess(io, ProcessHandler)
    }

    @inline def write(io: Io): Unit = {
      io.writebuffer.flip
      io.channel.write(io.writebuffer, io, WriteHandler)
    }

    @inline def unhandled(e: Any) = error("unhandled " + e)

    @inline def ignore = ()

    accept

  }

}
