package com.ibm.plain.sample.helloworld

import com.ibm.plain.rest.PlainDSLDispatcher
import com.ibm.plain.sample.helloworld.resources.DSLResource

final class DSLDispatcher

	extends PlainDSLDispatcher(List(classOf[DSLResource])) {

}