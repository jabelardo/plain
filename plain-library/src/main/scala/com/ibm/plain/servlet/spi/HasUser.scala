package com.ibm

package plain

package servlet

package spi

import java.security.Principal

/**
 *
 */
trait HasUser {

  final def getRemoteUser: String = unsupported

  final def getUserPrincipal: Principal = unsupported

  final def isUserInRole(role: String): Boolean = unsupported

  final def getAuthType: String = unsupported

}

