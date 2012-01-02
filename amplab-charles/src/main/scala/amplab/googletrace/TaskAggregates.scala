package amplab.googletrace

import Protos._
import TraceUtil._

import spark.RDD
import spark.SparkContext._

object TaskAggregates {
  // Returns TaskInfo to merge into existing TaskInfos. This will be a 
  // deliberately incomplete TaskInfo.
  def consolidateInfoFrom(_events: Seq[TaskEvent]): TaskInfo = {
    val events = _events.sortBy(_.getTime)
    val builder = TaskInfo.newBuilder
    val eventTypes = events.map(_.getType).groupBy(identity).mapValues(_.size)
    for (t <- TaskEventType.values) {
      builder.addNumEventsByType(0)
    }
    for (t <- TaskEventType.values) {
      builder.setNumEventsByType(t.getNumber, eventTypes.getOrElse(t, 0))
    }
    builder.setFinalEvent(events.last.getType)
    events.filter(_.getType == TaskEventType.SUBMIT).headOption.foreach(
      e => builder.setFirstSubmitTime(e.getTime))
    events.filter(_.getType == TaskEventType.SCHEDULE).headOption.foreach(
      e => builder.setFirstScheduleTime(e.getTime))
    events.filter(e => e.getType == TaskEventType.EVICT ||
                       e.getType == TaskEventType.FAIL ||
                       e.getType == TaskEventType.FINISH ||
                       e.getType == TaskEventType.KILL ||
                       e.getType == TaskEventType.LOST).headOption.foreach(
      e => builder.setLastDescheduleTime(e.getTime))
    builder.setNumMissing(events.map(_.hasMissingType).size)
    builder.setNumEvents(events.size)
    return builder.buildPartial
  }

  def markTasks(inEvents: RDD[TaskEvent]): RDD[TaskEvent] = {
    def markOne(events: Seq[TaskEvent]): Seq[TaskEvent] = {
      val extraInfo = consolidateInfoFrom(events)
      events.map(event => {
        val builder = event.toBuilder
        builder.getInfoBuilder.mergeFrom(extraInfo)
        builder.build
      })
    }
    import Join.keyByTask

    inEvents.map(keyByTask).groupByKey.mapValues(markOne).flatMap(_._2)
  }

  def getTaskInfoSamples(inEvents: RDD[TaskEvent]): RDD[(Long, Seq[TaskInfo])] = {
    import Join.keyByTask

    inEvents.map(keyByTask).groupByKey.mapValues(_.head).map(
      kv => kv._1._1 -> kv._2.getInfo).groupByKey
  }
  
  def markJobUtilization(inJobs: RDD[JobUtilization],
                         inEvents: RDD[TaskEvent]): RDD[JobUtilization] = {
    val infos = getTaskInfoSamples(inEvents)
    def mergeJob(kv: (JobUtilization, Seq[TaskInfo])): JobUtilization = {
      import scala.collection.JavaConversions._
      val builder = kv._1.toBuilder
      builder.clearTaskSamples.addAllTaskSamples(kv._2)
      builder.build
    }
    inJobs.map(j => j.getJobInfo.getId -> j).join(infos).
        map(_._2).map(mergeJob)
  }
}
