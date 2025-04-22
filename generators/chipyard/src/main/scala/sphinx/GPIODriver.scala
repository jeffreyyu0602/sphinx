package chipyard.sphinx

import chisel3._
import chisel3.util._
import chisel3.experimental.{IntParam, BaseModule}
import freechips.rocketchip.subsystem.{BaseSubsystem, PBUS}
import org.chipsalliance.cde.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper.{HasRegMap, RegField}
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._
import freechips.rocketchip.interrupts.{IntSourceNode, IntSourcePortSimple}

case class GPIODriverConfig(
  mmioAddress: BigInt = A.MMIOGPIODRIVER
)

case object GPIODriverKey extends Field[Option[GPIODriverConfig]](None)

class TLGPIODriver(address: BigInt, pbusBeatBytes: Int)(implicit p: Parameters) extends LazyModule {
  val device = new SimpleDevice("gpio-driver", Seq("stanford,gpio-driver"))

  val node = TLRegisterNode(
    address = Seq(AddressSet(address, 0xff)),
    device = device,
    beatBytes = pbusBeatBytes,
    concurrency = 1)

  lazy val module = new GPIODriverModuleImp(this)
}

class GPIODriverTopIO extends Bundle {
  val ds = Output(UInt(4.W))
}

trait HasGPIODriverTopIO {
  def io: GPIODriverTopIO
}

class GPIODriverModuleImp(outer: TLGPIODriver) extends LazyModuleImp(outer) with HasGPIODriverTopIO {
  val config = p(GPIODriverKey).get

  val ds = Reg(UInt(4.W))
  val io = IO(new GPIODriverTopIO)
  io.ds := ds

  outer.node.regmap(
    0x00 -> Seq(RegField(4, ds)),
  )
}

trait CanHavePeripheryGPIODriver { this: BaseSubsystem =>
  val ioDriveStrength = p(GPIODriverKey) match {
    case Some(params) => {
      val pbus = locateTLBusWrapper(PBUS)

      val gpioDriver = pbus {
        LazyModule(new TLGPIODriver(A.MMIOGPIODRIVER, pbus.beatBytes)(p))
      }

      pbus.coupleTo("gpio-driver") {
        gpioDriver.node := TLFragmenter(pbus.beatBytes, pbus.blockBytes) := _
      }

      val pbusIO = pbus { InModuleBody {
        val ds = IO(Output(UInt(4.W)))
        ds := gpioDriver.module.io.ds
        ds
      }}

      val dsIO = InModuleBody {
        val ds = IO(Output(UInt(4.W))).suggestName("ioDriveStrength")
        ds := pbusIO
        ds
      }

      Some(dsIO)
    }
    case None => None
  }
}

class WithGPIODriver extends Config((site, here, up) => {
  case GPIODriverKey => Some(GPIODriverConfig(mmioAddress = A.MMIOGPIODRIVER))
})
