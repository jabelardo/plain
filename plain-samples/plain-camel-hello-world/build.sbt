import sbtassembly.Plugin._
import AssemblyKeys._
import Camel._

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
	cp filter { excludedCamelJars contains _.data.getName }
}

