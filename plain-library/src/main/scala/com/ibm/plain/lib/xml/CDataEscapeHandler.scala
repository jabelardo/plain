package com.ibm.plain.lib.xml

import java.io.Writer

import com.sun.xml.bind.marshaller.NioEscapeHandler

object CDataEscapeHandler

  extends NioEscapeHandler("UTF-8") {

  override def escape(chars: Array[Char], start: Int, length: Int, isattributevalue: Boolean, out: Writer) = {
    if (chars.startsWith("<![CDATA[")) out.write(chars) else super.escape(chars, start, length, isattributevalue, out)
  }

}
