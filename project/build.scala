import sbt._
import Keys._
import org.scalatra.sbt._
import org.scalatra.sbt.PluginKeys._
import com.mojolly.scalate.ScalatePlugin._
import ScalateKeys._

object MedicalCaseSearchBuild extends Build {
  val Organization = "com.pragmaticideal"
  val Name = "Medical Case Search"
  val Version = "0.1.0-SNAPSHOT"
  val ScalaVersion = "2.11.6"
  val ScalatraVersion = "2.3.0"
  val LuceneVersion = "5.2.1"

  lazy val project = Project (
    "medical-case-search",
    file("."),
    settings = ScalatraPlugin.scalatraSettings ++ scalateSettings ++ Seq(
      organization := Organization,
      name := Name,
      version := Version,
      scalaVersion := ScalaVersion,
      resolvers += Classpaths.typesafeReleases,
      resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
      libraryDependencies ++= Seq(
        // Scalatra
        "org.scalatra" %% "scalatra" % ScalatraVersion,
        "org.scalatra" %% "scalatra-scalate" % ScalatraVersion,
        "org.scalatra" %% "scalatra-json" % ScalatraVersion,
        "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
        // 3rd Party
        "org.json4s"   %% "json4s-jackson" % "3.2.9",
        "org.scala-lang.modules" %% "scala-pickling" % "0.10.1",
        "commons-io" % "commons-io" % "2.4",
        "org.apache.commons" % "commons-compress" % "1.9",
        "com.github.scopt" %% "scopt" % "3.3.0",
        // Lucene
        "org.apache.lucene" % "lucene-core" % LuceneVersion,
        "org.apache.lucene" % "lucene-analyzers-common" % LuceneVersion,
        "org.apache.lucene" % "lucene-queryparser" % LuceneVersion,
        // Servlet basics
        "ch.qos.logback" % "logback-classic" % "1.1.2" % "runtime",
        "org.eclipse.jetty" % "jetty-webapp" % "9.2.10.v20150310",
        "org.eclipse.jetty" % "jetty-server" % "9.2.10.v20150310",
        "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
        // Scalatest
        "org.scalatest" %% "scalatest" % "2.2.1" % "test" withSources() withJavadoc(),
        "org.scalacheck" %% "scalacheck" % "1.12.1" % "test" withSources() withJavadoc()
      ),
      scalateTemplateConfig in Compile <<= (sourceDirectory in Compile){ base =>
        Seq(
          TemplateConfig(
            base / "webapp" / "WEB-INF" / "templates",
            Seq.empty,  /* default imports should be added here */
            Seq(
              Binding("context", "_root_.org.scalatra.scalate.ScalatraRenderContext", importMembers = true, isImplicit = true)
            ),  /* add extra bindings here */
            Some("templates")
          )
        )
      }
    )
  )
}
