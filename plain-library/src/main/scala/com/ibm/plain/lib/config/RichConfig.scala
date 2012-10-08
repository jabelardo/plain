package com.ibm.plain

package lib

package config

import scala.collection.JavaConversions.seqAsJavaList

import com.typesafe.config.Config

/**
 * Add a couple of helpers to Config eg. get with default.
 */
class RichConfig(self: Config) {

  def getString(key: String, default: String) = if (self.hasPath(key)) self.getString(key) else default

  def getStringList(key: String, default: List[String]): java.util.List[String] =
    if (self.hasPath(key)) self.getStringList(key) else default

  def getInt(key: String, default: Int) = if (self.hasPath(key)) self.getInt(key) else default

  def getBoolean(key: String, default: Boolean) = if (self.hasPath(key)) self.getBoolean(key) else default

  def getBytes(key: String, default: Long): Long = if (self.hasPath(key)) self.getBytes(key) else default

}

