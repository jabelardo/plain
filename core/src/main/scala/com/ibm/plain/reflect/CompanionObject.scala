package com.ibm

package plain

package reflect

import scala.language.existentials

final case class CompanionObject[A](clazz: Class[A]) {

  final val companionobject: Option[A] = try Some(companion(clazz)) catch { case _: Throwable ⇒ None }

}

