package amplab.googletrace

import Protos._
import TraceUtil._

import com.google.protobuf.Message
import com.google.protobuf.{CodedInputStream, CodedOutputStream}

import com.esotericsoftware.kryo.{Kryo, Serializer => KSerializer}
import com.esotericsoftware.kryo.serialize.ArraySerializer
import com.esotericsoftware.kryo.serialize.IntSerializer
import com.esotericsoftware.kryo.compress.DeflateCompressor

import java.nio.ByteBuffer

class KryoRegistrator extends spark.KryoRegistrator {
  def registerClasses(kryo: Kryo): Unit = {
    KryoRegistrator.realRegisterClasses(kryo)
  }
}

object KryoRegistrator {
  import scala.collection.immutable
  class ScalaImmutableMapSerializer(kryo: Kryo) extends KSerializer {
    override final def writeObjectData(buf: ByteBuffer, _obj: AnyRef) {
      val obj = _obj.asInstanceOf[immutable.Map[_, _]]
      IntSerializer.put(buf, obj.size, true)
      obj.foreach { case (k, v) =>
        kryo.writeClassAndObject(buf, k)
        kryo.writeClassAndObject(buf, v)
      }
    }
    override final def readObjectData[U](buf: ByteBuffer, cls: Class[U]): U = {
      val keyValues = scala.collection.mutable.Buffer.empty[(AnyRef, AnyRef)]
      val length = IntSerializer.get(buf, true)
      (1 to length).foreach { _ =>
        val key = kryo.readClassAndObject(buf)
        val value = kryo.readClassAndObject(buf)
        keyValues += key -> value
      }
      return keyValues.toMap.asInstanceOf[U]
    }
  }

  val tlBuffer = new java.lang.ThreadLocal[Array[Byte]] {
    override def initialValue: Array[Byte] = new Array[Byte](1024 * 1024 * 128)
  }
  abstract class PBSerialize[T <: Message] extends KSerializer {
    override final def writeObjectData(buf: ByteBuffer, _obj: AnyRef) {
      val obj = _obj.asInstanceOf[T]
      val tempBuf = tlBuffer.get
      val codedOut = CodedOutputStream.newInstance(tempBuf)
      obj.writeTo(codedOut)
      val len = obj.getSerializedSize
      assert(len == tempBuf.size - codedOut.spaceLeft)
      buf.putInt(obj.getSerializedSize)
      buf.put(tempBuf, 0, len)
    }
    def parseFrom(in: CodedInputStream): T
    override final def readObjectData[U](buf: ByteBuffer, cls: Class[U]): U = {
      val len = buf.getInt
      val tempBuf = tlBuffer.get
      buf.get(tempBuf, 0, len)
      parseFrom(CodedInputStream.newInstance(tempBuf, 0, len)).asInstanceOf[U]
    }
  }

  def realRegisterClasses(kryo: Kryo): Unit = {
    kryo.register(classOf[TaskUsage], new PBSerialize[TaskUsage] {
      final override def parseFrom(in: CodedInputStream) = TaskUsage.parseFrom(in)
    })
    kryo.register(classOf[TaskEvent], new PBSerialize[TaskEvent] {
      final override def parseFrom(in: CodedInputStream) = TaskEvent.parseFrom(in)
    })
    kryo.register(classOf[JobEvent], new PBSerialize[JobEvent] {
      final override def parseFrom(in: CodedInputStream) = JobEvent.parseFrom(in)
    })
    kryo.register(classOf[MachineEvent], new PBSerialize[MachineEvent] {
      final override def parseFrom(in: CodedInputStream) =
          MachineEvent.parseFrom(in)
    })
    kryo.register(classOf[MachineAttribute], new PBSerialize[MachineAttribute] {
      final override def parseFrom(in: CodedInputStream) =
          MachineAttribute.parseFrom(in)
    })
    kryo.register(classOf[MachineConstraint], new PBSerialize[MachineConstraint] {
      final override def parseFrom(in: CodedInputStream) =
          MachineConstraint.parseFrom(in)
    })
    kryo.register(classOf[UsageByMachine], new PBSerialize[UsageByMachine] {
      final override def parseFrom(in: CodedInputStream) =
          UsageByMachine.parseFrom(in)
    })
    kryo.register(classOf[TaskUtilization], new PBSerialize[TaskUtilization] {
      final override def parseFrom(in: CodedInputStream) =
          TaskUtilization.parseFrom(in)
    })
    kryo.register(classOf[JobUtilization], new PBSerialize[JobUtilization] {
      final override def parseFrom(in: CodedInputStream) =
          JobUtilization.parseFrom(in)
    })
    kryo.register(classOf[TaskInfo], new PBSerialize[TaskInfo] {
      final override def parseFrom(in: CodedInputStream) =
          TaskInfo.parseFrom(in)
    })
    for (cls <- List(classOf[Array[TaskUsage]], classOf[Array[TaskEvent]],
                     classOf[Array[JobEvent]], classOf[Array[MachineEvent]],
                     classOf[Array[TaskInfo]], classOf[Array[JobUtilization]],
                     classOf[Array[TaskUtilization]])) {Set(1 -> 0, 2 -> 0, 3 -> 0, 4 -> 0, 5 -> 0).toMap
      kryo.register(cls, new DeflateCompressor(new ArraySerializer(kryo)))
    }

    kryo.register(Set[(Int, Int)]().toMap.getClass, new ScalaImmutableMapSerializer(kryo))
    kryo.register(Set(1 -> 0).toMap.getClass, new ScalaImmutableMapSerializer(kryo))
    kryo.register(Set(1 -> 0, 2 -> 0).toMap.getClass, new ScalaImmutableMapSerializer(kryo))
    kryo.register(Set(1 -> 0, 2 -> 0, 3 -> 0).toMap.getClass,
                  new ScalaImmutableMapSerializer(kryo))
    kryo.register(Set(1 -> 0, 2 -> 0, 3 -> 0, 4 -> 0).toMap.getClass,
                  new ScalaImmutableMapSerializer(kryo))
    kryo.register(Set(1 -> 0, 2 -> 0, 3 -> 0, 4 -> 0, 5 -> 0).toMap.getClass,
                  new ScalaImmutableMapSerializer(kryo))
  }
}
