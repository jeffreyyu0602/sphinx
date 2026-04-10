package chipyard.sphinx

import chisel3._
import chisel3.util.{Cat, HasBlackBoxPath, HasBlackBoxResource}
import chisel3.reflect.DataMirror
import chisel3.experimental.{Analog, BaseModule}

import org.chipsalliance.cde.config._

import chipyard.iobinders.IOCellKey
import chipyard.iocell._

trait HasCellNameAndRTE {
  val cellName: Option[String]
  val io: Bundle {
    val ds: UInt
    val rte: Bool
  }
}

abstract class TSMC7IOCell extends BlackBox with HasBlackBoxResource with HasBlackBoxPath {
  addPath(
    "/tsmc7/IP/IO/tphn7_18gpio_univ_130b/TSMCHOME/digital/Front_End/verilog/tphn7_18gpio_univ_130a/tphn7_18gpio_univ.v"
  )
  addResource("/vsrc/TSMC7IOCell.v")
}

class TSMC7DigitalGPIOCellBundle extends DigitalGPIOCellBundle {
  val ds  = Input(UInt(4.W))
  val rte = Input(Bool())
}

class TSMC7DigitalInIOCellBundle extends DigitalInIOCellBundle {
  val ds  = Input(UInt(4.W))
  val rte = Input(Bool())
}

class TSMC7DigitalOutIOCellBundle extends DigitalOutIOCellBundle {
  val ds  = Input(UInt(4.W))
  val rte = Input(Bool())
}

class TSMC7AnalogIOCell extends TSMC7IOCell with AnalogIOCell {
  val io = IO(new AnalogIOCellBundle)
}

// -------------------------------
// Digital GPIO Cells
// -------------------------------
class TSMC7DigitalGPIOCellH(val nameOpt: Option[String])
    extends TSMC7IOCell
    with DigitalGPIOCell
    with HasCellNameAndRTE {
  val cellName = nameOpt
  val io       = IO(new TSMC7DigitalGPIOCellBundle)
}

class TSMC7DigitalGPIOCellV(val nameOpt: Option[String])
    extends TSMC7IOCell
    with DigitalGPIOCell
    with HasCellNameAndRTE {
  val cellName = nameOpt
  val io       = IO(new TSMC7DigitalGPIOCellBundle)
}

// -------------------------------
// Digital Input Cells
// -------------------------------
class TSMC7DigitalInIOCellH(val nameOpt: Option[String])
    extends TSMC7IOCell
    with DigitalInIOCell
    with HasCellNameAndRTE {
  val cellName = nameOpt
  val io       = IO(new TSMC7DigitalInIOCellBundle)
}

class TSMC7DigitalInIOCellV(val nameOpt: Option[String])
    extends TSMC7IOCell
    with DigitalInIOCell
    with HasCellNameAndRTE {
  val cellName = nameOpt
  val io       = IO(new TSMC7DigitalInIOCellBundle)
}

// -------------------------------
// Digital Output Cells
// -------------------------------
class TSMC7DigitalOutIOCellH(val nameOpt: Option[String])
    extends TSMC7IOCell
    with DigitalOutIOCell
    with HasCellNameAndRTE {
  val cellName = nameOpt
  val io       = IO(new TSMC7DigitalOutIOCellBundle)
}

class TSMC7DigitalOutIOCellV(val nameOpt: Option[String])
    extends TSMC7IOCell
    with DigitalOutIOCell
    with HasCellNameAndRTE {
  val cellName = nameOpt
  val io       = IO(new TSMC7DigitalOutIOCellBundle)
}

case class TSMC7IOCellParams() extends IOCellTypeParams {
  val ioDirectionMap: Map[String, String] = Map(
    "iocell_jtag_TDO"  -> "Horizontal",
    "iocell_jtag_TDI"  -> "Horizontal",
    "iocell_jtag_TMS"  -> "Horizontal",
    "iocell_jtag_TCK"  -> "Horizontal",
    "iocell_clock"     -> "Horizontal",
    "iocell_reset"     -> "Horizontal",
    "iocell_clock_tap" -> "Horizontal",
  )

  def analog() = Module(new TSMC7AnalogIOCell)
  def gpio()   = Module(new TSMC7DigitalGPIOCellH(Some("unknown")))
  def input()  = Module(new TSMC7DigitalInIOCellH(Some("unknown")))
  def output() = Module(new TSMC7DigitalOutIOCellH(Some("unknown")))

  def gpio(nameOpt: Option[String] = None) = {
    val dir = nameOpt.flatMap(ioDirectionMap.get).getOrElse("Horizontal")
    dir match {
      case "Vertical" => Module(new TSMC7DigitalGPIOCellV(nameOpt))
      case _          => Module(new TSMC7DigitalGPIOCellH(nameOpt))
    }
  }

  def input(nameOpt: Option[String] = None) = {
    val dir = nameOpt.flatMap(ioDirectionMap.get).getOrElse("Horizontal")
    println(
      s"[DEBUG]: TSMC7IOCellParams.input called with name = ${nameOpt.getOrElse("None")}, resolved direction = $dir"
    )
    dir match {
      case "Vertical" => Module(new TSMC7DigitalInIOCellV(nameOpt))
      case _          => Module(new TSMC7DigitalInIOCellH(nameOpt))
    }
  }

  def output(nameOpt: Option[String] = None) = {
    val dir = nameOpt.flatMap(ioDirectionMap.get).getOrElse("Horizontal")
    println(
      s"[DEBUG]: TSMC7IOCellParams.output called with name = ${nameOpt.getOrElse("None")}, resolved direction = $dir"
    )
    dir match {
      case "Vertical" => Module(new TSMC7DigitalOutIOCellV(nameOpt))
      case _          => Module(new TSMC7DigitalOutIOCellH(nameOpt))
    }
  }
}

class TSMC7POCMCellV extends TSMC7IOCell {
  val io = IO(new Bundle {
    val rte = Input(Bool())
  })
}

class TSMC7POCMCellH extends TSMC7IOCell {
  val io = IO(new Bundle {
    val rte = Input(Bool())
  })
}
