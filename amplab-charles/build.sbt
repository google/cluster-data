import sbtprotobuf.{ProtobufPlugin=>PB}

seq(PB.protobufSettings: _*)

name := "google-trace-analysis"

version := "1.0"

scalaVersion := "2.9.1"

scalacOptions := Seq("-deprecation", "-unchecked", "-optimise", "-Yinline")

organization := "edu.berkeley.cs.amplab"

resolvers += "Apache Snapshots" at "https://repository.apache.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-core" % "0.20.205.0",
  "org.apache.hive" % "hive-serde" % "0.7.1-SNAPSHOT",
  "log4j" % "log4j" % "1.2.16",
  "org.spark-project" %% "spark-core" % "0.4-SNAPSHOT",
  "org.spark-project" %% "spark-repl" % "0.4-SNAPSHOT",
  "org.scalatest" %% "scalatest" % "1.6.1" % "test",
  "org.apache.commons" % "commons-math" % "2.2"
)

testOptions in Test += Tests.Argument("-oDF")

protoc in PB.protobufConfig := "./protoc --plugin=protoc-gen-twadoop=./protoc-gen-twadoop --twadoop_out=target/scala-2.9.1/src_managed/main/compiled_protobuf"

fork in run := true

javaOptions ++= Seq("-Djava.library.path=" +
    System.getProperty("java.library.path") + ":./native-lib-" +
    System.getProperty("os.name") + "-" + System.getProperty("os.arch"),
    "-Dspark.home=./spark-home",
    "-Dspark.kryo.registrator=amplab.googletrace.KryoRegistrator",
    "-Dspark.serializer=spark.KryoSerializer",
    "-Dspark.kryoserializer.buffer.mb=200",
    "-Dcom.sun.management.jmxremote")

mklauncherTask
