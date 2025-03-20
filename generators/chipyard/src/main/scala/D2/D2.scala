package chipyard.D2

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

class D2MuxIO extends Bundle {
  val designSelect = Input(Bool())
}

class D2ChipTopIO extends Bundle {
  val HCLK      = Input(Bool())
  val PORESETn  = Input(Bool())
  val CRG_DIAG0 = Output(Bool())

  val UART_M_RXD = Input(Bool())
  val UART_M_TXD = Output(Bool())
  val UART_M_RTS = Output(Bool())
  val UART_M_CTS = Input(Bool())

  val UART2_RXD = Input(Bool())
  val UART2_TXD = Output(Bool())

  val NUART0_RXD = Input(Bool())
  val NUART0_TXD = Output(Bool())
}

class D2ChipGPIO extends Bundle {
  val GPIO0 = Vec(3, Analog(1.W))
}

class D2Top extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val HCLK      = Input(Bool())
    val PORESETn  = Input(Bool())
    val CRG_DIAG0 = Output(Bool())

    val UART_M_RXD = Input(Bool())
    val UART_M_TXD = Output(Bool())
    val UART_M_RTS = Output(Bool())
    val UART_M_CTS = Input(Bool())

    val UART2_RXD = Input(Bool())
    val UART2_TXD = Output(Bool())

    val NUART0_RXD = Input(Bool())
    val NUART0_TXD = Output(Bool())

    val GPIO0_0_PORTIN  = Input(Bool())
    val GPIO0_0_PORTOUT = Output(Bool())
    val GPIO0_0_PORTEN  = Output(Bool())

    val GPIO0_1_PORTIN  = Input(Bool())
    val GPIO0_1_PORTOUT = Output(Bool())
    val GPIO0_1_PORTEN  = Output(Bool())

    val GPIO0_2_PORTIN  = Input(Bool())
    val GPIO0_2_PORTOUT = Output(Bool())
    val GPIO0_2_PORTEN  = Output(Bool())

    val HYPERRAM_DQ_0_PORTIN  = Input(Bool())
    val HYPERRAM_DQ_0_PORTOUT = Output(Bool())
    val HYPERRAM_DQ_0_PORTEN  = Output(Bool())

    val HYPERRAM_DQ_1_PORTIN  = Input(Bool())
    val HYPERRAM_DQ_1_PORTOUT = Output(Bool())
    val HYPERRAM_DQ_1_PORTEN  = Output(Bool())

    val HYPERRAM_DQ_2_PORTIN  = Input(Bool())
    val HYPERRAM_DQ_2_PORTOUT = Output(Bool())
    val HYPERRAM_DQ_2_PORTEN  = Output(Bool())

    val HYPERRAM_DQ_3_PORTIN  = Input(Bool())
    val HYPERRAM_DQ_3_PORTOUT = Output(Bool())
    val HYPERRAM_DQ_3_PORTEN  = Output(Bool())

    val HYPERRAM_DQ_4_PORTIN  = Input(Bool())
    val HYPERRAM_DQ_4_PORTOUT = Output(Bool())
    val HYPERRAM_DQ_4_PORTEN  = Output(Bool())

    val HYPERRAM_DQ_5_PORTIN  = Input(Bool())
    val HYPERRAM_DQ_5_PORTOUT = Output(Bool())
    val HYPERRAM_DQ_5_PORTEN  = Output(Bool())

    val HYPERRAM_DQ_6_PORTIN  = Input(Bool())
    val HYPERRAM_DQ_6_PORTOUT = Output(Bool())
    val HYPERRAM_DQ_6_PORTEN  = Output(Bool())

    val HYPERRAM_DQ_7_PORTIN  = Input(Bool())
    val HYPERRAM_DQ_7_PORTOUT = Output(Bool())
    val HYPERRAM_DQ_7_PORTEN  = Output(Bool())

    val HYPERRAM_RWDS_PORTIN  = Input(Bool())
    val HYPERRAM_RWDS_PORTOUT = Output(Bool())
    val HYPERRAM_RWDS_PORTEN  = Output(Bool())

    val HYPERRAM_RESETN = Output(Bool())
    val HYPERRAM_CSN0   = Output(Bool())
    val HYPERRAM_CK     = Output(Clock())
    val HYPERRAM_CKN    = Output(Clock())
  })
  addResource("/vsrc/D2Top.sv")
}
