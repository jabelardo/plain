import Integration._
import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

mainClass in (Compile) := Some("com.ibm.plain.bootstrap.Main")

packageArchetype.java_application

