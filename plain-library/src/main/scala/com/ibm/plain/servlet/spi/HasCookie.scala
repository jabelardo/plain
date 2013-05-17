package com.ibm

package plain

package servlet

package spi

import javax.servlet.http.Cookie

trait HasCookie {

  final def getCookies: Array[Cookie] = unsupported

  final def addCookie(cookie: Cookie) = unsupported

}
