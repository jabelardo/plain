import sbt._
import Keys._

object Plain {

	lazy val logging = Seq(
		"com.typesafe" % "config" % "1.2.0",
		"org.slf4j" % "slf4j-api" % "1.7.6",
		"com.lmax" % "disruptor" % "3.2.0",
		"org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.0-rc1",
		"org.apache.logging.log4j" % "log4j-api" % "2.0-rc1",
		"org.apache.logging.log4j" % "log4j-core" % "2.0-rc1"
	)

	lazy val commons = Seq(
		"com.googlecode.concurrentlinkedhashmap" % "concurrentlinkedhashmap-lru" % "1.4",
		"org.apache.commons" % "commons-lang3" % "3.2.1",
		"org.apache.commons" % "commons-compress" % "1.7",
		"commons-io" % "commons-io" % "2.4",
		"commons-net" % "commons-net" % "3.3",
		"commons-codec" % "commons-codec" % "1.9"
	)

	lazy val reflection = Seq(
		"org.scala-lang" % "scala-reflect" % "2.10.3",
		"org.reflections" % "reflections" % "0.9.8"
	)

	lazy val javax = Seq(
		"javax.servlet" % "javax.servlet-api" % "3.1.0",
		"javax.servlet" % "jstl" % "1.2",
		"org.glassfish.web" % "javax.servlet.jsp" % "2.3.2"
	)

	lazy val compress = Seq(
		"net.jpountz.lz4" % "lz4" % "1.2.0",
		"net.lingala.zip4j" % "zip4j" % "1.3.1"
	)

	lazy val json = Seq(
		"com.fasterxml.jackson.core" % "jackson-databind" % "2.3.0",
		"com.sun.jersey" % "jersey-json" % "1.18"
	)

	lazy val jdbc = Seq(
		"mysql" % "mysql-connector-java" % "5.1.29",
		"org.apache.derby" % "derby" % "10.10.1.1",
		"org.apache.derby" % "derbyclient" % "10.10.1.1",
		"com.h2database" % "h2" % "1.3.174"
	)

	// unmanaged
	lazy val commercialjdbc = Seq(
		"com.oracle" % "ojdbc" % "11.2.0",
		"com.microsoft.sqlserver" % "sqljdbc4" % "4.0"
	)

	lazy val test = Seq(
		"junit" % "junit" % "4.11" % "test",
		"com.novocode" % "junit-interface" % "0.10" % "test"
	)

	lazy val basicDependencies = logging ++ commons ++ reflection ++ compress ++ json ++ javax ++ test

}

