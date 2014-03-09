import sbt._
import Keys._

object Camel {

	def camelVersion = "2.12.3"

	def core = Seq(
                "org.apache.camel" % "camel-servlet" % camelVersion,
                "org.apache.camel" % "camel-jetty" % camelVersion,
                "org.apache.camel" % "camel-nagios" % camelVersion,
                "org.apache.camel" % "camel-ftp" % camelVersion,
                "org.apache.camel" % "camel-http4" % camelVersion,
                "org.apache.camel" % "camel-jdbc" % camelVersion,
                "org.apache.camel" % "camel-core" % camelVersion
	)

	def camelDependencies = core

	def camelSettings = Seq(

		resolvers ++= Seq(
        		"pentaho-releases" at "http://repository.pentaho.org/artifactory/repo/",
        		"fusesource-releases" at "http://repo.fusesource.com/nexus/content/groups/public/"
		),

                libraryDependencies ++= camelDependencies
	
	)

	def excludedCamelJars = Seq("geronimo-servlet_2.5_spec-1.2.jar", "javax.servlet-2.5.0.v201103041518.jar")

}

