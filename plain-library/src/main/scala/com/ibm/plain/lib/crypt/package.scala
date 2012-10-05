package com.ibm.plain

package lib

import lib.config.CheckedConfig

package object crypt

  extends CheckedConfig {

  /**
   * Convert bytes or a string into an MD5 hash simply by calling MD5(...).
   */
  object MD5 extends DigestMethod("MD5")

  /**
   * Convert bytes or a string into an SHA-1 hash simply by calling SHA1(...).
   */
  object SHA1 extends DigestMethod("SHA-1")

  /**
   * Convert bytes or a string into an SHA-256 hash simply by calling SHA256(...).
   */
  object SHA256 extends DigestMethod("SHA-256")

}