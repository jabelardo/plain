package com.ibm.plain.aio

import java.security.KeyStore
import javax.net.ssl._
import java.nio.ByteBuffer
import com.ibm.plain.logging.HasDummyLogger
import javax.net.ssl.SSLEngineResult.HandshakeStatus
import java.io.FileInputStream
import scala.annotation.tailrec
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel }
import java.nio.channels.CompletionHandler

/*
 * Should be moved later to plain.http
 */
trait SSL extends HasDummyLogger {

  val channel: Channel
  val sslEngine = createServerEngine(createSSLContext)
  val session = sslEngine.getSession

  val dummyBuffer = ByteBuffer.allocateDirect(0)
  def defaultCompletionHandler(onCompleted: String = "", onFailed: String = "") = new CompletionHandler[Integer, String] {
    def completed(result: Integer, attachment: String) {
      if (attachment.length > 0) log.debug(attachment)
      if (onCompleted.length > 0) log.warning(onCompleted)
    }
    def failed(e: Throwable, attachment: String) { if (onFailed.length > 0) log.warning(onFailed) }
  }
  def defaultCompletionHandler: CompletionHandler[Integer, String] = defaultCompletionHandler()

  var finishedHandler: Option[Unit => Unit] = None

  def onFinishOnce(f: => Unit) {
    this.finishedHandler = Some({ _: Unit =>
      f
      finishedHandler = None
    })
  }

  def finishHandshake = finishedHandler match {
    case Some(h) => h()
    case None =>
  }

  case class HandlerContainer[A](attachment: A, handler: CompletionHandler[Integer, A])

  var unwrapState: Option[(ByteBuffer, ByteBuffer, HandlerContainer[_])] = None

  sslEngine.beginHandshake

  private[SSL] def createSSLContext: SSLContext = {
    val keyStoreFile = "/Users/michael/Documents/Workspaces/plain/keystore.jks";
    val passwd = "password".toCharArray;
    val jks = KeyStore.getInstance("JKS")
    val kmf = KeyManagerFactory.getInstance("SunX509")
    val tmf = TrustManagerFactory.getInstance("SunX509")
    val ctx = SSLContext.getInstance("TLS")

    jks.load(new FileInputStream(keyStoreFile), passwd);
    kmf.init(jks, passwd)
    tmf.init(jks)
    ctx.init(kmf.getKeyManagers, tmf.getTrustManagers, null)
    ctx
  }

  private[SSL] def createServerEngine(ctx: SSLContext): SSLEngine = {
    val engine = ctx.createSSLEngine;
    engine.setUseClientMode(false);
    engine.setNeedClientAuth(false)
    engine
  }

  @tailrec private[this] def runDelegatedTasks {
    val task = sslEngine.getDelegatedTask
    if (task != null) { log.debug("Running delegated task ..."); task.run; runDelegatedTasks }
  }

  def wrap[A](src: ByteBuffer, dst: ByteBuffer, handler: HandlerContainer[A] = HandlerContainer("Data written to channel ...", defaultCompletionHandler)) {
    import SSLEngineResult.Status._
    import HandshakeStatus._

    log.debug("Encrypting " + src.remaining + " bytes.")
    dst.clear
    log.debug("src befor wrap: " + format(src, false))
    log.debug("dst befor wrap: " + format(dst, false))
    val result = sslEngine.wrap(src, dst)
    log.debug("src after wrap: " + format(src, false))
    log.debug("dst after wrap: " + format(dst, false))
    log.debug("wrap result: " + result.toString.replace("\n", " / "))

    if (dst.flip.remaining > 0) { // Send something ...
      log.debug("Send encrypted data to channel.")
      channel.write(dst, handler.attachment, new CompletionHandler[Integer, A] {
        def completed(p: Integer, a: A) {
          log.debug("dst after sending: " + format(dst, false))
          handler.handler.completed(p, a)
        }

        def failed(p: Throwable, a: A) = handler.handler.failed(p, a)
      })
    }

    result.getStatus match {
      case OK ⇒ result.getHandshakeStatus match {
        case NOT_HANDSHAKING | FINISHED ⇒
          if (src.remaining > 0) wrap(src, dst, handler) else finishHandshake
        case NEED_WRAP ⇒ wrap(src, dst)
        case NEED_UNWRAP ⇒ unwrap
        case NEED_TASK ⇒ runDelegatedTasks
      }
      case CLOSED ⇒ // TODO ?
      case BUFFER_OVERFLOW | BUFFER_UNDERFLOW ⇒ throw new IllegalStateException // Should not happen ...
    }
  }

  def wrap[A](src: ByteBuffer, attachment: A, handler: CompletionHandler[Integer, A]) {
    val dst = defaultByteBuffer
    wrap(src, dst, HandlerContainer(attachment, new CompletionHandler[Integer, A] {
      def completed(processed: Integer, a: A) { releaseByteBuffer(dst); handler.completed(processed, a) }
      def failed(e: Throwable, a: A) { releaseByteBuffer(dst); handler.failed(e, a) }
    }))
  }

  def wrap {
    val dst = defaultByteBuffer
    wrap(dummyBuffer, dst)
    releaseByteBuffer(dst)
  }

  def unwrap[A](src: ByteBuffer, dst: ByteBuffer, handler: HandlerContainer[A] = HandlerContainer("", defaultCompletionHandler("Received unhandled Application Data!"))) {
    import SSLEngineResult.Status._
    import HandshakeStatus._

    log.debug("Decrypting " + src.remaining + " bytes.")
    dst.clear
    log.debug("src befor unwrap: " + format(src, false))
    log.debug("dst befor unwrap: " + format(dst, false))
    val result = sslEngine.unwrap(src, dst)
    log.debug("src after unwrap: " + format(src, false))
    log.debug("dst after unwrap: " + format(dst, false))
    log.debug("unwrap result: " + result.toString.replace("\n", " / "))

    if (dst.position > 0) {
      log.debug("Complete SSLCHannel read.")
      handler.handler.completed(result.bytesProduced, handler.attachment)
    }

    def unwrapWithNextHandler = unwrap(src, dst, handler)

    result.getStatus match {
      case OK ⇒ result.getHandshakeStatus match {
        case NOT_HANDSHAKING | FINISHED ⇒ if (src.remaining > 0) unwrapWithNextHandler else finishHandshake
        case NEED_UNWRAP ⇒ unwrapWithNextHandler
        case NEED_WRAP ⇒
          onFinishOnce {
            unwrap(dst, handler.attachment, handler.handler) // Read from Channel, which should send application data ...
          }
          unwrapState = Some((src, dst, handler))
          wrap
        case NEED_TASK ⇒ runDelegatedTasks; unwrapWithNextHandler
      }
      case CLOSED ⇒ // ToDo Maybe an exception? I don't know ...
      case BUFFER_UNDERFLOW ⇒ // TODO Save to small parts
      case BUFFER_OVERFLOW ⇒ throw new IllegalStateException // we hope that the defaultBufferSize is not to small
    }
  }

  def unwrap[A](dst: ByteBuffer, attachment: A, handler: CompletionHandler[Integer, A]) {
    val src = defaultByteBuffer
    channel.read(src, attachment, new CompletionHandler[Integer, A] {
      def completed(produced: Integer, a: A) {
        src.flip
        unwrap(src, dst, HandlerContainer(attachment, handler))
        releaseByteBuffer(src)
      }

      def failed(e: Throwable, a: A) {
        releaseByteBuffer(src)
        handler.failed(e, a)
      }
    })
  }

  def unwrap {
    unwrapState match {
      case Some((_, dst, HandlerContainer(attachment, handler))) =>
        unwrap(dst, attachment, handler)
      case _ =>
        val dst = defaultByteBuffer
        unwrap(dst, "A", new CompletionHandler[Integer, String] {
          def completed(processed: Integer, s: String) {
            log.warning("DUMMY COMPLETED")
            releaseByteBuffer(dst)
          }
          def failed(e: Throwable, s: String) = releaseByteBuffer(dst) // Warning?
        })
    }
  }

}