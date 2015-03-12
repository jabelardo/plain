package plain

import sbt._

object Dependencies {

  object Core {

    def logging = Seq(
      "com.typesafe" % "config" % "1.2.1",
      "org.slf4j" % "slf4j-api" % "1.7.10",
      "com.lmax" % "disruptor" % "3.3.0",
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.1",
      "org.apache.logging.log4j" % "log4j-api" % "2.1",
      "org.apache.logging.log4j" % "log4j-core" % "2.1")

    def commons = Seq(
      "com.googlecode.concurrentlinkedhashmap" % "concurrentlinkedhashmap-lru" % "1.4",
      "org.apache.commons" % "commons-lang3" % "3.3.2",
      "org.apache.commons" % "commons-compress" % "1.9",
      "commons-io" % "commons-io" % "2.4",
      "commons-net" % "commons-net" % "3.3",
      "commons-codec" % "commons-codec" % "1.10",
      "org.apache.httpcomponents" % "httpclient" % "4.3.6")

    def net = Seq(
      "org.glassfish.grizzly" % "grizzly-websockets" % "2.3.18",
      "org.glassfish.grizzly" % "grizzly-http" % "2.3.18",
      "com.ning" % "async-http-client" % "1.9.8")

    def reflection = Seq(
      "org.scala-lang" % "scala-compiler" % "2.11.6",
      "org.scala-lang" % "scala-reflect" % "2.11.6",
      "org.reflections" % "reflections" % "0.9.9") 

    def javax = Seq(
      "javax.servlet" % "javax.servlet-api" % "3.1.0",
      "javax.servlet" % "jstl" % "1.2",
      "org.glassfish.web" % "javax.servlet.jsp" % "2.3.2")

    def compress = Seq(
      "net.jpountz.lz4" % "lz4" % "1.3.0",
      "net.lingala.zip4j" % "zip4j" % "1.3.2")

    def json = Seq(
      "org.json4s" %% "json4s-native" % "3.2.11",
      "org.json4s" %% "json4s-jackson" % "3.2.11",
      "org.json4s" %% "json4s-ext" % "3.2.11",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.5.0",
      "com.sun.jersey" % "jersey-json" % "1.18.3")

    def time = Seq(
      "joda-time" % "joda-time" % "2.7"
    )

    def modules = Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.3")

    def dependencies = logging ++ commons ++ net ++ reflection ++ javax ++ compress ++ json ++ time ++ modules

  }

  object Jdbc {

    def jdbc = Seq(
      "mysql" % "mysql-connector-java" % "5.1.31",
      "org.apache.derby" % "derby" % "10.10.2.0",
      "org.apache.derby" % "derbyclient" % "10.10.2.0",
      "com.h2database" % "h2" % "1.4.181")

    def oracleJdbc = Seq(
      "com.oracle" % "ojdbc6" % "11.2.0.4")

    def microsoftJdbc = Seq(
      "com.microsoft.sqlserver" % "sqljdbc4" % "4.0" % "provided")

    def dependencies = jdbc ++ oracleJdbc

  }

  object Integration {

    def camelVersion = "2.14.1"

    def activemqVersion = "5.11.0"

    def slickVersion = "2.1.0"

    def core = Seq(
      "org.apache.camel" % "camel-core" % camelVersion,
      "org.apache.camel" % "camel-scala" % camelVersion,
      "org.apache.camel" % "camel-exec" % camelVersion exclude ("commons-exec", "commons-exec"),
      "org.apache.commons" % "commons-exec" % "1.2",
      "org.apache.camel" % "camel-ssh" % camelVersion,
      "org.apache.camel" % "camel-stream" % camelVersion,
      "org.apache.camel" % "camel-zipfile" % camelVersion,
      "org.apache.camel" % "camel-quartz2" % camelVersion,
      "org.apache.camel" % "camel-test" % camelVersion)

    def messaging = Seq(
      "org.apache.camel" % "camel-sjms" % camelVersion,
      "org.apache.camel" % "camel-mail" % camelVersion,
      "org.apache.activemq" % "activemq-camel" % activemqVersion,
      "org.apache.activemq" % "activemq-broker" % activemqVersion,
      "org.apache.activemq" % "activemq-jaas" % activemqVersion,
      "org.apache.activemq" % "activemq-kahadb-store" % activemqVersion)

    def persistence = Seq(
      "org.apache.activemq.protobuf" % "activemq-protobuf" % "1.1", 
      "org.apache.camel" % "camel-leveldb" % camelVersion,
      "com.typesafe.slick" %% "slick-extensions" % slickVersion,
      "com.typesafe.slick" %% "slick" % slickVersion)

    def networking = Seq(
      "org.apache.camel" % "camel-ahc" % camelVersion,
      "org.apache.camel" % "camel-ldap" % camelVersion,
      "org.apache.camel" % "camel-servlet" % camelVersion)

    def dependencies = core ++ networking ++ messaging ++ persistence ++ Jdbc.dependencies

  }

  object Testing {

    def test = Seq(
      "junit" % "junit" % "4.11" % "test",
      "com.novocode" % "junit-interface" % "0.11" % "test")

    def dependencies = test

  }

}

