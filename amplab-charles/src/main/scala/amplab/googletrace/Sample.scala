package amplab.googletrace

import scala.collection.mutable.Buffer

import spark.SparkContext
import spark.RDD
import SparkContext._

import Convert._
import Util._
import Stored._

import Protos._

object Sample {
  def sampleMachinesFromUsage(rate: Int, data: RDD[TaskUsage]): RDD[TaskUsage] =
    data.filter(_.getMachineInfo.getId % rate == 0)

  def sampleMachinesFromTasks(rate: Int, data: RDD[TaskEvent]): RDD[TaskEvent] =
    data.filter(task =>
      task.hasMachineInfo && task.getMachineInfo.getId % rate == 0)

  def sampleMachinesFromMachines(rate: Int, data: RDD[MachineEvent]): RDD[MachineEvent] = 
    data.filter(m => m.getInfo.getId % rate == 0)

  def sampleJobsFromTasks(rate: Int, data: RDD[TaskEvent]): RDD[TaskEvent] =
    data.filter(_.getInfo.getJob.getId % rate == 0)

  def sampleJobsFromUsage(rate: Int, data: RDD[TaskUsage]): RDD[TaskUsage] =
    data.filter(_.getTaskInfo.getJob.getId % rate == 0)

  def sampleFile(rate: Int, name: String): String =
    outDir + "/sample" + rate + "_" + name

  def usageShardsFor(rate: Int) = 2000 / rate
  def tasksShardsFor(rate: Int) = 250 / rate

  def writeMachineSamples(sc: SparkContext, rate: Int,
                          tasks: RDD[TaskEvent],
                          usage: RDD[TaskUsage]):
      (RDD[TaskEvent], RDD[TaskUsage]) = {
    val usageSample = reshard(usageShardsFor(rate),
                              sampleMachinesFromUsage(rate, usage))
    val tasksSample = reshard(tasksShardsFor(rate),
                              sampleMachinesFromTasks(rate, tasks))

    putTasks(sc, tasksSample, sampleFile(rate, "tasks"))
    putUsage(sc, usageSample, sampleFile(rate, "usage"))

    (tasksSample, usageSample)
  }

  def readMachineSamples(sc: SparkContext, rate: Int): (RDD[TaskEvent], RDD[TaskUsage]) = {
    val tasksWithMachinesSample =
      readSavedTasks(sc, sampleFile(rate, "tasks"))
    val usageWithMachinesSample =
      readSavedUsage(sc, sampleFile(rate, "usage"))
    
    (tasksWithMachinesSample, usageWithMachinesSample)
  }

  def writeJobSample(sc: SparkContext, rate: Int, tasks: RDD[TaskEvent], usage: RDD[TaskUsage]):
      (RDD[TaskEvent], RDD[TaskUsage]) = {
    val usageSample = reshard(usageShardsFor(rate),
                              sampleJobsFromUsage(rate, usage))
    val tasksSample = reshard(tasksShardsFor(rate),
                              sampleJobsFromTasks(rate, tasks))
    
    putTasks(sc, tasksSample, sampleFile(rate, "job-tasks"))
    putUsage(sc, usageSample, sampleFile(rate, "job-usage"))

    (tasksSample, usageSample)
  }

  def readJobSample(sc: SparkContext, rate: Int): (RDD[TaskEvent], RDD[TaskUsage]) = {
    val tasksWithMachinesSample =
      readSavedTasks(sc, sampleFile(rate, "job-tasks"))
    val usageWithMachinesSample =
      readSavedUsage(sc, sampleFile(rate, "job-usage"))
    
    (tasksWithMachinesSample, usageWithMachinesSample)
  }
}
