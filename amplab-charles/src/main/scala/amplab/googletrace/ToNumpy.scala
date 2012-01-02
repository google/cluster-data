package amplab.googletrace

import java.io.{DataOutput, DataOutputStream, FileOutputStream}
import spark.RDD

object ToNumpy {
  trait Converter[T] extends Serializable {
    def writeNumpy(path: String, data: Array[T]): Unit
    def writeNumpy(path: String, data: RDD[T])(implicit cm: ClassManifest[T]): Unit = {
      /* XXX here and elsewhere we rely on this running from exactly one
         worker */
      Util.reshard(1, data).glom.foreach(writeNumpy(path, _))
    }
  }

  def converter[T](
    getValues: T => (Array[String], Array[Float]),
    fieldNames: (Array[String], Array[String])
  )(implicit cm: ClassManifest[T]): Converter[T] = {
    return new Converter[T] {
      val namesString =
          "[%s]".format(
            (fieldNames._1 ++ fieldNames._2).map("'" + _ + "'").mkString(",")
          )
      val typesString = 
          (List.fill(fieldNames._1.size)("'S40'") ++
           List.fill(fieldNames._2.size)("'>f4'")).mkString(",")
      val dtypeString =
          "{'names':%s,'formats':[%s]}".format(namesString, typesString)
      final def writeLine(t: T, out: DataOutput): Unit = {
        val (strings, floats) = getValues(t)
        assert(strings.size == fieldNames._1.size)
        assert(floats.size == fieldNames._2.size)
        for (s <- strings) {
          val padded =
            (s + "                                            ").substring(0, 40)
          out.writeBytes(padded)
        }
        for (f <- floats) {
          out.writeFloat(f)
        }
      }

      final def toRawBytes(data: Array[T]): Array[Byte] = {
        val byteOut = new java.io.ByteArrayOutputStream
        val out = new DataOutputStream(byteOut)
        data.foreach(writeLine(_, out))
        val result = byteOut.toByteArray
        out.close
        result
      }

      final def writeHeader(out: DataOutput, size: Long): Unit = {
        val metaString = 
            "{'descr':%s,'fortran_order':False,'shape':(%d,)}\n".format(
              dtypeString, size
            )
        out.writeBytes("\u0093NUMPY\u0001\u0000")
        val metaLen = metaString.size
        out.writeByte(metaLen % 256)
        out.writeByte(metaLen / 256)
        out.writeBytes(metaString)
      }
          
      override final def writeNumpy(path: String, data: Array[T]): Unit = {
        val out = new DataOutputStream(new FileOutputStream(path))
        writeHeader(out, data.size)
        data.foreach(writeLine(_, out))
        out.close
      }

      override final def writeNumpy(path: String, data: RDD[T])(implicit cm: ClassManifest[T]): Unit = {
        val size = data.count
        Util.reshard(1, data.glom.map(toRawBytes(_))).glom.foreach(
          byteArrays => {
            val out = new DataOutputStream(new FileOutputStream(path))
            writeHeader(out, size)
            byteArrays.foreach(out.write(_))
            out.close
          }
        )
      }
    }
  }

  import TraceUtil._
  def jobUtilizationConverter(sample: JobUtilization):
      Converter[JobUtilization] = {
    import scala.collection.JavaConversions._
    def percentileString(f: Float): String = {
      if (f == 1.0)
        return "max"
      else
        return "%d".format((f * 100.0).asInstanceOf[Int])
    }
    val percentileTypes =
      sample.getTaskPercentileList.zip(sample.getUsagePercentileList).map {
        case (taskPer, usagePer) => "t%s_pt%s".format(
          percentileString(taskPer), percentileString(usagePer)
        )
      }
    def getValues(t: JobUtilization): (Array[String], Array[Float]) = {
      (
        Array[String](
          t.getJobInfo.getName,
          t.getJobInfo.getLogicalName,
          t.getJobInfo.getUser
        ),
        Array[Float](
          t.getJobInfo.getSchedulingClass,
          t.getTaskSamples(0).getPriority,
          if (t.getTaskSamples(0).getDifferentMachines)
            0f
          else
            1f,
          t.getStartTime,
          t.getEndTime,
          t.getEndTime - t.getStartTime,
          t.getRunningTime,
          t.getNumTasks,
          t.getMinRequest.getCpus,
          t.getMinRequest.getMemory,
          t.getMaxRequest.getCpus,
          t.getMaxRequest.getMemory 
        ) ++ percentileTypes.indices.flatMap(
          i => List(t.getPercentileTaskUsage(i).getCpus,
                    t.getPercentileTaskUsage(i).getMemory,
                    t.getPercentileMeanTaskUsage(i).getCpus,
                    t.getPercentileMeanTaskUsage(i).getMemory)
        ) ++ (0 to 8).map(
          i => t.getNumEventsByType(i).floatValue
        ) ++ (0 to 8).map(
          i => t.getNumTasksFinal(i).floatValue
        ) ++ List(
          t.getTaskSamplesList.map(_.getNumMissing).sum.floatValue,
          t.getTaskSamplesList.map(_.getNumEvents).sum.floatValue
        )
      )
    }
    val stringNames = Array[String](
        "name", "logical_name", "user"
    )
    val floatNames = Array[String](
        "scheduling_class", "priority", "different_machines",
        "start_time", "end_time", "duration", "running_time",
        "num_tasks",
        "min_req_cpus", "min_req_memory",
        "max_req_cpus", "max_req_memory" 
    ) ++ percentileTypes.flatMap(
      n => List("max_cpus", "max_mem", "mean_cpu", "mean_mem").map(n + "_" + _)
    ) ++ List("num_submit", "num_schedule", "num_evict", "num_fail",
              "num_finish", "num_kill", "num_lost", "num_update_pending",
              "num_update_running"
    ) ++ List("num_final_submit", "num_final_schedule", "num_final_evict", "num_final_fail",
              "num_final_finish", "num_final_kill", "num_final_lost", "num_final_update_pending",
              "num_final_update_running", "num_missing", "num_events")
    converter(getValues, (stringNames, floatNames))
  }
}
