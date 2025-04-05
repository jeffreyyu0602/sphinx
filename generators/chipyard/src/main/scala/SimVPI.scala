package chipyard

import chisel3._
import chisel3.util._
import chisel3.experimental.{IntParam}

import org.chipsalliance.cde.config.{Parameters, Field}

object SimVPI {
  def connect(clock: Clock, reset: Reset, chipId: Int = 0): Bool = {
    val sim = Module(new SimVPI(chipId))
    sim.io.clock := clock
    sim.io.reset := reset
    val exit = sim.io.exit

    val success = exit === 1.U
    val error = exit >= 2.U
    assert(!error, "*** FAILED *** (exit code = %d)\n", exit >> 1.U)
    success
  }
}

class SimVPI(chipId: Int) extends BlackBox(Map("CHIPID" -> IntParam(chipId))) with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val exit = Output(UInt(32.W))
  })

  addResource("/vsrc/SimVPI.v")
  addResource("/csrc/SimVPI.cc")
  addResource("/testchipip/csrc/testchip_htif.cc")
  addResource("/testchipip/csrc/testchip_htif.h")
  addResource("/testchipip/csrc/testchip_tsi.cc")
  addResource("/testchipip/csrc/testchip_tsi.h")
}
