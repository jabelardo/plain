import plain.PlainBuild._
import plain.Dependencies._

defaultSettings

formatSettings

libraryDependencies ++= Core.dependencies ++ Jdbc.dependencies ++ plain.Dependencies.Test.dependencies

