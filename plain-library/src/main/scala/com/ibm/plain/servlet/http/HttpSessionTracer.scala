package com.ibm

package plain

package servlet

package http

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.{ AfterReturning, Aspect, Before }
import org.aspectj.lang.reflect.MethodSignature

//@Aspect
final class HttpSessionTracer {

  //  @Before("execution(* com.ibm.plain.servlet.*.*(..))")
  final def before(joinpoint: JoinPoint) = {
    val cname = simple(joinpoint)
    joinpoint.getSignature match {
      case method: MethodSignature ⇒
        val mname = method.getName
        val arglist = args(method.getParameterNames zip joinpoint.getArgs)
        println(s"enter : $cname.$mname$arglist")
      case _ ⇒
    }
  }

  //  @AfterReturning(pointcut = "execution(* com.ibm.plain.servlet.*.*(..))", returning = "result")
  final def afterReturning(joinpoint: JoinPoint, result: Object) = {
    val cname = simple(joinpoint)
    joinpoint.getSignature match {
      case method: MethodSignature ⇒
        val mname = method.getName
        val ret = rvalue(method.getReturnType, result)
        println(s"exit  : $cname.$mname <- $ret")
      case _ ⇒
    }
  }

  @inline private[this] final def simple(joinpoint: JoinPoint) = joinpoint.getSignature.getDeclaringType.getSimpleName

  @inline private[this] final def args(m: Array[(String, Object)]) = if (0 == m.size) "" else m.map(e ⇒ e._1 + "=" + e._2).mkString("(", ", ", ")")

  @inline private[this] final def rvalue(rclass: Class[_], value: Object) = rclass match {
    case _ ⇒ rclass.getSimpleName + "=" + ignoreOrElse(value.toString, "null")
  }

}

