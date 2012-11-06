package com.ibm

package plain

package rest

package resource

import rest.PlainDSLResource

class DSLResource extends PlainDSLResource {

  val path = "some/path"

  onGET(Map.empty) {
    use(Response, Entity, URLParams) {
      case (req, entity, List(asInt(s1), s2)) ⇒
        Ok("HUHU")
    }
  } || {
    Ok("Oder so")
  } || {
    use(Response, Entity) {
      case (entity, e) ⇒
        Ok("Und so weiter...")
    }
  }

}