import plain.PlainBuild._
import plain.Dependencies._

defaultSettings

formatSettings

libraryDependencies ++= Core.dependencies ++ Integration.dependencies ++ plain.Dependencies.Test.dependencies

