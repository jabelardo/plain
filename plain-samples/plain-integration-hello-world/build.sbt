import sbtassembly.Plugin._
import AssemblyKeys._
import Camel._

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
	cp filter { excludedCamelJars contains _.data.getName }
}

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
	{
		case x @ PathList("META-INF", xs @ _*) => (xs map {_.toLowerCase}) match {
			case ("spring.tooling" :: Nil) => MergeStrategy.filterDistinctLines
			case _ => old(x)
		}
		case x => old(x)
	}
}

