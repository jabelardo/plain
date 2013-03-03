package com.ibm

package plain

package http

/**
 * We plan to support decoding only for 'chunked' or 'gzip' or 'chunked, gzip', for the latter we need first to un-gzip and then un-chunk it. Need to check if this is the right order.
 */
trait TransferDecoding

/**
 * Same as for decoding just vice versa.
 */
trait TransferEncoding

