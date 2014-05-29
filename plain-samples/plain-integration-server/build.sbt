import sbtassembly.Plugin._
import AssemblyKeys._
import Integration._

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
	val l = cp filter { excludedIntegrationJars contains _.data.getName }
	println("excludedJars : \n" + l.mkString("\n"))
println("unmanaged " + unmanagedBase);
	l
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

