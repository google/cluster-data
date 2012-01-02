import amplab.googletrace.Convert._
import amplab.googletrace.Join._
import amplab.googletrace.Stored._
import amplab.googletrace.Utilizations._
import amplab.googletrace.Protos._
import amplab.googletrace.Util._
import amplab.googletrace.TaskAggregates._

import spark.RDD
import spark.SparkContext

import amplab.googletrace.mapreduce.input._
import amplab.googletrace.mapreduce.output._
import com.twitter.elephantbird.mapreduce.io.ProtobufWritable
import org.apache.hadoop.io.LongWritable

val rawJobs = reshard(20, in(sc, convertJobEvent, inDir + "/job_events", false)).cache
val rawTasks = in(sc, convertTaskEvent, inDir + "/task_events")
val rawUsage = in(sc, convertTaskUsage, inDir + "/task_usage")
val rawMachines = in(sc, convertMachineEvent, inDir + "/machine_events",
false).cache

putJobs(sc, rawJobs, outDir + "/jobs_unjoined")
putTasks(sc, rawTasks, outDir + "/tasks_unjoined")
putUsage(sc, rawUsage, outDir + "/usage_unjoined")
putMachines(sc, rawMachines, outDir + "/machines_unjoined")

val markedTasks = markTasks(rawTasks)
putTasks(sc, rawTasks, outDir + "/tasks_marked")

val tasksWithJobs = placeJoined(markedTasks, rawJobs)
putTasks(sc, tasksWithJobs, outDir + "/tasks_with_jobs")

val tasksWithMachines = broadcastPlaceJoined(tasksWithJobs, rawMachines)
putTasks(sc, tasksWithMachines, outDir + "/tasks_with_jobs_and_machines")

val usageWithMachines = broadcastPlaceJoined(rawUsage, rawMachines)
putUsage(sc, usageWithMachines, outDir + "/usage_with_machines")

val usageWithJobs = placeJoined(usageWithMachines, tasksWithMachines)
putUsage(sc, usageWithJobs, outDir + "/usage_with_jobs_and_machines")
