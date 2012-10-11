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

  final val treadVersion10As11 = getBoolean("plain.http.allow-version-10-but-tread-it-like-11", false)

  final val treadAnyVersionAs11 = getBoolean("plain.http.allow-any-version-but-tread-it-like-11", false)

}
