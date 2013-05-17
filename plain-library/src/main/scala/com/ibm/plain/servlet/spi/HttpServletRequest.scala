package com.ibm

package plain

package servlet

package spi

/**
 *
 */
trait HttpServletRequest

  extends HasContext

  with HasAttribute

  with HasParameter

  with HasDispatcher

  with HasContent

  with HasLocale

  with HasSocket

  with HasHeader

  with HasSession

  with HasUser

  with HasPath

  with HasCookie
  