import sbt._
import Keys._

import java.io.PrintWriter

object MyBuild extends Build {
  val Mklauncher = config("mklauncher") extend(Compile)
  val mklauncher = TaskKey[Unit]("mklauncher")
  val mklauncherTask = mklauncher <<= (target, fullClasspath in Runtime, javaOptions in Runtime) map { (target, cp, opts) =>
    def writeFile(file: File, str: String) {
      val writer = new PrintWriter(file)
      writer.println(str)
      writer.close()
    }
    val cpString = cp.map(_.data).mkString(":")
    val launchString = """
CLASSPATH="%s"
JAVA_OPTS="%s"
export JAVA_OPTS
scala -usejavacp -Djava.class.path="${CLASSPATH}" "$@"
""".format(cpString, opts.mkString(" "))
    val targetFile = (target / "scala-sbt").asFile
    writeFile(targetFile, launchString)
    targetFile.setExecutable(true)
  }

  lazy val root = Project(id = "google-trace-analysis",
                          base = file("."),
                          settings = Project.defaultSettings)
}
