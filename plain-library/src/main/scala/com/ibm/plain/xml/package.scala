package com.ibm

package plain

import scala.annotation.meta.field

import com.ibm.plain.xml.XmlMarshaled

import config.{ CheckedConfig, config2RichConfig }
import javax.xml.bind.annotation.{ XmlAnyElement, XmlAttribute, XmlElement, XmlElementRef, XmlElementRefs, XmlElementWrapper, XmlTransient }
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

package object xml

  extends CheckedConfig {

  import config.settings._
  import config._

  final val formattedOutput = getBoolean("plain.xml.formatted-output", false)

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
