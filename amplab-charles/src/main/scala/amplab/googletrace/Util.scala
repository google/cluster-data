package amplab.googletrace

import spark.SparkContext
import spark.RDD
import SparkContext._

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{LongWritable, NullWritable, Text}
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat
import org.apache.hadoop.mapreduce.OutputFormat
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat

import org.apache.hadoop.mapreduce.TaskAttemptContext
import org.apache.hadoop.mapreduce.TaskAttemptID

import com.twitter.elephantbird.mapreduce.io.ProtobufWritable
import com.twitter.elephantbird.util.TypeRef

import amplab.googletrace.mapreduce.output._
import Protos._

import spark.SerializableWritable
import java.text.SimpleDateFormat
import java.util.Date

import com.google.protobuf.Message

import scala.collection.immutable.LongMap

object Util {
  def min(x: Long, y: Long): Long = if (x < y) x else y

  def reshard[T](splits: Int, data: RDD[T])(implicit tm: ClassManifest[T]): RDD[T] =
    data.map(data => data.hashCode -> data).groupByKey(splits).flatMap(_._2)

  @inline
  def keyByTime[T, K](data: RDD[T], keyOf: T => K, timeOf: T => Long,
                      timePeriod: Long, maxTime:Long): RDD[((Int, K), T)] = {
    def timePeriodOf(t: T): Int = 
      min(timeOf(t) / timePeriod, maxTime / timePeriod).asInstanceOf[Int]
    return data.map(t => (timePeriodOf(t), keyOf(t)) -> t)
  }

  /** Divide data into partitions of length timePeriod on its time axis.
   *
   * Each partition will contain
   * (a) all records falling within the time period; and
   * (b) for each key k, the last record from a prior time period (if any)
   */
  @inline
  def partitionByTime[T, @specialized K](
      data: RDD[T], keyOf: T => K, timeOf: T => Long,
      lessThan: (T, T) => Boolean, timePeriod: Long, maxTime: Long)
      (implicit km: ClassManifest[K], tm: ClassManifest[T]):
        RDD[((Int, K), T)] = {
    val template = data.first
    val lastPeriod = (maxTime / timePeriod).asInstanceOf[Int]
    def timePeriodOfT(t: T): Int = 
      min(timeOf(t) / timePeriod, lastPeriod).asInstanceOf[Int]
    val originalByPeriod =
      data.map(t => (timePeriodOfT(t), keyOf(t)) -> t)
    def latest(t1: T, t2: T): T = 
      if (timeOf(t1) < timeOf(t2))
        t2
      else
        t1
    def earliest(t1: T, t2: T): T = 
      if (timeOf(t1) < timeOf(t2))
        t1
      else
        t2
    def broadcastLater(kv: ((Int, K), T)): Seq[((Int, K), T)] = 
      ((kv._1._1 + 1) to lastPeriod).map(period => (period, kv._1._2) -> kv._2)
    def fixTriplet(in: (K, (T, Int))): ((Int, K), T) =
      (in._2._2, in._1) -> in._2._1
    /* the reduceByKey() should just be an optimization. */
    val earliers = originalByPeriod.reduceByKey(latest).
      flatMap(broadcastLater).reduceByKey(latest)
    originalByPeriod ++ earliers
  }

  def writeHadoop[F <: OutputFormat[K, V], K, V](
      data: RDD[(K, V)], path: String)(implicit fm: ClassManifest[F]): Int = {
    val job = new Job
    val wrappedConf = new SerializableWritable(job.getConfiguration)
    FileOutputFormat.setOutputPath(job, new Path(path))
    /* XXX from HadoopFileWriter*/
    val formatter = new SimpleDateFormat("yyyyMMddHHmm")
    val jobtrackerID = formatter.format(new Date())
    val stageId = data.id
    def doWrite(context: spark.TaskContext, iter: Iterator[(K,V)]): Int = {
      /* "reduce task" <split #> <attempt # = spark task #> */
      val attemptId = new TaskAttemptID(jobtrackerID,
        stageId, false, context.splitId, context.attemptId)
      val hadoopContext = new TaskAttemptContext(wrappedConf.value, attemptId)
      val format = fm.erasure.newInstance.asInstanceOf[F]
      val committer = format.getOutputCommitter(hadoopContext)
      committer.setupTask(hadoopContext)
      val writer = format.getRecordWriter(hadoopContext)
      while (iter.hasNext) {
        val (k, v) = iter.next
        writer.write(k, v)
      }
      writer.close(hadoopContext)
      committer.commitTask(hadoopContext)
      return 1
    }
    val jobFormat = fm.erasure.newInstance.asInstanceOf[F]
    /* apparently we need a TaskAttemptID to construct an OutputCommitter;
     * however we're only going to use this local OutputCommitter for
     * setupJob/commitJob, so we just use a dummy "map" task.
     */
    val jobAttemptId = new TaskAttemptID(jobtrackerID, stageId, true, 0, 0)
    val jobTaskContext = new TaskAttemptContext(wrappedConf.value, jobAttemptId)
    val jobCommitter = jobFormat.getOutputCommitter(jobTaskContext)
    jobCommitter.setupJob(jobTaskContext)
    println("DONE SETUP")
    val count = data.context.runJob(data, doWrite _).sum
    println("ABOUT TO COMMIT")
    jobCommitter.commitJob(jobTaskContext)
    println("DONE COMMIT")
    return count
  }

  def convertTime(s: String): Long = {
    if (s == "18446744073709551615") {
      return Long.MaxValue
    } else {
      return java.lang.Long.parseLong(s)
    }
  }
  
  def convertId(s: String) = java.lang.Long.parseLong(s)
  def convertInt(s: String) = java.lang.Integer.parseInt(s)
  def convertFloat(s: String) = java.lang.Float.parseFloat(s)
  def convertBool(s: String) = s == "1"
  def convertMissingType(s: String) =
    MissingType.valueOf(java.lang.Integer.parseInt(s))

  def fixupForOutput[T <: Message](x: T): (T, ProtobufWritable[T]) = 
    return (null.asInstanceOf[T], new ProtobufWritable[T](x, new TypeRef[T] {}))

  def write[F <: OutputFormat[T, ProtobufWritable[T]], T <: Message](
      path: String, data: RDD[T])(implicit fm: ClassManifest[F]): Unit = {
    val fixedData = data.map(fixupForOutput)
    writeHadoop[F, T, ProtobufWritable[T]](fixedData, path)
  }
}

