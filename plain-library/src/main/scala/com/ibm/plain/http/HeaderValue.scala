package com.ibm

package plain

package http

import java.text.SimpleDateFormat

/**
 * Helpers to parse the values of header fields.
 */
trait HeaderValue[A] { def value(s: String): A }

object HeaderValue {

  /**
   * Header.value contains a list of Tokens.
   */
  trait TokenList extends HeaderValue[Array[String]] { final def value(s: String): Array[String] = s.split(",").map(_.trim) }

  /**
   * Header.value contains a String (identity).
   */
  trait StringValue extends HeaderValue[String] { final def value(s: String) = s.trim }

  /**
   * Header.value contains an Int.
   */
  trait IntValue extends HeaderValue[Int] { final def value(s: String) = s.trim.toInt }

  /**
   * Header.value contains a java.util.Date.
   */
  trait DateValue extends HeaderValue[java.util.Date] { final def value(s: String) = dateformat.parse(s.trim) }

  /**
   * The DateValue object provides the SimpleDateFormat used in http header fields.
   */
  private final val dateformat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z")

}

