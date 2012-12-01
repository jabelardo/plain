package com.ibm

package plain

package rest

import scala.reflect._
import scala.reflect.runtime.universe._

import http._
import http.MimeType._

/**
 *
 */
object Score {

  def apply(typ: Type, mimetype: MimeType): Double = inputmatrix.get((typ, mimetype)) match {
    case Some(score) ⇒ score
    case None ⇒ 0.0
  }

  private type ScoreMatrix = Map[(Type, MimeType), Double]

  private val inputmatrix: Map[(Type, MimeType), Double] = Map(
    (typeOf[Array[Byte]], `application/octet-stream`) -> 1.0,
    (typeOf[Predef.String], `text/plain`) -> 1.0)

}
