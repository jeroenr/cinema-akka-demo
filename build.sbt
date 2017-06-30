import ReleaseTransformations._
import com.typesafe.sbt.packager.docker.Cmd

name          := """cinema-service"""
organization  := "com.github.jeroenr"
scalaVersion  := "2.11.8"

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

resolvers += Resolver.jcenterRepo

libraryDependencies ++= {
  val akkaV            = "2.4.17"
  val akkaHttpV	       = "10.0.8"	
  val mongoDriverV     = "2.1.0"
  val univocityV       = "2.4.1"
  val specs2V          = "3.9.1"
  val ficusV           = "1.2.4"
  val slf4sV           = "1.7.25"
  val logbackV         = "1.2.3"
  val jodaV            = "2.9.9"
  val commonsV         = "1.6"

  Seq(
    "com.typesafe.akka" %% "akka-http"                         % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-spray-json" 	           % akkaHttpV,
    "com.typesafe.akka" %% "akka-slf4j"                        % akkaV,
    "org.mongodb.scala" %% "mongo-scala-driver"                % mongoDriverV,
    "com.univocity"     %  "univocity-parsers"                 % univocityV,
    "joda-time"         % "joda-time"                          % jodaV,
    "commons-validator" % "commons-validator"                  % commonsV,
    "org.slf4s"         %% "slf4s-api"                         % slf4sV,
    "com.iheart"        %% "ficus"                             % ficusV,
    "ch.qos.logback"    % "logback-classic"                    % logbackV,
    "org.specs2"        %% "specs2-core"                       % specs2V       % Test,
    "com.typesafe.akka" %% "akka-stream-testkit"               % akkaV            % Test,
    "com.typesafe.akka" %% "akka-http-testkit"                 % akkaHttpV        % Test
  )
}

scalacOptions := Seq(
  "-encoding", "utf8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-target:jvm-1.8",
  "-Xlog-reflective-calls",
  "-Ypatmat-exhaust-depth", "40",
  "-Xmax-classfile-name", "240", // for docker container
  "-optimise"
)

publishMavenStyle := true
publishArtifact in Test := false
releasePublishArtifactsAction := PgpKeys.publishSigned.value
pomIncludeRepository := { _ => false }
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
pomExtra :=
  <url>https://github.com/jeroenr/</url>
  <licenses>
    <license>
      <name>Apache-2.0</name>
      <url>http://opensource.org/licenses/Apache-2.0</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>https://github.com/jeroenr/cinema-service</url>
    <connection>scm:git:git@github.com:jeroenr/cinema-service.git</connection>
  </scm>

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _)),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
  pushChanges
)

mainClass in Compile := Some("com.github.jeroenr.cinema.Boot")

updateOptions := updateOptions.value.withCachedResolution(true)

// docker
enablePlugins(JavaServerAppPackaging)
enablePlugins(DockerPlugin)

publishArtifact in (Compile, packageDoc) := false

val shortCommit = ("git rev-parse --short HEAD" !!).replaceAll("\\n", "").replaceAll("\\r", "")

packageName in Docker := name.value
version in Docker     := shortCommit
dockerBaseImage       := "airdock/oracle-jdk:jdk-1.8"
defaultLinuxInstallLocation in Docker := s"/opt/${name.value}" // to have consistent directory for files
dockerRepository := Some("eu.gcr.io")

