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

class GPIODriverTopIO extends Bundle {
  val ds = Output(UInt(4.W))
  val rte = Output(Bool())
}

trait HasGPIODriverTopIO {
  def io: GPIODriverTopIO
}

class TLGPIODriver(address: BigInt, pbusBeatBytes: Int)(implicit p: Parameters) extends LazyModule {
  val device = new SimpleDevice("gpio-driver", Seq("stanford,gpio-driver"))

  val node = TLRegisterNode(
    address = Seq(AddressSet(address, 0xff)),
    device = device,
    beatBytes = pbusBeatBytes,
    concurrency = 1)

  lazy val module = new LazyModuleImp(this) with HasGPIODriverTopIO {
    // Register definitions
    val ds = RegInit(0.U(4.W))
    val rte = RegInit(false.B)

    // IO Definition
    val io = IO(new GPIODriverTopIO)
    io.ds := ds
    io.rte := rte

    // Register Mapping
    node.regmap(
      0x00 -> Seq(RegField(4, ds)),
      0x04 -> Seq(RegField(1, rte)),
    )
  }
}

trait CanHavePeripheryGPIODriver { this: BaseSubsystem =>
  val gpioDriverIO = p(GPIODriverKey) match {
    case Some(params) => {
      val pbus = locateTLBusWrapper(PBUS)

      // STEP 1: The "Inner" Logic (Inside PBUS)
      // We use pbus { ... } to ensure the driver gets the PBUS Clock/Reset.
      val innerIO = pbus {
        val gpioDriver = LazyModule(new TLGPIODriver(params.mmioAddress, pbus.beatBytes)(p))

        pbus.coupleTo("gpio-driver") {
          gpioDriver.node := TLFragmenter(pbus.beatBytes, pbus.blockBytes) := _
        }

        // Create an IO on the PBUS module boundary to expose the signals
        val wiring = InModuleBody {
          val io = IO(Output(new GPIODriverTopIO))
          io.ds  := gpioDriver.module.io.ds
          io.rte := gpioDriver.module.io.rte
          io
        }

        // Return this "middle-man" IO so DigitalTop can see it
        wiring
      }

      // STEP 2: The "Outer" Logic (Inside DigitalTop)
      // Now we connect the PBUS output to the actual Chip Pins
      val topIO = InModuleBody {
        val io = IO(Output(new GPIODriverTopIO)).suggestName("gpio_driver")

        // Connect DigitalTop IO <--- PBUS IO
        io.ds  := innerIO.ds
        io.rte := innerIO.rte
        io
      }

      Some(topIO)
    }
    case None => None
  }
}

class WithGPIODriver extends Config((site, here, up) => {
  case GPIODriverKey => Some(GPIODriverConfig(mmioAddress = A.MMIOGPIODRIVER))
})
