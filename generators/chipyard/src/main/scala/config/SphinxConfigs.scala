package chipyard

import chisel3._
import org.chipsalliance.cde.config._
import freechips.rocketchip.devices.tilelink.{BootROMLocated, BootROMParams}
import freechips.rocketchip.subsystem.InSubsystem
import gemmini.CounterEvent.n

class WithMyBootROM
    extends Config((site, here, up) => { case BootROMLocated(InSubsystem) =>
      Some(
        BootROMParams(
          contentFileName = "generators/chipyard/src/main/resources/bootrom/bootrom.rv64.img",
          hang            = 0x10000,
        )
      )
    })

case class WithDigitalTopName(name: String) extends Config((site, here, up) => { case DigitalTopName => name })

class SphinxConfig
    extends Config(
      new WithMyBootROM ++
        new chipyard.sphinx.WithTSMC7IOCells ++
        new chipyard.sphinx.WithTSMC7ChipTop ++
        new chipyard.sphinx.WithGPIODriver ++
        // new chipyard.harness.WithSphinxSelected ++
        // new chipyard.harness.WithD2TiedOff ++
        // new WithDigitalTopName("SphinxTop") ++
        // new hyperram.WithHyperRAM ++
        // new chipyard.harness.WithSimHyperRAM ++
        // new chipyard.iobinders.WithHyperRAMIOCells ++
        new freechips.rocketchip.subsystem.WithoutTLMonitors ++
        new testchipip.soc.WithSbusScratchpad(
          base     = 0x40000000L,
          size     = 768 * 1024,
          banks    = 6,
          subBanks = 2,
        ) ++
        new voyager.WithVoyager("MXNF4", 64, 64, 256, 256, 256, 1.0, "tsmc7", "E5M3", true, true) ++
        new chipyard.harness.WithVoyagerVerification ++
        new chipyard.config.WithSystemBusWidth(256) ++
        new voyager.WithMaxBurstSize(128) ++
        new chipyard.clocking.WithSingleClockBroadcastClockGenerator ++
        new testchipip.serdes.WithNoSerialTL ++
        new testchipip.boot.WithNoCustomBootPin ++
        new testchipip.boot.WithNoBootAddrReg ++
        new testchipip.soc.WithNoScratchpads ++                // remove subsystem scratchpads, confusingly named, does not remove the L1D$ scratchpads
        new chipyard.config.WithL2TLBs(0) ++
        new freechips.rocketchip.subsystem.WithNBanks(0) ++    // remove L2$
        new freechips.rocketchip.subsystem.WithNoMemPort ++    // remove backing memory
        new freechips.rocketchip.rocket.WithScratchpadsOnly ++ // use rocket l1 DCache scratchpad as base phys mem
        new freechips.rocketchip.rocket.WithNHugeCores(1) ++   // single rocket-core
        new chipyard.config.WithNoUART ++                      // remove UART
        new chipyard.config.AbstractConfig
    )
