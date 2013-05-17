package com.ibm

package plain

package servlet

package spi

/**
 *
 */
trait HasEncoding {

  final def encodeURL(url: String) = unsupported

  final def encodeUrl(url: String) = encodeURL(url)

  final def encodeRedirectURL(url: String) = unsupported

  final def encodeRedirectUrl(url: String) = encodeRedirectURL(url)

}

