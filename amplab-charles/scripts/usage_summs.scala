import amplab.googletrace.Convert._
import amplab.googletrace.Join._
import amplab.googletrace.Protos._
import amplab.googletrace.Util._
import amplab.googletrace.TraceUtil._
import amplab.googletrace.Stored._
import amplab.googletrace.TaskAggregates._
import amplab.googletrace.Utilizations._

import amplab.googletrace.ToNumpy

import spark.RDD

val usage = readSavedUsage(sc, outDir + "/usage_with_jobs_and_machines")
val tasks = readSavedTasks(sc, outDir + "/tasks_with_jobs_and_machines")

val (jobUtil, taskUtil, taskUsageWAvg, uByM, uByMDaily) =
    makeUtilizations(tasks, usage)

def writeTaskUtils: Unit = {
  putTaskUtilizations(sc, reshard(8, taskUtil), outDir + "/task_utils")
}
def writeJobUtils: Unit = {
  putJobUtilizations(sc, reshard(2, jobUtil), outDir + "/job_utils_with_tasks")
}
def writeTaskUsageWAvg: Unit = {
  putTaskUsageWithAvg(sc, taskUsageWAvg, outDir + "/task_usage_w_avg")
}
def writeUsageByMachineWAvg: Unit = {
  putUsageByMachine(sc, uByM, outDir + "/usage_by_machine_w_avg")
  putUsageByMachine(sc, uByMDaily, outDir + "/usage_by_machine_w_avg_daily")
}

def usageIn(f: TaskUsageWithAvg => Double, u: UsageByMachine): Map[Int, Double] = {
  import scala.collection.JavaConversions._
  u.getComponentsWithAvgList.map(x => {
    val prio =
      if (x.getTask.getInfo.hasPriority)
        x.getTask.getInfo.getPriority
      else
        -1
    val weight = x.getUsage.getEndTime - x.getUsage.getStartTime
    val resources = f(x)
    (prio, (weight, resources))
  }).groupBy(_._1).map(kv => {
    val key = kv._1
    val values = kv._2.map(_._2)
    val weightSum = values.map(_._1).sum
    val resourceSum = values.map(x => x._1.asInstanceOf[Double] * x._2).sum
    key -> resourceSum / weightSum
  }).toMap
}

val MINUTE = 60L * 1000L * 1000L
def normalizeTime(t: Long): Long = t / (5 * MINUTE)

def sumPriority(in: RDD[(Long, Map[Int, Double])]): RDD[(Long, Seq[Double])] =
  in.groupByKey.mapValues {
    case measures =>
      (-1 to 11).map(i => measures.map(_.getOrElse(i, 0.0)).reduce(_ + _))
  }

val priorityCpuUsage = 
  sumPriority(uByM.map(u => normalizeTime(u.getStartTime) ->
                            usageIn(_.getUsage.getResources.getCpus, u)))

val priorityMemoryUsage = 
  sumPriority(uByM.map(u => normalizeTime(u.getStartTime) ->
                            usageIn(_.getUsage.getResources.getMemory, u)))

val priorityCpuReservation =
  sumPriority(uByM.map(u => normalizeTime(u.getStartTime) ->
                            usageIn(
                  _.getNextDeath.getInfo.getRequestedResources.getCpus, u)))

val priorityMemoryReservation =
  sumPriority(uByM.map(u => normalizeTime(u.getStartTime) ->
                            usageIn(
                  _.getNextDeath.getInfo.getRequestedResources.getMemory, u)))

def writePriority(name: String, data: RDD[(Long, Seq[Double])]): Unit = {
  data.map(kv =>
    kv._1 + " " + kv._2.mkString(" ")
  ).saveAsTextFile(outDir + "/" + name)
}

def writePriorities: Unit = {
  writePriority("cpu_time_prio", priorityCpuUsage)
  writePriority("mem_time_prio", priorityMemoryUsage)
  writePriority("cpu_res_time_prio", priorityCpuReservation)
  writePriority("mem_res_time_prio", priorityMemoryReservation)
}


def doRun: Unit = {
    jobUtil.cache
    taskUtil.cache
    writeTaskUtils
    writeJobUtils
    writeTaskUsageWAvg
    writeUsageByMachineWAvg
    writePriorities
}
