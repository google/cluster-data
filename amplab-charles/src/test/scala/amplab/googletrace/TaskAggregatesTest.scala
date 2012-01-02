package amplab.googletrace

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import spark.SparkContext

import Protos._
import TaskAggregates._

import com.google.protobuf.TextFormat

class TaskAggregatesTest extends FunSuite with ShouldMatchers {
  test("consolidateTaskFrom for one element") {
    val taskEventBuilder = TaskEvent.newBuilder
    TextFormat.merge("""
      time: 1000 missing_type: EXISTS_BUT_NO_CREATION
      type: KILL
      info: < task_index: 99 job: < id: 100 > >
    """, taskEventBuilder)
    val taskEvents = Seq(taskEventBuilder.build)
    val extraInfo = consolidateInfoFrom(taskEvents)
    val taskInfoBuilder = TaskInfo.newBuilder
    TextFormat.merge("""
      num_events_by_type: 0 num_events_by_type: 0
      num_events_by_type: 0 num_events_by_type: 0 num_events_by_type: 0
          num_events_by_type: 1
      num_events_by_type: 0
      num_events_by_type: 0 num_events_by_type: 0
      final_event: KILL
      num_missing: 1
      last_deschedule_time: 1000
      num_events: 1
    """, taskInfoBuilder)
    assert(taskInfoBuilder.buildPartial === extraInfo)
  }
}
