import sbt._
import Keys._

object Plain {

	def logging = Seq(
		"com.typesafe" % "config" % "1.2.0",
		"org.slf4j" % "slf4j-api" % "1.7.6",
		"com.lmax" % "disruptor" % "3.2.1",
		"org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.0-rc1",
		"org.apache.logging.log4j" % "log4j-api" % "2.0-rc1",
		"org.apache.logging.log4j" % "log4j-core" % "2.0-rc1"
	) 

	def commons = Seq(
		"com.googlecode.concurrentlinkedhashmap" % "concurrentlinkedhashmap-lru" % "1.4",
		"org.apache.commons" % "commons-lang3" % "3.3.1",
		"org.apache.commons" % "commons-compress" % "1.8",
		"commons-io" % "commons-io" % "2.4",
		"commons-net" % "commons-net" % "3.3",
		"commons-codec" % "commons-codec" % "1.9",
		"org.apache.httpcomponents" % "httpclient" % "4.3.3"
            
	) 

	def net = Seq(
		"com.ning" % "async-http-client" % "1.8.7"
	)

	def reflection = Seq(
		"org.scala-lang" % "scala-reflect" % "2.10.3",
		"org.reflections" % "reflections" % "0.9.8"
	)

	def javax = Seq(
		"javax.servlet" % "javax.servlet-api" % "3.1.0",
		"javax.servlet" % "jstl" % "1.2",
		"org.glassfish.web" % "javax.servlet.jsp" % "2.3.2"
	)

	def compress = Seq(
		"net.jpountz.lz4" % "lz4" % "1.2.0",
		"net.lingala.zip4j" % "zip4j" % "1.3.2"
	) 

	def json = Seq(
		"com.fasterxml.jackson.core" % "jackson-databind" % "2.3.2",
		"com.sun.jersey" % "jersey-json" % "1.18.1"
	) 

	def jdbc = Seq(
		"mysql" % "mysql-connector-java" % "5.1.29",
		"org.apache.derby" % "derby" % "10.10.1.1",
		"org.apache.derby" % "derbyclient" % "10.10.1.1",
		"com.h2database" % "h2" % "1.3.175"
	) 

	def commercialJdbc = Seq(
		"com.oracle" % "ojdbc" % "11.2.0",
		"com.microsoft.sqlserver" % "sqljdbc4" % "4.0"
	)

	def test = Seq(
		"junit" % "junit" % "4.11" % "test",
		"com.novocode" % "junit-interface" % "0.10" % "test"
	) 

	def plainDependencies = logging ++ commons ++ net ++ reflection ++ compress ++ json ++ javax ++ test

	def plainSettings = Defaults.defaultSettings ++ Seq(
		libraryDependencies ++= plainDependencies
	)

	def jdbcSettings = plainSettings ++ Seq(
		libraryDependencies ++= plainDependencies ++ jdbc
	)

}

