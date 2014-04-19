import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

object Camel {

	def camelVersion = "2.13.0"

	def akkaVersion = "2.3.0"

	def activemqVersion = "5.9.0"

	def core = Seq(
                "org.apache.camel" % "camel-scala" % camelVersion,
                "org.apache.camel" % "camel-exec" % camelVersion,
                "org.apache.camel" % "camel-ssh" % camelVersion,
                "org.apache.camel" % "camel-stream" % camelVersion,
                "org.apache.camel" % "camel-fop" % camelVersion,
                "org.apache.camel" % "camel-quartz2" % camelVersion,
                "org.apache.camel" % "camel-spring-batch" % camelVersion,
                "org.apache.camel" % "camel-test" % camelVersion,
                "org.apache.camel" % "camel-core" % camelVersion
	) 

	def akka = Seq(
		"com.typesafe.akka" %% "akka-camel" % akkaVersion
	)

	def messaging = Seq(
                "org.apache.camel" % "camel-sjms" % camelVersion,
                "org.apache.camel" % "camel-mail" % camelVersion
	)

        def persistence = Seq(
                "org.apache.camel" % "camel-jpa" % camelVersion,
                "org.apache.camel" % "camel-mybatis" % camelVersion,
                "org.apache.camel" % "camel-jdbc" % camelVersion,
                "org.apache.camel" % "camel-sql" % camelVersion,
                "org.apache.camel" % "camel-leveldb" % camelVersion
        )

        def networking = Seq(
                "org.apache.camel" % "camel-jsch" % camelVersion,
                "org.apache.camel" % "camel-ldap" % camelVersion,
                "org.apache.camel" % "camel-netty" % camelVersion,
                "org.apache.camel" % "camel-netty-http" % camelVersion,
		"org.apache.camel" % "camel-servlet" % camelVersion,
                "org.apache.camel" % "camel-nagios" % camelVersion,
                "org.apache.camel" % "camel-ftp" % camelVersion,
                "org.apache.camel" % "camel-http4" % camelVersion,
		"org.apache.camel" % "camel-ahc" % camelVersion
        )

	def camelDependencies = core ++ akka ++ networking ++ messaging ++ persistence

	def camelSettings = Seq(

		resolvers ++= Seq(
        		"pentaho-releases" at "http://repository.pentaho.org/artifactory/repo/",
        		"fusesource-releases" at "http://repo.fusesource.com/nexus/content/groups/public/"
		),

                libraryDependencies ++= camelDependencies
	
	)

	def excludedCamelJars = Seq(
//		"jsp-2.1-6.1.14.jar",
//		"jsp-api-2.1-6.1.14.jar",
//		"jersey-server-1.8.jar",
//		"jersey-json-1.18.jar",
		"xml-apis-1.4.01.jar",
//		"jasper-runtime-5.5.12.jar",
//		"jasper-compiler-5.5.12.jar",
		"xmlpull-1.1.3.1.jar",
		"geronimo-servlet_2.5_spec-1.2.jar",
		"servlet-api-2.4.jar",
		"servlet-api-2.5-20081211.jar",
		"servlet-api-2.5-6.1.14.jar",
		"geronimo-servlet_3.0_spec-1.0.jar",
		"javax.servlet-2.5.0.v201103041518.jar"
	)

}

