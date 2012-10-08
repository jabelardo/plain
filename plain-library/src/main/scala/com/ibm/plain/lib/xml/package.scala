package com.ibm.plain

package lib

import scala.annotation.meta.field

import com.ibm.plain.lib.config.CheckedConfig

import javax.xml.bind.annotation.{ XmlAnyElement, XmlAttribute, XmlElement, XmlElementRef, XmlElementRefs, XmlElementWrapper, XmlTransient }
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

package object xml

  extends CheckedConfig {

  import config.settings._

  final val formattedOutput = getBoolean("plain.xml.formatted-output")

  final val unmarshalXml = XmlMarshaled

  type xmlAnyElement = XmlAnyElement @field
  type xmlAttribute = XmlAttribute @field
  type xmlElement = XmlElement @field
  type xmlElementRef = XmlElementRef @field
  type xmlElementRefs = XmlElementRefs @field
  type xmlElementWrapper = XmlElementWrapper @field
  type xmlJavaTypeAdapter = XmlJavaTypeAdapter @field
  type xmlTransient = XmlTransient @field

}
