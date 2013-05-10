package com.ibm

package plain

package servlet

import javax.servlet.{ RequestDispatcher ⇒ JRequestDispatcher, ServletRequest, ServletResponse }

/**
 *
 */
final class RequestDispatcher

  extends JRequestDispatcher {

  final def forward(request: ServletRequest, response: ServletResponse) = unsupported

  final def include(request: ServletRequest, response: ServletResponse) = unsupported

}

