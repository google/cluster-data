package amplab.googletrace

import Convert._
import Protos._
import TraceUtil._

import amplab.googletrace.mapreduce.input._
import amplab.googletrace.mapreduce.output._
import com.twitter.elephantbird.mapreduce.io.ProtobufWritable
import org.apache.hadoop.io.LongWritable

import spark.SparkContext
import spark.RDD
import SparkContext._

object Stored {
  var inDir = System.getProperty("trace.in.directory")
  var outDir = System.getProperty("trace.processed.directory")

  def readSavedTasks(sc: SparkContext, inFile: String): RDD[TaskEvent] = 
    inLzo[LongWritable, ProtobufWritable[TaskEvent],
          LzoTaskEventProtobufBlockInputFormat](sc, inFile).
    map(kv => kv._2.get)

  def readSavedUsage(sc: SparkContext, inFile: String): RDD[TaskUsage] = 
    inLzo[LongWritable, ProtobufWritable[TaskUsage],
          LzoTaskUsageProtobufBlockInputFormat](sc, inFile).
    map(kv => kv._2.get)

  def readSavedMachines(sc: SparkContext, inFile: String): RDD[MachineEvent] = 
    inLzo[LongWritable, ProtobufWritable[MachineEvent],
          LzoMachineEventProtobufBlockInputFormat](sc, inFile).
    map(kv => kv._2.get)

  def readSavedJobs(sc: SparkContext, inFile: String): RDD[JobEvent] = 
    inLzo[LongWritable, ProtobufWritable[JobEvent],
          LzoJobEventProtobufBlockInputFormat](sc, inFile).
    map(kv => kv._2.get)

  def readSavedJobUtilizations(sc: SparkContext, inFile: String): RDD[JobUtilization] = 
    inLzo[LongWritable, ProtobufWritable[JobUtilization],
          LzoJobUtilizationProtobufBlockInputFormat](sc, inFile).
    map(kv => kv._2.get)
  
  def readSavedTaskUtilizations(sc: SparkContext, inFile: String): RDD[TaskUtilization] = 
    inLzo[LongWritable, ProtobufWritable[TaskUtilization],
          LzoTaskUtilizationProtobufBlockInputFormat](sc, inFile).
    map(kv => kv._2.get)

  def putJobs(sc: SparkContext, data: RDD[JobEvent], outFile: String): Unit = {
    out[LzoJobEventProtobufBlockOutputFormat, JobEvent](sc, data, outFile)
  }
  
  def putTasks(sc: SparkContext, data: RDD[TaskEvent], outFile: String): Unit = {
    out[LzoTaskEventProtobufBlockOutputFormat, TaskEvent](sc, data, outFile)
  }
  
  def putUsage(sc: SparkContext, data: RDD[TaskUsage], outFile: String): Unit = {
    out[LzoTaskUsageProtobufBlockOutputFormat, TaskUsage](sc, data, outFile)
  }
  
  def putMachines(sc: SparkContext, data: RDD[MachineEvent], outFile: String): Unit = {
    out[LzoMachineEventProtobufBlockOutputFormat, MachineEvent](sc, data, outFile)
  }

  def putJobUtilizations(sc: SparkContext, data: RDD[JobUtilization], outFile: String): Unit = {
    out[LzoJobUtilizationProtobufBlockOutputFormat, JobUtilization](sc, data, outFile)
  }

  def putTaskUtilizations(sc: SparkContext, data: RDD[TaskUtilization], outFile: String): Unit = {
    out[LzoTaskUtilizationProtobufBlockOutputFormat, TaskUtilization](sc, data, outFile)
  }

  def putUsageByMachine(sc: SparkContext, data: RDD[UsageByMachine], outFile: String): Unit = {
    out[LzoUsageByMachineProtobufBlockOutputFormat, UsageByMachine](sc, data,
      outFile)
  }

  def putTaskUsageWithAvg(sc: SparkContext, data: RDD[TaskUsageWithAvg], outFile: String): Unit = {
    out[LzoTaskUsageWithAvgProtobufBlockOutputFormat, TaskUsageWithAvg](sc, data,
      outFile)
  }
}
