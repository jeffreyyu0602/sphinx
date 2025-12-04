package chipyard.sphinx

import chisel3._
import chisel3.util.{Cat, HasBlackBoxInline, HasBlackBoxResource, HasBlackBoxPath}
import chisel3.reflect.DataMirror
import chisel3.experimental.{Analog, BaseModule}

import org.chipsalliance.cde.config._

import chipyard.iobinders.IOCellKey
import chipyard.iocell._

abstract class TSMC7IOCell extends BlackBox with HasBlackBoxResource with HasBlackBoxPath {
  addPath("/tsmc7/IP/IO/tphn7_18gpio_univ_130b/TSMCHOME/digital/Front_End/verilog/tphn7_18gpio_univ_130a/tphn7_18gpio_univ.v")
  addResource("/vsrc/TSMC7IOCell.v")
}

class TSMC7DigitalGPIOCellBundle extends DigitalGPIOCellBundle {
  val ds = Input(UInt(4.W))
  val rte = Input(Bool())
}

class TSMC7DigitalInIOCellBundle extends DigitalInIOCellBundle{
  val ds = Input(UInt(4.W))
  val rte = Input(Bool())
}

class TSMC7DigitalOutIOCellBundle extends DigitalOutIOCellBundle {
  val ds = Input(UInt(4.W))
  val rte = Input(Bool())
}

class TSMC7AnalogIOCell extends TSMC7IOCell with AnalogIOCell {
  val io = IO(new AnalogIOCellBundle)
}
class TSMC7DigitalGPIOCellH extends TSMC7IOCell with DigitalGPIOCell {
  val io = IO(new TSMC7DigitalGPIOCellBundle)
}
class TSMC7DigitalInIOCellH extends TSMC7IOCell with DigitalInIOCell {
  val io = IO(new TSMC7DigitalInIOCellBundle)
}
class TSMC7DigitalOutIOCellH extends TSMC7IOCell with DigitalOutIOCell {
  val io = IO(new TSMC7DigitalOutIOCellBundle)
}

class TSMC7DigitalGPIOCellV extends TSMC7IOCell with DigitalGPIOCell {
  val io = IO(new TSMC7DigitalGPIOCellBundle)
}
class TSMC7DigitalInIOCellV extends TSMC7IOCell with DigitalInIOCell {
  val io = IO(new TSMC7DigitalInIOCellBundle)
}
class TSMC7DigitalOutIOCellV extends TSMC7IOCell with DigitalOutIOCell {
  val io = IO(new TSMC7DigitalOutIOCellBundle)
}


case class TSMC7IOCellParams() extends IOCellTypeParams {
  val ioDirectionMap: Map[String, String] = Map(
    "iocell_jtag_TDO" -> "Vertical",
    "iocell_jtag_TDI" -> "Vertical",
    "iocell_jtag_TMS" -> "Vertical",
    "iocell_jtag_TCK" -> "Vertical",
    "iocell_clock" -> "Vertical",
    "iocell_reset" -> "Vertical",
    "iocell_clock_tap" -> "Vertical",
  )

  def analog() = Module(new TSMC7AnalogIOCell)
  def gpio() = Module(new TSMC7DigitalGPIOCellH)
  def input() = Module(new TSMC7DigitalInIOCellH)
  def output() = Module(new TSMC7DigitalOutIOCellH)

  def gpio(nameOpt: Option[String] = None) = {
    val dir = nameOpt.flatMap(ioDirectionMap.get).getOrElse("Horizontal")
    dir match {
      case "Vertical" => Module(new TSMC7DigitalGPIOCellV)
      case _ => Module(new TSMC7DigitalGPIOCellH)
    }
  }

  def input(nameOpt: Option[String] = None) = {
    val dir = nameOpt.flatMap(ioDirectionMap.get).getOrElse("Horizontal")
    println(s"[DEBUG]: TSMC7IOCellParams.input called with name = ${nameOpt.getOrElse("None")}, resolved direction = $dir")
    dir match {
      case "Vertical" => Module(new TSMC7DigitalInIOCellV)
      case _ => Module(new TSMC7DigitalInIOCellH)
    }
  }

  def output(nameOpt: Option[String] = None) = {
    val dir = nameOpt.flatMap(ioDirectionMap.get).getOrElse("Horizontal")
    println(s"[DEBUG]: TSMC7IOCellParams.output called with name = ${nameOpt.getOrElse("None")}, resolved direction = $dir")
    dir match {
      case "Vertical" => Module(new TSMC7DigitalOutIOCellV)
      case _ => Module(new TSMC7DigitalOutIOCellH)
    }
  }
}
