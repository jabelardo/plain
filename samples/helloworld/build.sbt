import plain.PlainBuild._
import plain.Dependencies._

applicationSettings

libraryDependencies ++= Core.dependencies ++ Integration.dependencies 

mainClass in (Compile) := Some("com.ibm.plain.bootstrap.Main")

packageArchetype.java_application

