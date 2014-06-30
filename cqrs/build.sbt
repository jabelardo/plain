import plain.PlainBuild._
import plain.Dependencies._

defaultSettings

libraryDependencies ++= Core.dependencies ++ Jdbc.dependencies ++ Testing.dependencies

