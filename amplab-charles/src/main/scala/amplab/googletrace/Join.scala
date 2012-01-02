package amplab.googletrace

import Protos._
import TraceUtil._

import spark.SparkContext
import spark.RDD
import SparkContext._

import scala.collection.mutable.Buffer

object Join {    
  def keyByTask(taskEvent: TaskEvent): ((Long, Int), TaskEvent) =
    (taskEvent.getInfo.getJob.getId, taskEvent.getInfo.getTaskIndex) -> taskEvent
  
  def keyByTask(usageEvent: TaskUsage): ((Long, Int), TaskUsage) =
    (usageEvent.getTaskInfo.getJob.getId, usageEvent.getTaskInfo.getTaskIndex) ->
      usageEvent
  
  def keyByJob(taskEvent: TaskEvent): (Long, TaskEvent) =
    taskEvent.getInfo.getJob.getId -> taskEvent

  trait TimeOf[T] extends Function1[T, Long] with Serializable {
    def apply(value: T): Long
    def lessThan(first: T, second: T): Boolean = apply(first) < apply(second)
  }

  implicit def taskEventTime: TimeOf[TaskEvent] = new TimeOf[TaskEvent] {
    def apply(value: TaskEvent): Long = 
      if (value.getType == TaskEventType.SCHEDULE)
        value.getTime + 1
      else
        value.getTime
  }
  implicit def jobEventTime: TimeOf[JobEvent] = new TimeOf[JobEvent] {
    def apply(value: JobEvent): Long =
      if (value.getType == TaskEventType.SCHEDULE)
        value.getTime + 1
      else
        value.getTime
  }
  implicit def machineEventTime: TimeOf[MachineEvent] = new TimeOf[MachineEvent]
  {
    def apply(value: MachineEvent): Long = value.getTime
  }

  implicit def taskUsageTime: TimeOf[TaskUsage] = new TimeOf[TaskUsage]
  {
    def apply(value: TaskUsage): Long = value.getStartTime + 1000000L
  }

  trait Insert[T, U, K] extends Serializable {
    def throughT(t: T): Boolean = false
    def hasThroughT: Boolean = false
    def apply(into: T, value: U): T
    def keyT(t: T): K
    def keyU(u: U): K
  }

  implicit def insertJobInTask: Insert[TaskEvent, JobEvent, Long] =
    new Insert[TaskEvent, JobEvent, Long] {
      def apply(into: TaskEvent, value: JobEvent): TaskEvent = {
        val builder = into.toBuilder
        builder.getInfoBuilder.getJobBuilder.mergeFrom(value.getInfo)
        return builder.build
      }
      def keyT(t: TaskEvent) = t.getInfo.getJob.getId
      def keyU(u: JobEvent) = u.getInfo.getId
    }

  implicit def insertMachineInTask: Insert[TaskEvent, MachineEvent, Long] =
    new Insert[TaskEvent, MachineEvent, Long] {
      override def hasThroughT: Boolean = true
      override def throughT(t: TaskEvent): Boolean = 
        !t.hasMachineInfo || !t.getMachineInfo.hasId ||
        t.getMachineInfo.getId == 0
      def apply(into: TaskEvent, value: MachineEvent): TaskEvent = {
        val builder = into.toBuilder
        builder.getMachineInfoBuilder.mergeFrom(value.getInfo)
        return builder.build
      }
      def keyT(t: TaskEvent) = 
        if (t.hasMachineInfo && t.getMachineInfo.hasId) 
          t.getMachineInfo.getId
        else
          -1L
      def keyU(u: MachineEvent) = u.getInfo.getId
    }

  implicit def insertTaskInUsage: Insert[TaskUsage, TaskEvent, (Long, Int)] =
    new Insert[TaskUsage, TaskEvent, (Long, Int)] {
      def apply(into: TaskUsage, value: TaskEvent): TaskUsage = {
        val builder = into.toBuilder
        builder.getTaskInfoBuilder.mergeFrom(value.getInfo)
        return builder.build
      }
      def keyT(t: TaskUsage) = (t.getTaskInfo.getJob.getId,
                                t.getTaskInfo.getTaskIndex)
      def keyU(t: TaskEvent) = (t.getInfo.getJob.getId,
                                t.getInfo.getTaskIndex)
    }

  implicit def insertMachineInUsage: Insert[TaskUsage, MachineEvent, Long] =
    new Insert[TaskUsage, MachineEvent, Long] {
      def apply(into: TaskUsage, value: MachineEvent): TaskUsage = {
        val builder = into.toBuilder
        builder.getMachineInfoBuilder.mergeFrom(value.getInfo)
        return builder.build
      }
      def keyT(t: TaskUsage) = t.getMachineInfo.getId
      def keyU(u: MachineEvent) = u.getInfo.getId
    }

  implicit def insertJobInJobUtilization: Insert[JobUtilization, JobEvent, Long] =
    new Insert[JobUtilization, JobEvent, Long] {
      def apply(into: JobUtilization, value: JobEvent): JobUtilization = {
        val builder = into.toBuilder
        builder.getJobInfoBuilder.mergeFrom(value.getInfo)
        return builder.build
      }
      def keyT(t: JobUtilization) = t.getJobInfo.getId
      def keyU(u: JobEvent) = u.getInfo.getId
    }

  implicit def timeOfJobForJoin: TimeOf[JobUtilization] =
    new TimeOf[JobUtilization] {
      def apply(value: JobUtilization): Long = value.getEndTime
    }

  val TIME_PERIOD = 1000L * 1000L * 60L * 20L
  val MAX_TIME = 1000L * 1000L * 60L * 60L * 24L * 30L

  /* time division version of placeJoined to avoid shards of death at the
     cost of an extra shuffle and a bunch of extra copies of records that
     cross the time divisions */
  def placeJoinedBig[RealKey, T, U](_into: RDD[T], values: RDD[U])
      (implicit timeOfT: TimeOf[T], timeOfU: TimeOf[U],
       insertU: Insert[T,U,RealKey],
       km: ClassManifest[RealKey], tm: ClassManifest[T], um: ClassManifest[U])
      : RDD[T] = {
    import Util.keyByTime
    import Util.partitionByTime
    val throughInto: RDD[T] =
      if (insertU.hasThroughT)
        _into.filter(t => insertU.throughT(t))
      else
        _into.context.makeRDD(Array[T]())
    val into: RDD[T] =
      if (insertU.hasThroughT)
        _into.filter(t => !insertU.throughT(t))
      else
        _into
    type K = (Int, RealKey)
    val keyedInto: RDD[(K, T)] =
      keyByTime(into, insertU.keyT _, timeOfT, TIME_PERIOD, MAX_TIME)
    val keyedValues: RDD[(K, U)] = 
      partitionByTime(values, insertU.keyU _, timeOfU, timeOfU.lessThan _,
                      TIME_PERIOD, MAX_TIME)
    val grouped: RDD[(K, (Seq[T], Seq[U]))] = keyedInto.groupWith(keyedValues)
    def processGroup(kv: (K, (Seq[T], Seq[U]))): Seq[T] = {
      val v = kv._2
      val sortedT: Seq[T] = v._1.sortBy(x => timeOfT(x))
      val sortedU: Seq[U] = v._2.sortBy(x => timeOfU(x))
      val result = Buffer.empty[T]
      val uIterator = sortedU.iterator.buffered
      var currentU: Option[U] = None
      for (t <- sortedT) {
        val currentTime = timeOfT(t)
        while (uIterator.hasNext && currentTime >= timeOfU(uIterator.head)) {
          currentU = Some(uIterator.next)
        }
        currentU match {
        case Some(u) => result += insertU(t, u)
        case None => result += t
        }
      }
      return result
    }
    val afterProcess = grouped.flatMap(processGroup)
    if (insertU.hasThroughT)
      afterProcess ++ throughInto
    else
      afterProcess
  }

  def broadcastPlaceJoined[@specialized RealKey, T, U](_into: RDD[T], values: RDD[U])
      (implicit timeOfT: TimeOf[T], timeOfU: TimeOf[U],
       insertU: Insert[T,U,RealKey],
       km: ClassManifest[RealKey], tm: ClassManifest[T], um: ClassManifest[U])
      : RDD[T] = {
    type K = RealKey
    val keyedValues: RDD[(K, U)] = values.map(u => insertU.keyU(u) -> u)
    val keyedValuesGrouped = keyedValues.groupByKey(1).
      mapValues(_.sortBy(x => timeOfU(x)))
    val keyedValuesMap = keyedValuesGrouped.collect().toMap
    val lookupTable = keyedValues.context.broadcast(keyedValuesMap)

    val throughInto: RDD[T] =
      if (insertU.hasThroughT)
        _into.filter(t => insertU.throughT(t))
      else
        _into.context.makeRDD(Array[T]())
    val into: RDD[T] =
      if (insertU.hasThroughT)
        _into.filter(t => !insertU.throughT(t))
      else
        _into
    def matchT(t: T): T = {
      val k = insertU.keyT(t)
      val sortedU: Seq[U] = lookupTable.value.getOrElse(k, Seq.empty[U])
      val uIterator = sortedU.iterator.buffered
      var currentU: Option[U] = None
      val currentTime = timeOfT(t)
      while (uIterator.hasNext && currentTime >= timeOfU(uIterator.head)) {
        currentU = Some(uIterator.next)
      }
      currentU match {
      case Some(u) => insertU(t, u)
      case None => t
      }
    }
    val afterProcess = into.map(matchT)
    if (insertU.hasThroughT)
      afterProcess ++ throughInto
    else
      afterProcess
  }

  def placeJoined[@specialized RealKey, T, U](_into: RDD[T], values: RDD[U])
      (implicit timeOfT: TimeOf[T], timeOfU: TimeOf[U],
       insertU: Insert[T,U,RealKey],
       km: ClassManifest[RealKey], tm: ClassManifest[T], um: ClassManifest[U])
      : RDD[T] = {
    type K = RealKey
    val throughInto: RDD[T] =
      if (insertU.hasThroughT)
        _into.filter(t => insertU.throughT(t))
      else
        _into.context.makeRDD(Array[T]())
    val into: RDD[T] =
      if (insertU.hasThroughT)
        _into.filter(t => !insertU.throughT(t))
      else
        _into
    val keyedInto: RDD[(K, T)] = into.map(t => insertU.keyT(t) -> t)
    val keyedValues: RDD[(K, U)] = values.map(u => insertU.keyU(u) -> u)
    val grouped: RDD[(K, (Seq[T], Seq[U]))] =
        keyedInto.groupWith(keyedValues)
    def processGroup(kv: (K, (Seq[T], Seq[U]))): Seq[T] = {
      val v = kv._2
      val sortedT: Seq[T] = v._1.sortBy(x => timeOfT(x))
      val sortedU: Seq[U] = v._2.sortBy(x => timeOfU(x))
      val result = Buffer.empty[T]
      val uIterator = sortedU.iterator.buffered
      var currentU: Option[U] = None
      for (t <- sortedT) {
        val currentTime = timeOfT(t)
        while (uIterator.hasNext && currentTime >= timeOfU(uIterator.head)) {
          currentU = Some(uIterator.next)
        }
        currentU match {
        case Some(u) => result += insertU(t, u)
        case None => result += t
        }
      }
      return result
    }
    val afterProcess = grouped.flatMap(processGroup)
    if (insertU.hasThroughT)
      afterProcess ++ throughInto
    else
      afterProcess
  }
}
