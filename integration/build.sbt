import plain.PlainBuild._
import plain.Dependencies._

defaultSettings

libraryDependencies ++= Core.dependencies ++ Integration.dependencies ++ Testing.dependencies

