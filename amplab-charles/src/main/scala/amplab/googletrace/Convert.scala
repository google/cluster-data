package amplab.googletrace

import scala.collection.mutable.Buffer

import spark.SparkContext
import spark.RDD
import SparkContext._

import Util._
import Protos._

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

object Convert {
  def convertTaskUsage(v: Array[String]): TaskUsage = {
    val builder = TaskUsage.newBuilder
    val taskInfoBuilder = builder.getTaskInfoBuilder
    val jobInfoBuilder = taskInfoBuilder.getJobBuilder
    val machineInfoBuilder = builder.getMachineInfoBuilder
    val resourcesBuilder = builder.getResourcesBuilder
    val maxResourcesBuilder = builder.getMaxResourcesBuilder
    builder.setStartTime(convertTime(v(0)))
    builder.setEndTime(convertTime(v(1)))
    jobInfoBuilder.setId(convertId(v(2)))
    taskInfoBuilder.setTaskIndex(convertInt(v(3)))
    machineInfoBuilder.setId(convertId(v(4)))
    if (!(v(5) isEmpty)) resourcesBuilder.setCpus(convertFloat(v(5)))
    if (!(v(6) isEmpty)) resourcesBuilder.setMemory(convertFloat(v(6)))
    if (!(v(7) isEmpty)) resourcesBuilder.setAssignedMemory(convertFloat(v(7)))
    if (!(v(8) isEmpty))
      resourcesBuilder.setUnmappedPageCacheMemory(convertFloat(v(8)))
    if (!(v(9) isEmpty)) resourcesBuilder.setPageCacheMemory(convertFloat(v(9)))
    if (!(v(10) isEmpty)) maxResourcesBuilder.setMemory(convertFloat(v(10)))
    if (!(v(11) isEmpty)) resourcesBuilder.setDiskTime(convertFloat(v(11)))
    if (!(v(12) isEmpty)) resourcesBuilder.setDiskSpace(convertFloat(v(12)))
    if (!(v(13) isEmpty)) maxResourcesBuilder.setCpus(convertFloat(v(13)))
    if (!(v(14) isEmpty)) maxResourcesBuilder.setDiskTime(convertFloat(v(14)))
    if (!(v(15) isEmpty)) builder.setCyclesPerInstruction(convertFloat(v(15)))
    if (!(v(16) isEmpty))
      builder.setMemoryAccessesPerInstruction(convertFloat(v(16)))
    if (!(v(17) isEmpty)) builder.setSamplePortion(convertFloat(v(17)))
    if (!(v(18) isEmpty)) builder.setAggregationType(convertBool(v(18)))
    return builder.build
  }

  def convertTaskEvent(v: Array[String]): TaskEvent = {
    val builder = TaskEvent.newBuilder
    val taskInfoBuilder = builder.getInfoBuilder
    val resourcesBuilder = taskInfoBuilder.getRequestedResourcesBuilder
    val jobInfoBuilder = taskInfoBuilder.getJobBuilder
    val machineInfoBuilder = builder.getMachineInfoBuilder
    builder.setTime(convertTime(v(0)))
    if (!(v(1) isEmpty)) builder.setMissingType(convertMissingType(v(1)))
    jobInfoBuilder.setId(convertId(v(2)))
    taskInfoBuilder.setTaskIndex(convertInt(v(3)))
    if (!(v(4) isEmpty)) {
      machineInfoBuilder.setId(convertId(v(4)))
    } else {
      builder.clearMachineInfo
    }
    builder.setType(TaskEventType.valueOf(convertInt(v(5))))
    if (!(v(6) isEmpty)) jobInfoBuilder.setUser(v(6))
    if (!(v(7) isEmpty)) taskInfoBuilder.setSchedulingClass(convertInt(v(7)))
    if (!(v(8) isEmpty)) taskInfoBuilder.setPriority(convertInt(v(8)))
    if (!(v(9) isEmpty)) resourcesBuilder.setCpus(convertFloat(v(9)))
    if (!(v(10) isEmpty)) resourcesBuilder.setMemory(convertFloat(v(10)))
    if (!(v(11) isEmpty)) resourcesBuilder.setDiskSpace(convertFloat(v(11)))
    if (!(v(12) isEmpty)) taskInfoBuilder.setDifferentMachines(convertBool(v(12)))
    return builder.build
  }

  def convertJobEvent(v: Array[String]): JobEvent = {
    val builder = JobEvent.newBuilder
    val jobInfoBuilder = builder.getInfoBuilder

    builder.setTime(convertTime(v(0)))
    if (!(v(1) isEmpty)) builder.setMissingType(convertMissingType(v(1)))
    jobInfoBuilder.setId(convertId(v(2)))
    builder.setType(TaskEventType.valueOf(convertInt(v(3))))
    if (!(v(4) isEmpty)) jobInfoBuilder.setUser(v(4))
    if (!(v(5) isEmpty)) jobInfoBuilder.setSchedulingClass(convertInt(v(5)))
    if (!(v(6) isEmpty)) jobInfoBuilder.setName(v(6))
    if (!(v(7) isEmpty)) jobInfoBuilder.setLogicalName(v(7))
    return builder.build
  }

  def convertMachineEvent(v: Array[String]): MachineEvent = {
    val builder = MachineEvent.newBuilder
    val machineInfoBuilder = builder.getInfoBuilder
    val capacityBuilder = machineInfoBuilder.getCapacityBuilder

    builder.setTime(convertTime(v(0)))
    machineInfoBuilder.setId(convertId(v(1)))
    val raw_type = convertInt(v(2))
    if (raw_type == 0 /* ADD */ || raw_type == 2 /* UPDATE */) {
      builder.setUp(true)
    } else {
      builder.setUp(false)
    }
    if (!(v(3) isEmpty)) machineInfoBuilder.setPlatformId(v(3))
    if (!(v(4) isEmpty)) capacityBuilder.setCpus(convertFloat(v(4)))
    if (!(v(5) isEmpty)) capacityBuilder.setMemory(convertFloat(v(5)))
    return builder.build
  }

  val COMMA = java.util.regex.Pattern.compile(",")

  // FIXME: hack to avoid any shard being too big to fit in memory on
  // some rather constrained nodes.
  def reshardStrings(data: RDD[String]): RDD[String] =
    data.map(x => (x, Nil)).groupByKey(600).map(_._1)

  def in[T <: Message](sc: SparkContext, convert: Array[String] => T,
                       inDir: String, isBig: Boolean = true)(
                       implicit fm: ClassManifest[T]): RDD[T] = {
    val _lines = sc.textFile(inDir + "/*?????-of-?????.csv*")
    val lines = if (isBig) reshardStrings(_lines) else _lines
    val records = lines.map(COMMA.split(_, -1))
    return records.map(convert)
  }

  def inLzo[K, V, F <: InputFormat[K, V]](sc: SparkContext, path: String)(
      implicit fm: ClassManifest[F], km: ClassManifest[K],
      vm: ClassManifest[V]): RDD[(K, V)] = {
    val conf = new Configuration
    conf.set("io.compression.codecs",
      "org.apache.hadoop.io.compress.DefaultCodec," +
      "org.apache.hadoop.io.compress.GzipCodec," +
      "org.apache.hadoop.io.compress.BZip2Codec," +
      "com.hadoop.compression.lzo.LzoCodec," +
      "com.hadoop.compression.lzo.LzopCodec")
    conf.set("io.compression.codec.lzo.class", 
      "com.hadoop.compression.lzo.LzoCodec")
    val job = new HadoopJob(conf)
    FileInputFormat.addInputPath(job, new Path(path))
    sc.newAPIHadoopFile(path,
      fm.erasure.asInstanceOf[Class[F]],
      km.erasure.asInstanceOf[Class[K]],
      vm.erasure.asInstanceOf[Class[V]],
      job.getConfiguration)
  }

  def inConverted[F <: InputFormat[LongWritable, ProtobufWritable[T]],
                  T <: Message](sc: SparkContext, path: String)(
                  implicit fm: ClassManifest[F], tm: ClassManifest[T]): RDD[T] =
    inLzo[LongWritable, ProtobufWritable[T], F](sc, path).map(_._2.get)
  
  def out[F <: OutputFormat[T, ProtobufWritable[T]], T <: Message](
      sc: SparkContext,
      data: RDD[T],
      out: String)(implicit fm: ClassManifest[F], tm: ClassManifest[T]): Unit = {
    write[F, T](out, data)
  } 

}
