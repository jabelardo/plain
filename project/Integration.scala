import sbt._
import Keys._

object Integration {

	def camelVersion = "2.13.1"

	def activemqVersion = "5.10.0"

	def core = Seq(
                "org.apache.camel" % "camel-scala" % camelVersion,
                "org.apache.camel" % "camel-exec" % camelVersion,
                "org.apache.camel" % "camel-ssh" % camelVersion,
                "org.apache.camel" % "camel-stream" % camelVersion,
                "org.apache.camel" % "camel-quartz2" % camelVersion,
                "org.apache.camel" % "camel-spring-batch" % camelVersion,
                "org.apache.camel" % "camel-test" % camelVersion,
                "org.apache.camel" % "camel-core" % camelVersion
	) 

	def messaging = Seq(
                "org.apache.camel" % "camel-sjms" % camelVersion,
                "org.apache.camel" % "camel-mail" % camelVersion,
		"org.apache.activemq" % "activemq-camel" % activemqVersion,
		"org.apache.activemq" % "activemq-broker" % activemqVersion,
                "org.apache.activemq" % "activemq-jaas" % activemqVersion,
		"org.apache.activemq" % "activemq-kahadb-store" % activemqVersion
	)

        def persistence = Seq(
		"org.apache.activemq.protobuf" % "activemq-protobuf" % "1.1",
                "org.apache.camel" % "camel-jpa" % camelVersion,
                "org.apache.camel" % "camel-mybatis" % camelVersion,
                "org.apache.camel" % "camel-jdbc" % camelVersion,
                "org.apache.camel" % "camel-sql" % camelVersion,
                "org.apache.camel" % "camel-leveldb" % camelVersion,
		"com.typesafe.slick" %% "slick" % "2.1.0-M2"
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

	def integrationDependencies = core ++ networking ++ messaging ++ persistence

	def integrationSettings = Seq(

		resolvers ++= Seq(
        		"pentaho-releases" at "http://repository.pentaho.org/artifactory/repo/",
        		"fusesource-releases" at "http://repo.fusesource.com/nexus/content/groups/public/"
		),

                libraryDependencies ++= integrationDependencies
	
	)

}

