import spark.RDD
import spark.SparkContext

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
import amplab.googletrace.ToNumpy._

val markedJobUtils = readSavedJobUtilizations(sc, inFile)
