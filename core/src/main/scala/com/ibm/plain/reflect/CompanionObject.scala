package com.ibm.plain
package reflect

import scala.language.existentials

final case class CompanionObject(runtimeclass: Class[_]) {

  final val companionobject: Option[Any] = try Some(companion(runtimeclass)) catch { case _: Throwable ⇒ None }

}

