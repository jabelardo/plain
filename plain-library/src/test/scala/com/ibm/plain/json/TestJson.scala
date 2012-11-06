package com.ibm

package plain

package json

import org.junit.Test
import javax.xml.bind.annotation.{ XmlAccessorType, XmlRootElement }
import javax.xml.bind.annotation._
import xml._

@XmlRootElement(name = "person")
@XmlAccessorType(XmlAccessType.PROPERTY)
case class Person(

  @xmlAttribute name: String,

  @xmlAttribute age: Int)

  extends XmlMarshaled

  with JsonMarshaled {

  def this() = this(null, -1)

}

@Test class TestJson {

  @Test def testA = {
    val joe = Person("joe", 33)
    assert("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><person name="joe" age="33"/>""" == joe.toXml)
    assert("""{"person":{"name":"joe","age":33}}""" == joe.toJson)
    assert(unmarshalJson(joe.toJson, classOf[Person]) == joe)
    assert(unmarshalXml(joe.toXml, classOf[Person]) == joe)
    assert(true)
  }

}

