package com.ibm.plain

package lib

package config

import com.typesafe.config.Config

class RichConfig(self: Config) {

  def getString(key: String, default: String) = if (self.hasPath(key)) self.getString(key) else default

  def getBytes(key: String, default: Long): Long = if (self.hasPath(key)) self.getBytes(key) else default

}

