package plain

import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin._
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.SbtNativePackager.NativePackagerKeys._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import com.typesafe.sbteclipse.plugin.EclipsePlugin._
import com.typesafe.sbteclipse.plugin.EclipsePlugin.EclipseKeys._
import net.virtualvoid.sbt.graph.Plugin.graphSettings

object PlainBuild

  extends Build {

  def defaultSettings = {
    graphSettings ++
      releaseSettings ++
      formatSettings ++
      Seq(
        organization := "com.ibm.plain",
        scalaVersion in ThisBuild := "2.11.1",
        crossScalaVersions in ThisBuild := Seq("2.11.1"),
        mainClass in (Compile, run) := Some("com.ibm.plain.bootstrap.Main"),
        publishTo := {
          def repo = if (version.value.trim.endsWith("SNAPSHOT")) "libs-snapshot-local" else "libs-release-local"
          Some("Artifactory Realm" at "http://pdmbuildvm.munich.de.ibm.com:8080/artifactory/" + repo)
        },
        credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
        createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource,
        eclipseOutput := Some("target/scala-2.11/classes"),
        withSource := true,
        incOptions := incOptions.value.withNameHashing(true),
        scalacOptions in (doc) := Seq("-diagrams", "-doc-title plain.io", "-access private"),
        scalacOptions ++= Seq(
          "-g:vars",
          "-encoding", "UTF-8",
          "-target:jvm-1.7",
          "-deprecation",
          "-feature",
          "-Yinline-warnings",
          "-Yno-generic-signatures",
          "-optimize",
          "-unchecked"),
        javacOptions ++= Seq(
          "-source", "1.8",
          "-target", "1.8",
          "-Xlint:unchecked",
          "-Xlint:deprecation",
          "-Xlint:-options"),
        resolvers ++= Seq(
	  "sonatype-releases" at "http://oss.sonatype.org/content/repositories/releases/",
	  "typesafe-releases" at "http://repo.typesafe.com/typesafe/releases/",
	  "scalasbt-releases" at "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/",
          "pentaho-releases" at "http://repository.pentaho.org/artifactory/repo/",
	  "codels" at "https://code.lds.org/nexus/content/groups/main-repo/",
          "fusesource-releases" at "http://repo.fusesource.com/nexus/content/groups/public/"))
  }

  def formatSettings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test := formattingPreferences)

  def formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
  }

  def applicationSettings = defaultSettings ++ Seq(fork in (Compile, run) := false)

  lazy val root = Project(
    id = "plain",
    base = file("."),
    aggregate = Seq(core, cqrs, integration, samples))

  lazy val libraries = Project(
    id = "plain-libraries",
    base = file("libraries"),
    aggregate = Seq(core, cqrs, integration))

  lazy val samples = Project(
    id = "samples",
    base = file("samples"),
    aggregate = Seq(integrationclient, integrationserver, jdbc, helloworld, servlet))

  lazy val core = Project(
    id = "plain-core",
    base = file("core"))

  lazy val cqrs = Project(
    id = "plain-cqrs",
    base = file("cqrs"),
    dependencies = Seq(core))

  lazy val integration = Project(
    id = "plain-integration",
    base = file("./integration"),
    dependencies = Seq(core))

  lazy val integrationclient = Project(
    id = "integrationclient",
    base = file("samples/integrationclient"),
    dependencies = Seq(integration))

  lazy val integrationserver = Project(
    id = "integrationserver",
    base = file("samples/integrationserver"),
    dependencies = Seq(integration))

  lazy val jdbc = Project(
    id = "jdbc",
    base = file("samples/jdbc"),
    dependencies = Seq(core))

  lazy val helloworld = Project(
    id = "helloworld",
    base = file("samples/helloworld"),
    dependencies = Seq(core))

  lazy val servlet = Project(
    id = "servlet",
    base = file("samples/servlet"),
    dependencies = Seq(core))

}
