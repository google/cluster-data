package amplab.googletrace

import org.scalatest.FunSuite
import spark.SparkContext

import Convert._
import Join._

object JoinTestingUtils {
  case class A(t: Long, k: Int, x: String)
  case class B(t: Long, k: Int, y: String)
  implicit def timeOfA: TimeOf[A] = new TimeOf[A] {
    def apply(a: A) = a.t
  }
  implicit def timeOfB: TimeOf[B] = new TimeOf[B] {
    def apply(b: B) = b.t
  }
  implicit def insertAB: Insert[A, B, Int] = new Insert[A, B, Int] {
    override def hasThroughT: Boolean = true
    override def throughT(a: A): Boolean = a.k == -1
    def apply(into: A, value: B): A = {
      A(into.t, into.k, into.x + value.y)
    }
    def keyT(a: A): Int = a.k
    def keyU(b: B): Int = b.k
  }
}

class ConvertTestSuite extends FunSuite {
  test("placeJoined") {
    import JoinTestingUtils._
    val sc = new SparkContext("local", "test")
    val join = placeJoined(sc.makeRDD(
      Array(
        A(0, 100, "tooearly"),
        A(1, 100, "A1:"),
        A(2, 100, "A2:"),
        A(3, 101, "nojoin"),
        A(4, 102, "A3:"),
        A(5, -1, "through")
      )
    ), sc.makeRDD(
      Array(
        B(1, 100, "B0"),
        B(1, 102, "B1"),
        B(1, -1, "not in output")
      )
    ))
    val results = join.collect().toList
    assert(results.map(_.x).toSet === Set(
      "A1:B0", "A2:B0", "A3:B1", "nojoin", "tooearly", "through"
    ))
  }

  test("placeJoinedBig") {
    import JoinTestingUtils._
    val sc = new SparkContext("local", "test")
    val join = placeJoinedBig(sc.makeRDD(
      Array(
        A(0, 100, "tooearly"),
        A(1, 100, "A1:"),
        A(2, 100, "A2:"),
        A(3, 101, "nojoin"),
        A(4, 102, "A3:"),
        A(5, -1, "through"),
        A(TIME_PERIOD + 1, 100, "A4:"),
        A(TIME_PERIOD * 2 + 1, 100, "A5:"),
        A(TIME_PERIOD + 1, 102, "A6:"),
        A(TIME_PERIOD * 5 + 1, 102, "A7:")
      )
    ), sc.makeRDD(
      Array(
        B(1, 100, "B0"),
        B(1, 102, "B1"),
        B(1, -1, "not in output"),
        B(TIME_PERIOD + 1, 102, "B2")
      )
    ))
    val results = join.collect().toList
    assert(results.map(_.x).toSet === Set(
      "A1:B0", "A2:B0", "A3:B1", "nojoin", "tooearly", "through",

      "A4:B0", "A5:B0", "A6:B2", "A7:B2"
    ))
  }

  test("broadcastPlaceJoined") {
    import JoinTestingUtils._
    val sc = new SparkContext("local", "test")
    val join = broadcastPlaceJoined(sc.makeRDD(
      Array(
        A(0, 100, "tooearly"),
        A(1, 100, "A1:"),
        A(2, 100, "A2:"),
        A(3, 101, "nojoin"),
        A(4, 102, "A3:"),
        A(5, -1, "through")
      )
    ), sc.makeRDD(
      Array(
        B(1, 100, "B0"),
        B(1, 102, "B1"),
        B(1, -1, "not in output")
      )
    ))
    val results = join.collect().toList
    assert(results.map(_.x).toSet === Set(
      "A1:B0", "A2:B0", "A3:B1", "nojoin", "tooearly", "through"
    ))
  }
}
