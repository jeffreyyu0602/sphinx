package chipyard.sphinx

import chisel3._
import chisel3.util.{HasBlackBoxPath}
import chisel3.experimental.{IntParam, StringParam}

class PRTEDISABLE extends BlackBox with HasBlackBoxPath {
  val io = IO(new Bundle {
    val RTE = Output(Bool()) 
  })
  addPath("/tsmc7/IP/IO/tphn7_18gpio_univ_130b/TSMCHOME/digital/Front_End/verilog/tphn7_18gpio_univ_130a/tphn7_18gpio_univ.v")
}
