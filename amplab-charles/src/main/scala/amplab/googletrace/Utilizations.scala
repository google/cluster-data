package amplab.googletrace

import spark.SparkContext
import spark.RDD
import SparkContext._

import Util._
import Protos._
import TraceUtil._

import amplab.googletrace.mapreduce.output._
import amplab.googletrace.mapreduce.input._

import org.apache.hadoop.mapreduce.lib.input.TextInputFormat
import org.apache.hadoop.mapreduce.lib.input.{FileInputFormat}
import org.apache.hadoop.mapreduce.OutputFormat
import org.apache.hadoop.mapreduce.InputFormat
import org.apache.hadoop.io.LongWritable
import org.apache.hadoop.fs.Path
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce.{Job => HadoopJob}

import com.twitter.elephantbird.mapreduce.io.ProtobufWritable

import com.google.protobuf.Message

import scala.collection.SortedMap

object Utilizations {
  /* (value, weight) --> Map[partial sum of weight -> value] */
  def preparePercentileList(unsortedS: Seq[(Float, Float)]): SortedMap[Float, Float] = {
    val s = unsortedS.sortBy(_._1)
    val weightSums = s.map(_._2).scanLeft(0f)(_ + _).tail
    SortedMap(weightSums.zip(s.map(_._1)): _*)
  }

  def getPercentile(p: Double, s: SortedMap[Float, Float]): Float = {
    val place = (s.last._1 * p).asInstanceOf[Float]
    (s.to(place).lastOption, s.from(place).firstOption) match {
    case (Some((lowOffset, low)), None) => low
    case (_, Some((highOffset, high))) => high
    case (None, None) => throw new Error("getPercentileWeighted on nothing")
    }
  }

  val PER_TASK_PERCENTILES = List(0.01, 0.25, 0.5, 0.75, 0.99, 1.0)
  val JOB_PERCENTILES = List(0.01, 0.25, 0.5, 0.75, 0.99, 1.0)

  def getWeight(u: TaskUsage): Float =
    (u.getEndTime - u.getStartTime) / (300f * 1000f * 1000f)

  def getTaskUtilization(usage: Seq[TaskUsage]): Option[TaskUtilization] = {
    val result = TaskUtilization.newBuilder
    val firstUsage = usage.head

    val taskInfos =
      usage.filter(u => u.hasTaskInfo && u.getTaskInfo.hasRequestedResources).
            map(_.getTaskInfo).toBuffer
    if (taskInfos.size == 0)
      return None
    result.setInfo(taskInfos.head)

    result.setStartTime(usage.map(_.getStartTime).min).
           setEndTime(usage.map(_.getEndTime).max).
           setRunningTime(usage.map(u => u.getEndTime - u.getStartTime).sum)

    val minReqBuilder = result.getMinRequestBuilder
    val maxReqBuilder = result.getMaxRequestBuilder

    minReqBuilder.setCpus(taskInfos.map(_.getRequestedResources.getCpus).min)
    maxReqBuilder.setCpus(taskInfos.map(_.getRequestedResources.getCpus).max)
    minReqBuilder.setMemory(taskInfos.map(_.getRequestedResources.getMemory).min)
    maxReqBuilder.setMemory(taskInfos.map(_.getRequestedResources.getMemory).max)

    val maxCpus = preparePercentileList(
        usage.map(u => u.getMaxResources.getCpus -> getWeight(u))
      )
    val maxMemory = preparePercentileList(
        usage.map(u => u.getMaxResources.getMemory -> getWeight(u))
      )
    val cpus = preparePercentileList(
        usage.map(u => u.getResources.getCpus -> getWeight(u))
      )
    val memory = preparePercentileList(
        usage.map(u => u.getResources.getMemory -> getWeight(u))
      )

    for (percentile <- PER_TASK_PERCENTILES) {
      val meanUsage = Resources.newBuilder.
        setCpus(getPercentile(percentile, cpus)).
        setMemory(getPercentile(percentile, memory))

      val maxUsage = Resources.newBuilder.
        setCpus(getPercentile(percentile, maxCpus)).
        setMemory(getPercentile(percentile, maxMemory))

      result.addUsagePercentile(percentile.asInstanceOf[Float]).
             addPercentileTaskUsage(maxUsage).
             addPercentileMeanTaskUsage(meanUsage)
    }

    Some(result.build)
  }

  def getJobUtilization(tasks: Seq[TaskUtilization]): JobUtilization = {
    import scala.collection.JavaConversions._
    val result = JobUtilization.newBuilder
    val firstTask = tasks.head
    result.setJobInfo(firstTask.getInfo.getJob)
    val numTasks = tasks.groupBy(_.getInfo.getTaskIndex).size
    result.setNumTasks(numTasks)

    result.addTaskSamples(firstTask.getInfo)

    (0 to 8).map {
      case i =>
        result.addNumTasksFinal(
          tasks.filter(_.getInfo.getFinalEvent.getNumber == i).size)
        result.addNumEventsByType(tasks.map(_.getInfo.getNumEventsByType(i)).sum)
    }

    val minReqBuilder = result.getMinRequestBuilder
    val maxReqBuilder = result.getMaxRequestBuilder
    minReqBuilder.setCpus(tasks.map(_.getMinRequest.getCpus).min).
                  setMemory(tasks.map(_.getMinRequest.getMemory).min)
    maxReqBuilder.setCpus(tasks.map(_.getMaxRequest.getCpus).max).
                  setMemory(tasks.map(_.getMaxRequest.getMemory).max)

    val percentiles = firstTask.getUsagePercentileList
    for ((p, i) <- percentiles.zipWithIndex) {
      val maxCpus = preparePercentileList(
          tasks.map(t => t.getPercentileTaskUsage(i).getCpus -> 1f)
        )
      val maxMemory = preparePercentileList(
          tasks.map(t => t.getPercentileTaskUsage(i).getMemory -> 1f)
        )
      val cpus = preparePercentileList(
          tasks.map(t => t.getPercentileMeanTaskUsage(i).getCpus -> 1f)
        )
      val memory = preparePercentileList(
          tasks.map(t => t.getPercentileMeanTaskUsage(i).getMemory -> 1f)
        )

      for (p2 <- JOB_PERCENTILES) {
        result.addTaskPercentile(p2.asInstanceOf[Float]).
               addUsagePercentile(p.asInstanceOf[Float]).
          addPercentileTaskUsage(Resources.newBuilder.
            setCpus(getPercentile(p2, maxCpus)).
            setMemory(getPercentile(p2, maxMemory))).
          addPercentileMeanTaskUsage(Resources.newBuilder.
            setCpus(getPercentile(p2, cpus)).
            setMemory(getPercentile(p2, memory)))
      }
    }

    result.setRunningTime(tasks.map(_.getRunningTime).sum).
           setStartTime(tasks.map(_.getStartTime).min).
           setEndTime(tasks.map(_.getEndTime).max)

    result.build
  }
  
  def findTaskUtilizations(tasks: RDD[TaskUsage]): RDD[TaskUtilization] = {
    return tasks.map(keyByTask).groupByKey.
        flatMap(kv => {
          val u = getTaskUtilization(kv._2)
          if (u.isEmpty)
            None
          else
            Some(u.get)
        })
  }


  def findJobUtilizationsFromTasks(tasks: RDD[TaskUtilization]): RDD[JobUtilization] = {
    return tasks.map(keyByJob).groupByKey.map(kv => getJobUtilization(kv._2))
  }

  def findJobUtilizations(tasks: RDD[TaskUsage]): RDD[JobUtilization] = {
    return tasks.map(keyByTask).groupByKey.
        flatMap(kv => {
          val u = getTaskUtilization(kv._2)
          if (u.isEmpty)
            None
          else
            Some(kv._1._1 -> u.get)
        }).groupByKey.map(kv => getJobUtilization(kv._2))
  }

  private val MINUTE = 60L * 1000L * 1000L
  private val DAY = 24 * 60 * MINUTE

  private def normalizeTime(t: Long): Long = t / (5 * MINUTE)
  private def normalizeTimeDaily(t: Long): Long = (t % DAY) / (5 * MINUTE)

  def keyByJob(j: JobUtilization): (Long, JobUtilization) =
    j.getJobInfo.getId -> j
  
  def keyByJob(t: TaskUsageWithAvg): (Long, TaskUsageWithAvg) =
    (t.getUsage.getTaskInfo.getJob.getId, t)

  def keyByJob(t: TaskUtilization): (Long, TaskUtilization) =
    (t.getInfo.getJob.getId, t)
  
  def keyByTask(t: TaskUtilization): ((Long, Int), TaskUtilization) =
    (t.getInfo.getJob.getId -> t.getInfo.getTaskIndex, t)

  def keyByTask(t: TaskUsage): ((Long, Int), TaskUsage) =
    (t.getTaskInfo.getJob.getId -> t.getTaskInfo.getTaskIndex, t)
  
  def keyByTask(t: TaskUsageWithAvg): ((Long, Int), TaskUsageWithAvg) =
    (t.getUsage.getTaskInfo.getJob.getId -> t.getUsage.getTaskInfo.getTaskIndex,
     t)

  def combineUsage(usage: RDD[TaskUsage], 
                   maybeTasks: Option[RDD[TaskUtilization]],
                   maybeJobs: Option[RDD[JobUtilization]]): RDD[TaskUsageWithAvg] = {
    val usageEncap = usage.map(u => TaskUsageWithAvg.newBuilder.setUsage(u).build)
    val usageWithTasks = maybeTasks match {
    case Some(tasks) => {
      usageEncap.map(keyByTask).join(tasks.map(keyByTask)).map {
        case (ignoredKey, (usage, task)) => {
          usage.toBuilder.setTask(task).build
        }
      }
    }
    case None => usageEncap
    }
    val usageWithJobs = maybeJobs match {
    case Some(jobs) => {
      usageWithTasks.map(keyByJob).join(jobs.map(keyByJob)).map {
        case (ignoredKey, (usage, job)) => {
          usage.toBuilder.setJob(job).build
        }
      }
    }
    case None => usageWithTasks
    }

    usageWithJobs
  }

  def combineUsageWithDeaths(usageWithAvg: RDD[TaskUsageWithAvg],
                             events: RDD[TaskEvent]): RDD[TaskUsageWithAvg] = {
    val deaths = events.filter(e => 
      e.getType != TaskEventType.SUBMIT &&
      e.getType != TaskEventType.SCHEDULE &&
      e.getType != TaskEventType.UPDATE_PENDING &&
      e.getType != TaskEventType.UPDATE_RUNNING)
    val deathsByKey = deaths.map(Join.keyByTask)
    val usageByKey = usageWithAvg.map(keyByTask)
    def applyDeaths(pair: (Seq[TaskUsageWithAvg], Seq[TaskEvent])): Seq[TaskUsageWithAvg]= {
      val events = pair._2.sortBy(_.getTime)
      val usages = pair._1.sortBy(_.getUsage.getStartTime)

      val eventIter = events.iterator.buffered

      var nextDeath = if (eventIter.hasNext) Some(eventIter.next) else None
      val results = scala.collection.mutable.Buffer.empty[TaskUsageWithAvg]
      for (usage <- usages) {
        while (nextDeath.isDefined && usage.getUsage.getStartTime > nextDeath.get.getTime) {
          nextDeath = if (eventIter.hasNext) Some(eventIter.next) else None
        }
        nextDeath match {
        case Some(death) => results += usage.toBuilder.setNextDeath(death).build
        case None => results += usage
        }
      }
      results
    }
    usageByKey.groupWith(deathsByKey).flatMap(kv => applyDeaths(kv._2))
  }


  def keyUsageByMT(usage: TaskUsageWithAvg): ((Long, Long), TaskUsageWithAvg) = 
    (usage.getUsage.getMachineInfo.getId,
     normalizeTime(usage.getUsage.getStartTime)) -> usage

  def keyUsageByMTDaily(usage: TaskUsageWithAvg): ((Long, Long), TaskUsageWithAvg) = 
    (usage.getUsage.getMachineInfo.getId,
     normalizeTimeDaily(usage.getUsage.getStartTime)) -> usage

  def accumulateUsage(usage: Seq[TaskUsage]): Resources = {
    def usageKey(u: TaskUsage): (Long, Int) =
      (u.getTaskInfo.getJob.getId, u.getTaskInfo.getTaskIndex)
    /* TODO: fix the ordering here */
    val usageByTask = scala.collection.immutable.Map[(Long, Int), TaskUsage](
      usage.map(u => usageKey(u) -> u): _*
    )
    def weight(u: TaskUsage): Double =
      (u.getEndTime - u.getStartTime) / (300.0 * 1000.0 * 1000.0)
    var cpu = usage.map(u => u.getResources.getCpus * weight(u)).sum
    var mem = usageByTask.values.map(u => u.getResources.getMemory).sum
    Resources.newBuilder.setCpus(cpu.asInstanceOf[Float]).setMemory(mem.asInstanceOf[Float]).build
  }

  def toUsageByMachine(usage: Seq[TaskUsageWithAvg]): UsageByMachine = {
    import scala.collection.JavaConversions._
    val startTime = usage.map(_.getUsage.getStartTime).min
    val endTime = usage.map(_.getUsage.getEndTime).max
    val info = usage.head.getUsage.getMachineInfo
    val totalUsage = accumulateUsage(usage.map(_.getUsage))
    val result = UsageByMachine.newBuilder
    result.setResources(accumulateUsage(usage.map(_.getUsage))).
           addAllComponentsWithAvg(usage).
           setStartTime(startTime).
           setInfo(info).
           setEndTime(endTime).build
  }

  def computeUsageByMachine(usage: RDD[TaskUsageWithAvg]): RDD[UsageByMachine] = {
    usage.map(keyUsageByMT).groupByKey.mapValues(toUsageByMachine).
          map(kv => kv._2)
  }

  def computeUsageByMachineDaily(usage: RDD[TaskUsageWithAvg]): RDD[UsageByMachine] = {
    usage.map(keyUsageByMTDaily).groupByKey.mapValues(toUsageByMachine).
          map(kv => kv._2)
  }
  
  def computeUsageByMachineU(usage: RDD[TaskUsage]): RDD[UsageByMachine] = {
    computeUsageByMachine(combineUsage(usage, None, None))
  }

  def findComponents(u: UsageByMachine): Seq[TaskUsage] = {
    import scala.collection.JavaConversions._
    if (u.getComponentsCount > 0)
      u.getComponentsList
    else
      u.getComponentsWithAvgList.map(_.getUsage)
  }

  def getUsages(u: UsageByMachine, f: Resources => Float): (Float, Float, Float, Float, Float) = {
    import scala.collection.JavaConversions._
    val used = f(u.getResources)
    val capacity = f(u.getInfo.getCapacity)
    val uniqueComponents = findComponents(u).map(x => 
      (x.getTaskInfo.getJob.getId, x.getTaskInfo.getTaskIndex) -> x).toMap.values
    val reserved =
      uniqueComponents.map(x => f(x.getTaskInfo.getRequestedResources)).sum
    val reservedHigh =
      uniqueComponents.filter(_.getTaskInfo.getPriority > 8).map(
        x => f(x.getTaskInfo.getRequestedResources)).sum
    val usedHigh =
      uniqueComponents.filter(_.getTaskInfo.getPriority > 8).map(
        x => f(x.getResources)).sum
    (used, capacity, reserved, reservedHigh, usedHigh)
  }

  def getUsagesString(x: (Float, Float, Float, Float, Float)) =
    "used %s capacity %s reserved %s reservedHigh %s usedHigh %s".format(
      x._1, x._2, x._3, x._4, x._5)

  def makeUtilizations(inEvents: RDD[TaskEvent], inUsage: RDD[TaskUsage]):
      (RDD[JobUtilization], RDD[TaskUtilization],
       RDD[TaskUsageWithAvg], RDD[UsageByMachine],
       RDD[UsageByMachine]) = {
    val taskUtils = findTaskUtilizations(inUsage).cache
    val jobUtils = findJobUtilizationsFromTasks(taskUtils).cache
    val usageWithAvg = combineUsageWithDeaths(
        combineUsage(inUsage, Some(taskUtils), Some(jobUtils)),
        inEvents)
    val usageByMachine = computeUsageByMachine(usageWithAvg)
    val usageByMachineDaily = computeUsageByMachineDaily(usageWithAvg)
    (jobUtils, taskUtils, usageWithAvg, usageByMachine, usageByMachineDaily)
  }
}
