package com.ibm

package plain

package integration

package spaces

import bootstrap.ExternalComponent
import logging.Logger
import concurrent.scheduleOnce

/**
 *
 */
final class Spaces

  extends ExternalComponent[Spaces]("plain-integration-spaces")

  with Logger {

  override def order = bootstrapOrder

  override def start = {

//    println("spaces component")
//
//    println(spacesConfig.toArray.toList)
//
//    scheduleOnce(5000) {
//      SpaceClient.get("http://localhost:8080/ping")
//      SpaceClient.get("http://localhost:8080/ping")
//      SpaceClient.get("http://localhost:8080/ping")
//      SpaceClient.get("http://localhost:8080/ping")
//      SpaceClient.get("http://localhost:8080/ping")
//    }

    this
  }

}


