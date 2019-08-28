name := "reactivemongocantconnect"

scalaVersion := "2.12.8"

scalafmtOnCompile := true

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.5.23",
  "org.reactivemongo" %% "reactivemongo" % "0.18.4",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.23",
) map (_ % Runtime)

enablePlugins(PackPlugin)
