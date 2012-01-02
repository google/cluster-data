package amplab.googletrace

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import spark.SparkContext

import Utilizations._
import TraceUtil._
import Protos._

import scala.math.abs

import com.google.protobuf.TextFormat

class UtilizationsTestSuite extends FunSuite with ShouldMatchers {
  test("getPercentile on 1 elements") {
    val single = preparePercentileList(
      Array(42.0f -> 100.0f)
    )
    assert(42.0f === getPercentile(0.0, single))
    assert(42.0f === getPercentile(1.0, single))
    assert(42.0f === getPercentile(0.5, single))
    assert(42.0f === getPercentile(0.3, single))
  }

  test("getPercentile example 1") {
    val single = preparePercentileList(
      Array(42.0f -> 100.0f, 43.0f -> 50.0f, 41.0f -> 50.0f)
    )
    assert(41.0f === getPercentile(0.0, single))
    assert(43.0f === getPercentile(1.0, single))
    assert(42.0f === getPercentile(0.6, single))
    assert(42.0f === getPercentile(0.5, single))
    assert(41.0f === getPercentile(0.2, single))
  }

  test("getPercentile example 2") {
    val three = preparePercentileList(
      Array(0.0f -> 25.0f, 2.0f -> 50.0f, 5.0f -> 100.0f)
    )
    assert(0.0f === getPercentile(0.0, three))
    assert(5.0f === getPercentile(1.0, three))
    assert(5.0f === getPercentile(0.5, three))
    assert(2.0f === getPercentile(0.25, three))
    assert(0.0f === getPercentile(0.10, three))
  }

  test("getPercentile example 3") {
    val three = preparePercentileList(
      Array(0.0f -> 25.0f, 2.0f -> 50.0f)
    )
    assert(0.0f === getPercentile(0.0, three))
    assert(2.0f === getPercentile(1.0, three))
    assert(0.0f === getPercentile(0.25, three))
    assert(2.0f === getPercentile(0.40, three))
  }

  test("getTaskUtilization missing request") {
    val usageBuilder = TaskUsage.newBuilder
    TextFormat.merge("""
      start_time: 0 end_time: 10
      resources: < cpus: 0.5 memory: 0.25 >
      max_resources: < cpus: 0.75 memory: 0.125 >
      task_info: < job: < id: 42 > task_index: 10 >
    """, usageBuilder)
    assert(None == getTaskUtilization(Seq(usageBuilder.build)))
  }

  test("getTaskUtilization on 1 record") {
    val usageBuilder = TaskUsage.newBuilder
    TextFormat.merge("""
      start_time: 0 end_time: 10
      resources: < cpus: 0.5 memory: 0.25 >
      max_resources: < cpus: 0.75 memory: 0.125 >
      task_info: < job: < id: 42 > task_index: 10
        requested_resources: <
          cpus: 0.25 memory: 0.75
        >
      >
    """, usageBuilder)
    val usage = usageBuilder.build

    val expectUtilBuilder = TaskUtilization.newBuilder
    TextFormat.merge("""
      info: <
        job: < id: 42 > task_index: 10
        requested_resources: <
          cpus: 0.25 memory: 0.75
        >
      >

      min_request: <
        cpus: 0.25 memory: 0.75
      >
      max_request: <
        cpus: 0.25 memory: 0.75
      >
      
      usage_percentile: 0.01
      usage_percentile: 0.25
      usage_percentile: 0.5
      usage_percentile: 0.75
      usage_percentile: 0.99
      usage_percentile: 1.0
      percentile_task_usage: < cpus: 0.75 memory: 0.125 >
      percentile_task_usage: < cpus: 0.75 memory: 0.125 >
      percentile_task_usage: < cpus: 0.75 memory: 0.125 >
      percentile_task_usage: < cpus: 0.75 memory: 0.125 >
      percentile_task_usage: < cpus: 0.75 memory: 0.125 >
      percentile_task_usage: < cpus: 0.75 memory: 0.125 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >

      start_time: 0
      end_time: 10
      running_time: 10
    """, expectUtilBuilder)
    assert(Some(expectUtilBuilder.build) === getTaskUtilization(Seq(usage)))
  }

  test("getTaskUtilization on 2 records") {
    val usageBuilder = TaskUsage.newBuilder
    TextFormat.merge("""
      start_time: 0 end_time: 10
      resources: < cpus: 0.5 memory: 0.25 >
      max_resources: < cpus: 1.0 memory: 1.0 >
      task_info: < job: < id: 42 > task_index: 10
        requested_resources: <
          cpus: 0.25 memory: 0.75
        >
      >
    """, usageBuilder)
    val usage1 = usageBuilder.build
    usageBuilder.clear
    TextFormat.merge("""
      start_time: 20 end_time: 40
      resources: < cpus: 0.25 memory: 0.125 >
      max_resources: < cpus: 2.0 memory: 2.0 >
      task_info: < job: < id: 42 > task_index: 10
        requested_resources: <
          cpus: 0.5 memory: 0.25
        >
      >
    """, usageBuilder)
    val usage2 = usageBuilder.build

    /* TODO(Charles): weighted average by runtime */
    val expectUtilBuilder = TaskUtilization.newBuilder
    TextFormat.merge("""
      info: <
        job: < id: 42 > task_index: 10
        requested_resources: <
          cpus: 0.25 memory: 0.75
        >
      >

      min_request: <
        cpus: 0.25 memory: 0.25
      >
      max_request: <
        cpus: 0.5 memory: 0.75
      >
     
      usage_percentile: 0.01
      usage_percentile: 0.25 
      usage_percentile: 0.5
      usage_percentile: 0.75
      usage_percentile: 0.99
      usage_percentile: 1.0

      start_time: 0
      end_time: 40
      running_time: 30
    """, expectUtilBuilder)
    for (percentile <- PER_TASK_PERCENTILES) {
      def f(d1: Float, d2: Float): Float =
        if (percentile <= 2.0/3.0)
          d1
        else
          d2
      def g(d1: Float, d2: Float): Float =
        if (percentile <= 1.0/3.0)
          d1
        else
          d2
      expectUtilBuilder.addPercentileTaskUsage(
        Resources.newBuilder.setCpus(g(1.0f, 2.0f)).
          setMemory(g(1.0f, 2.0f))
      ).addPercentileMeanTaskUsage(
        Resources.newBuilder.setCpus(f(0.25f, 0.5f)).
          setMemory(f(0.125f, 0.25f))
      )
    }
    assert(Some(expectUtilBuilder.build) ===
           getTaskUtilization(Seq(usage1, usage2)))
  }

  test("getJobUtilization on 1 record") {
    val taskUtilBuilder = TaskUtilization.newBuilder
    TextFormat.merge("""
      info: <
        job: < id: 42 > task_index: 10
        requested_resources: <
          cpus: 0.25 memory: 0.75
        >
        final_event: SCHEDULE
        num_events_by_type: 1 num_events_by_type: 2 num_events_by_type: 3
        num_events_by_type: 4 num_events_by_type: 5 num_events_by_type: 6
        num_events_by_type: 7 num_events_by_type: 8 num_events_by_type: 9
      >

      min_request: <
        cpus: 0.25 memory: 0.75
      >
      max_request: <
        cpus: 0.25 memory: 0.75
      >
      
      usage_percentile: 0.5
      usage_percentile: 1.0
      percentile_task_usage: < cpus: 1.0 memory: 0.125 >
      percentile_task_usage: < cpus: 0.75 memory: 0.125 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >

      start_time: 0
      end_time: 20
      running_time: 10
    """, taskUtilBuilder)
    val jobUtilBuilder = JobUtilization.newBuilder
    TextFormat.merge("""
      job_info: < id: 42 >
      num_tasks: 1
      min_request: <
        cpus: 0.25 memory: 0.75
      >
      max_request: <
        cpus: 0.25 memory: 0.75
      >

      task_percentile: 0.01
      task_percentile: 0.25
      task_percentile: 0.5
      task_percentile: 0.75
      task_percentile: 0.99
      task_percentile: 1.0
      task_percentile: 0.01
      task_percentile: 0.25
      task_percentile: 0.5
      task_percentile: 0.75
      task_percentile: 0.99
      task_percentile: 1.0
      usage_percentile: 0.5
      usage_percentile: 0.5
      usage_percentile: 0.5
      usage_percentile: 0.5
      usage_percentile: 0.5
      usage_percentile: 0.5
      usage_percentile: 1.0
      usage_percentile: 1.0
      usage_percentile: 1.0
      usage_percentile: 1.0
      usage_percentile: 1.0
      usage_percentile: 1.0
      percentile_task_usage: < cpus: 1.0 memory: 0.125 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >
      percentile_task_usage: < cpus: 1.0 memory: 0.125 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >
      percentile_task_usage: < cpus: 1.0 memory: 0.125 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >
      percentile_task_usage: < cpus: 1.0 memory: 0.125 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >
      percentile_task_usage: < cpus: 1.0 memory: 0.125 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >
      percentile_task_usage: < cpus: 1.0 memory: 0.125 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >

      percentile_task_usage: < cpus: 0.75 memory: 0.125 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >
      percentile_task_usage: < cpus: 0.75 memory: 0.125 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >
      percentile_task_usage: < cpus: 0.75 memory: 0.125 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >
      percentile_task_usage: < cpus: 0.75 memory: 0.125 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >
      percentile_task_usage: < cpus: 0.75 memory: 0.125 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >
      percentile_task_usage: < cpus: 0.75 memory: 0.125 >
      percentile_mean_task_usage: < cpus: 0.5 memory: 0.25 >

      start_time: 0
      end_time: 20
      running_time: 10

      num_tasks_final: 0 num_tasks_final: 1 num_tasks_final: 0
      num_tasks_final: 0 num_tasks_final: 0 num_tasks_final: 0
      num_tasks_final: 0 num_tasks_final: 0 num_tasks_final: 0

      num_events_by_type: 1 num_events_by_type: 2 num_events_by_type: 3
      num_events_by_type: 4 num_events_by_type: 5 num_events_by_type: 6
      num_events_by_type: 7 num_events_by_type: 8 num_events_by_type: 9
    """, jobUtilBuilder)
    jobUtilBuilder.addTaskSamples(taskUtilBuilder.getInfo)
    assert(jobUtilBuilder.build ===
      getJobUtilization(Seq(taskUtilBuilder.build)))
  }
}
