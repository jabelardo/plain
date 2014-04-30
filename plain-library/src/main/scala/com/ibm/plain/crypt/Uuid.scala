package com.ibm

package plain

package crypt

import java.util.UUID

import language.implicitConversions
import scala.util.control.Breaks.TryBlock

/**
 * A simplifying wrapper around java.util.UUID.
 *
 * @constructor Is private, use Uuid.newType4Uuid instead.
 */
final class Uuid private (private[this] val uuid: UUID) {

  /**
   * Returns the original uuid formatted string: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
   */
  final val render = uuid.toString.toLowerCase

  /**
   * Removes all "-", length is always 32, all lowercase.
   */
  override final val toString = render.replace("-", "")

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
   * @return A "type 4" uuid (see java.util.UUID for more details).
   */
  def newUuid = new Uuid(UUID.randomUUID)

  /**
   * Create a type 4 uuid from a string. Only two valid string formats are valid: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx or x*32
   */
  def fromString(s: String): Uuid = {
    if (s.contains('-')) {
      require(36 == s.length && 4 == s.length - s.replace("-", "").length, "Invalid uuid string representation: " + s)
      new Uuid(UUID.fromString(s))
    } else {
      require(32 == s.length, "Invalid uuid string representation: " + s)
      new Uuid(UUID.fromString(s.substring(0, 8) + "-" + s.substring(8, 12) + "-" + s.substring(12, 16) + "-" + s.substring(16, 20) + "-" + s.substring(20, 32)))
    }
  }

  def isValid(s: String) = try2Boolean(fromString(s))

  /**
   * Converts Uuid to a string.
   */
  implicit def uuid2string(uuid: Uuid): String = uuid.toString

  /**
   * Converts a string to a Uuid.
   */
  implicit def string2uuid(s: String): Uuid = fromString(s)

}
