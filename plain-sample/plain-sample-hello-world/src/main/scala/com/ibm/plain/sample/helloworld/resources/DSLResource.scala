package com.ibm

package plain

package sample.helloworld.resources

import rest.PlainDSLResource

class DSLResource

  extends PlainDSLResource("some/path") {

  onGET {
    Ok("Huhuuu")
  }

}