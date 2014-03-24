package com.ibm

package plain

package aio

/**
 *
 */
trait Message

/**
 * An InMessage is send from the Initiator of an Exchange (the Client) to the accepting EndPoint of the Exchange (the Server).
 * In Http, for example, the Request.
 */
trait InMessage

  extends Message

/**
 * An OutMessage is send from the Server to the Client of the Exchange. In Http, for example, the Response.
 */
trait OutMessage

  extends Message

