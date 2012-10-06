package com.ibm.plain

package lib

package crypt

import language.implicitConversions

/**
 * A simplifying wrapper around [[java.util.UUID]].
 *
 * @constructor Is private, use Uuid.newType4Uuid instead.
 */
class Uuid private (private[this] val uuid: java.util.UUID) {

  /**
   * Removes all "-", length is always 32, all lowercase.
   */
  override lazy val toString = uuid.toString.replace("-", "")

}

/**
 * Generator for different kinds of uuids.
 */
object Uuid {

  /**
   * removed [org.codehaus.aspectwerkz.proxy.Uuid], not unique even in small use cases
   */

  /**
   * Generates a "type 4" uuid.
   *
   * @return A "type 4" uuid (see [[java.util.UUID]] for more details).
   */
  def newUuid = new Uuid(java.util.UUID.randomUUID)

  def fromString(s: String) = new Uuid(java.util.UUID.fromString(s))

  /**
   * Converts Uuid to a string.
   */
  implicit def uuid2string(uuid: Uuid): String = uuid.toString

  implicit def string2uuid(s: String): Uuid = fromString(s)

}
