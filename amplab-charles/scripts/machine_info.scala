import amplab.googletrace.Convert._
import amplab.googletrace.Join._
import amplab.googletrace.Protos._
import amplab.googletrace.Util._
import amplab.googletrace.TraceUtil._
import amplab.googletrace.Stored._
import amplab.googletrace.TaskAggregates._
import amplab.googletrace.Utilizations._
import amplab.googletrace.ToNumpy

val machines = readSavedMachines(sc, outDir + "/machine_events")

lazy val machinesById =
  machines.filter(_.getUp).map(x => x.getInfo.getId -> x).collect.toMap

def machineValues(info: MachineInfo): (Array[String], Array[Float]) = {
  Array(info.getPlatformId.substring(0, 1)) ->
  Array(info.getCapacity.getCpus, info.getCapacity.getMemory)
}

lazy val machineConverter =
  ToNumpy.converter[MachineInfo](machineValues, 
    Array[String]("platform") -> Array[String]("cpu", "memory"))

def writeMachines: Unit = {
  machineConverter.writeNumpy(
    outDir + "/machine_configs.npy",
    machinesById.values.map(_.getInfo).toArray
  )
}
