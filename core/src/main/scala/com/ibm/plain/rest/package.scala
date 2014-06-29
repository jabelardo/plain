package com.ibm

package plain

import config.CheckedConfig

package object rest

  extends CheckedConfig {

  import config._
  import config.settings._

  /**
   * Using scala.collection.X will allow to create it using mutable collections, but treat them as immutable. Clever users can still cast them, though.
   */
  type Form = scala.collection.Map[String, scala.collection.Set[String]]

  type MultipartForm = scala.collection.Map[String, Any]

  final implicit class Html(val xml: scala.xml.Elem)

    extends AnyVal {

    override final def toString = xml.toString

  }

}
