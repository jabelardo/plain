package com.ibm.plain

package lib

import java.net.InetSocketAddress
import java.nio.channels.{ AsynchronousChannelGroup ⇒ Group, AsynchronousServerSocketChannel ⇒ ServerChannel }
import java.util.concurrent.{ ForkJoinPool, TimeUnit }

package object http

  extends config.CheckedConfig {

  import config._
  import config.settings._

  final val port = getInt("plain.http.port", 7500)

  final val backlog = getInt("plain.http.backlog", 10000)

  final val group = Group.withThreadPool(new ForkJoinPool)

  final val server = ServerChannel.open(group).bind(new InetSocketAddress(port), backlog)

  def serve = {
    println("serve")
    group.awaitTermination(300, TimeUnit.SECONDS)
    println("after group.await")
    server.close
    println("server closed")
    Thread.sleep(500)
    group.shutdown
    println("shutdown")
    Thread.sleep(500)
    if (!group.isTerminated) {
      Thread.sleep(5000)
      group.shutdownNow
      println("shutdownNow")
    }
    println("http ended.")
  }

}
