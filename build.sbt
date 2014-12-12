sbtPlugin := true

organization := "net.valraiso.sbt"

name := "sbt-minimize"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.webjars" % "mkdirp" % "0.3.5",
  "org.webjars" % "when-node" % "3.2.2"
)

resolvers ++= Seq(
  "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.url("sbt snapshot plugins", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns),
  Resolver.sonatypeRepo("snapshots"),
  "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/",
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
)

addSbtPlugin("com.typesafe.sbt" %% "sbt-js-engine" % "1.0.0")

publishMavenStyle := true

publishTo := Some(Resolver.file("file",  new File( Path.userHome.absolutePath+"/dev/apps/valraiso.github.com/releases" )) )

scriptedSettings

scriptedBufferLog := false

scriptedLaunchOpts <+= version apply { v => s"-Dproject.version=$v" }
