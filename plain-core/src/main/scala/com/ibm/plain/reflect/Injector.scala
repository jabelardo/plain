package com.ibm

package plain

package reflect

import javax.annotation.Resource
import javax.sql.DataSource
import org.reflections._

import logging.Logger
import jdbc.dataSourceForJndiLookupName

final class Injector[A] private (any: A)

    extends Logger {

  def inject: A = {
    injectResource
    any
  }

  private[this] final def injectResource = {
    val resourcefields = any.getClass.getDeclaredFields.filter(_.isAnnotationPresent(classOf[Resource]))
    resourcefields.foreach { field ⇒
      val resource = field.getAnnotation(classOf[Resource])
      val lookup = if (0 < resource.name.length) resource.name else if (0 < resource.lookup.length) resource.lookup else if (0 < resource.mappedName.length) resource.mappedName else ""
      if (classOf[DataSource] == field.getType) {
        dataSourceForJndiLookupName(lookup) match {
          case Some(datasource) ⇒
            field.setAccessible(true)
            field.set(any, datasource)
            debug("@Resource " + any.getClass.getName + "." + field.getName + " injected.")
          case None ⇒ warn("@Resource " + any.getClass.getName + "." + field.getName + " not injected.")
        }
      } else {
        warn("@Resource injection not implemented for : " + field.getType)
      }
    }

  }

}

object Injector {

  final def apply[A](any: A): A = new Injector(any).inject

}