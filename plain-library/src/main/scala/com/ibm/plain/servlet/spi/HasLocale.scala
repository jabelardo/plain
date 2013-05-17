package com.ibm

package plain

package servlet

package spi

import java.util.{ Collections, Enumeration, Locale }

import scala.collection.JavaConversions.seqAsJavaList

/**
 *
 */
trait HasLocale {

  final def getLocale: Locale = locale

  final def getLocales: Enumeration[Locale] = Collections.enumeration(Locale.getAvailableLocales.toList)

  final def setLocale(locale: Locale) = this.locale = locale

  private[this] final var locale: Locale = Locale.getDefault

}

