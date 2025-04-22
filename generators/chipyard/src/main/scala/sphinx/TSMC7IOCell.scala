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
}

class TSMC7DigitalInIOCellBundle extends DigitalInIOCellBundle{
  val ds = Input(UInt(4.W))
}

class TSMC7DigitalOutIOCellBundle extends DigitalOutIOCellBundle {
  val ds = Input(UInt(4.W))
}

class TSMC7AnalogIOCell extends TSMC7IOCell with AnalogIOCell {
  val io = IO(new AnalogIOCellBundle)
}
class TSMC7DigitalGPIOCell extends TSMC7IOCell with DigitalGPIOCell {
  val io = IO(new TSMC7DigitalGPIOCellBundle)
}
class TSMC7DigitalInIOCell extends TSMC7IOCell with DigitalInIOCell {
  val io = IO(new TSMC7DigitalInIOCellBundle)
}
class TSMC7DigitalOutIOCell extends TSMC7IOCell with DigitalOutIOCell {
  val io = IO(new TSMC7DigitalOutIOCellBundle)
}

case class TSMC7IOCellParams() extends IOCellTypeParams {
  def analog() = Module(new TSMC7AnalogIOCell)
  def gpio() = Module(new TSMC7DigitalGPIOCell)
  def input() = Module(new TSMC7DigitalInIOCell)
  def output() = Module(new TSMC7DigitalOutIOCell)
}
