package com.ibm

package plain

package http

import Status.ClientError
import text.fastSplit
import MimeType.`*/*`

/**
 *
 */
final case class Accept private (

  mimetypes: List[MimeType])

  extends AnyVal {

  import Accept._

}

/**
 *
 */
object Accept {

  /**
   *
   */
  def apply(headervalue: String): Accept = {

    @inline def range(r: String): (Double, MimeType) = (fastSplit(r, ';') match {
      case h :: Nil ⇒ (1.0, h)
      case h :: t :: Nil ⇒ fastSplit(t, '=') match {
        case k :: v :: Nil if "q" == k.trim ⇒ (v.trim.toDouble, h)
        case _ ⇒ (1.0, h)
      }
      case _ ⇒ throw ClientError.`415`
    }) match {
      case (q, m) ⇒ (q, MimeType(m.trim))
    }

    if (ignoreAcceptHeader) All else fastSplit(headervalue, ',') match {
      case l: List[String] ⇒ new Accept(l.map(range).sortWith { case (a, b) ⇒ a._1 > b._1 }.map(_._2))
      case _ ⇒ throw ClientError.`415`
    }
  }

  private[this] final val All = new Accept(List(`*/*`))

}

trait AcceptValue extends HeaderValue[Accept] {

  @inline def value(name: String) = Accept(name)

}
