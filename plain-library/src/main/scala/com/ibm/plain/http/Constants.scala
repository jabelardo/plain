package com.ibm

package plain

package http

import scala.collection.immutable.{ BitSet, NumericRange, Range ⇒ SRange }

import text.ASCII

/**
 * Basic http request parsing constants.
 */
private[http] object RequestConstants {

  final val ` ` = ' '.toByte
  final val `\t` = '\t'.toByte
  final val `:` = ':'.toByte
  final val `\r` = '\r'.toByte
  final val del = 127.toByte

  final val `/` = "/"
  final val `?` = "?"
  final val `;` = ";"
  final val cr = "\r"
  final val lf = "\n"

  final val char = b(0 to 127)
  final val lower = b('a' to 'z')
  final val upper = b('A' to 'Z')
  final val alpha = lower | upper
  final val digit = b('0' to '9')
  final val hex = digit | b('a' to 'f') | b('A' to 'F')
  final val control = b(0 to 31) + del
  final val whitespace = b(' ', '\t')
  final val separators = whitespace | b('(', ')', '[', ']', '<', '>', '@', ',', ';', ':', '\\', '\"', '/', '?', '=', '{', '}')
  final val text = char -- control ++ whitespace
  final val token = char -- control -- separators
  final val gendelimiters = b(':', '/', '?', '#', '[', ']', '@')
  final val subdelimiters = b('!', '$', '&', '\'', '(', ')', '*', '+', ',', ';', '=')
  final val reserved = gendelimiters | subdelimiters
  final val unreserved = alpha | digit | b('-', '.', '_', '~')
  final val path = unreserved | subdelimiters | b(':', '@', '%')
  final val query = path | b('/', '?', '#')

  private[this] final def b(in: Int*): Set[Int] = BitSet(in: _*)
  private[this] final def b(in: SRange.Inclusive): Set[Int] = BitSet(in: _*)
  private[this] final def b(in: NumericRange.Inclusive[Char]): Set[Int] = BitSet(in.map(_.toInt): _*)

}

