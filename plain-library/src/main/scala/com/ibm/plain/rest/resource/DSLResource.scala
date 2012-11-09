package com.ibm

package plain

package rest

package resource

import rest.PlainDSLResource

class DSLResource

  extends PlainDSLResource("some/$/path") {

  onGET {
    use(Entity, URLParams) {
      case (entity, List(asInt(s1))) ⇒
        Ok("HUHU")
    }
  }

}