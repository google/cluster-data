import spark.RDD
import spark.SparkContext
import spark.SparkEnv

import amplab.googletrace.mapreduce.input._
import amplab.googletrace.mapreduce.output._
import com.twitter.elephantbird.mapreduce.io.ProtobufWritable
import org.apache.hadoop.io.LongWritable

import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.{InputFormat, Job}
import org.apache.hadoop.mapreduce.lib.input.{FileInputFormat}

import org.apache.hadoop.conf.Configuration

import org.apache.hadoop.io.compress.CompressionCodecFactory

import amplab.googletrace.Convert._
import amplab.googletrace.Join._
import amplab.googletrace.Protos._
import amplab.googletrace.Util._
import amplab.googletrace.TraceUtil._
import amplab.googletrace.Stored._
import amplab.googletrace.TaskAggregates._
import amplab.googletrace.ToNumpy

/* XXX note that this depends on a file of JobUtilizations having been
 * generated */
val markedJobUtils = readSavedJobUtilizations(sc, outDir +
    "/all_job_utils-w-task-samples").cache

val SECOND = 1000L * 1000L
val MINUTE = 60L * SECOND
val HOUR = 60L * MINUTE
val DAY = 24L * HOUR

class JobUtilizationUtils(u: JobUtilization) {
  def getDuration: Long = u.getEndTime - u.getStartTime
}
implicit def jobUtilToUtils(u: JobUtilization): JobUtilizationUtils =
    new JobUtilizationUtils(u)

val subMinuteJobs = markedJobUtils.filter(_.getDuration <= MINUTE)
val minuteJobs = markedJobUtils.filter(_.getDuration > MINUTE)
val subHourJobs = markedJobUtils.filter(_.getDuration <= HOUR)
val hourJobs = markedJobUtils.filter(_.getDuration > HOUR)
val subDayJobs = markedJobUtils.filter(_.getDuration <= DAY)
val dayJobs = markedJobUtils.filter(_.getDuration > DAY)

val multiTaskJobs = markedJobUtils.filter(_.getNumTasks > 1)

def writeNumpyJobUtils: Unit = {
  ToNumpy.jobUtilizationConverter.writeNumpy(
    outDir + "/subminute_job_utils",
    subMinuteJobs
  )
  ToNumpy.jobUtilizationConverter.writeNumpy(
    outDir + "/minute_job_utils",
    minuteJobs
  )
  ToNumpy.jobUtilizationConverter.writeNumpy(
    outDir + "/hour_job_utils",
    hourJobs
  )
  ToNumpy.jobUtilizationConverter.writeNumpy(
    outDir + "/day_job_utils",
    dayJobs
  )
  ToNumpy.jobUtilizationConverter.writeNumpy(
    outDir + "/subhour_job_utils",
    subHourJobs
  )
  ToNumpy.jobUtilizationConverter.writeNumpy(
    outDir + "/subday_job_utils",
    subDayJobs
  )
  ToNumpy.jobUtilizationConverter.writeNumpy(
    outDir + "/multitask_job_utils",
    multiTaskJobs
  )
}
