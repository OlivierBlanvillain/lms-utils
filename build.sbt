/**
 * re-organized as per the new recommendations of sbt 0.13
 */
lazy val commonSettings = Seq(
  organization := "com.github.manojo",
  version := "0.1-SNAPSHOT",

  scalaVersion := "2.11.2",
  scalaOrganization := "org.scala-lang.virtualized",
  scalacOptions ++= Seq(
    "-Yvirtualize",
    //"-optimize",
    "-deprecation",
    "-feature",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:postfixOps"
    //"-Yinline-warnings"
  ),

  libraryDependencies ++= Seq(
    "org.scala-lang.lms" %% "lms-core" % "1.0.0",
    "org.scalatest" % "scalatest_2.11" % "2.2.2" % "test"
  ),

  resolvers += Resolver.sonatypeRepo("snapshots")

) ++ publishSettings ++ publishableSettings

lazy val root = (project in file(".")).
  settings(packagedArtifacts := Map.empty).
  aggregate(util, testutil)


lazy val util = (project in file("util")).
  settings(commonSettings: _*).
  settings(
    name := "lms-utils",
    /**
     * tests are not thread safe
     * this applies to all lms tests that write
     * to a file, and do diff tests
     */
    parallelExecution in Test := false
  ).
  dependsOn(testutil % "test -> compile")

lazy val testutil = (project in file("testutil")).
  settings(commonSettings: _*).
  settings(
    name := "lms-testutils"
  )

/**
 * We are able to publish this thing!
 */
lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishOnlyWhenOnMaster := publishOnlyWhenOnMasterImpl.value,
  publishTo <<= version { v: String =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },

  pomIncludeRepository := { x => false },
  publishArtifact in Compile := false,
  publishArtifact in Test := false,
  pomExtra := (
    <url>https://github.com/manojo/lms-utils</url>
    <inceptionYear>2015</inceptionYear>
    <licenses>
      <license>
        <name>MIT</name>
        <url>https://github.com/manojo/lms-utils/blob/master/LICENSE</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git:github.com/manojo/lms-utils.git</url>
      <connection>scm:git:git://github.com/manojo/lms-utils.git</connection>
    </scm>
    <issueManagement>
      <system>GitHub</system>
      <url>https://github.com/manojo/lms-utils/issues</url>
    </issueManagement>
  ),
  publishArtifact in (Compile, packageDoc) := false
)

lazy val publishOnlyWhenOnMaster = taskKey[Unit](
  "publish task for Travis (don't publish when building pull requests, only publish" +
  "when the build is triggered by merge into master)")

def publishOnlyWhenOnMasterImpl = Def.taskDyn {
  import scala.util.Try
  val travis   = Try(sys.env("TRAVIS")).getOrElse("false") == "true"
  val pr       = Try(sys.env("TRAVIS_PULL_REQUEST")).getOrElse("false") != "false"
  val branch   = Try(sys.env("TRAVIS_BRANCH")).getOrElse("??")
  val snapshot = version.value.trim.endsWith("SNAPSHOT")
  (travis, pr, branch, snapshot) match {
    case (true, false, "master", true) => publish
    case _                             => Def.task ()
  }
}

lazy val publishableSettings = Seq(
  publishArtifact in Compile := true,
  publishArtifact in Test := false,
  credentials ++= {
    val mavenSettingsFile = System.getenv("MAVEN_SETTINGS_FILE")
    if (mavenSettingsFile != null) {
      println("Loading Sonatype credentials from " + mavenSettingsFile)
      try {
        import scala.xml._
        val settings = XML.loadFile(mavenSettingsFile)
        def readServerConfig(key: String) = (settings \\ "settings" \\ "servers" \\ "server" \\ key).head.text
        Some(Credentials(
          "Sonatype Nexus Repository Manager",
          "oss.sonatype.org",
          readServerConfig("username"),
          readServerConfig("password")
        ))
      } catch {
        case ex: Exception =>
          println("Failed to load Maven settings from " + mavenSettingsFile + ": " + ex)
          None
      }
    } else {
      for {
        realm <- sys.env.get("SCALAMETA_MAVEN_REALM")
        domain <- sys.env.get("SCALAMETA_MAVEN_DOMAIN")
        user <- sys.env.get("SCALAMETA_MAVEN_USER")
        password <- sys.env.get("SCALAMETA_MAVEN_PASSWORD")
      } yield {
        println("Loading Sonatype credentials from environment variables")
        Credentials(realm, domain, user, password)
      }
    }
  }.toList
)

/**
 * Where does this go in this new way of organizing a build file?
 */
defaultScalariformSettings
