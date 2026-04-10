package chipyard

import chisel3._
import org.chipsalliance.cde.config._
import freechips.rocketchip.devices.tilelink.BootROMLocated

class WithBootROM
    extends Config((site, here, up) => { case BootROMLocated(x) =>
      up(BootROMLocated(x), site)
        .map(
          _.copy(
            hang            = 0x10000,
            contentFileName = "generators/chipyard/src/main/resources/bootrom/bootrom.rv64.img",
          )
        )
    })

class SphinxConfig
    extends Config(
      new WithBootROM ++
        new chipyard.sphinx.WithTSMC7IOCells ++
        new chipyard.sphinx.WithTSMC7ChipTop ++
        new chipyard.sphinx.WithGPIODriver ++
        new freechips.rocketchip.subsystem.WithoutTLMonitors ++
        new testchipip.soc.WithSbusScratchpad(
          base     = 0x40000000L,
          size     = (2 * 1024 * 1024).toLong,
          banks    = 16,
          subBanks = 1,
        ) ++
        new voyager.WithVoyager("MXNF4", 64, 64, 512, 576, 256, 0.9, "tsmc7", "E5M3", true, true, true) ++
        new chipyard.harness.WithVoyagerVerification ++
        new chipyard.config.WithSystemBusWidth(256) ++
        new chipyard.sphinx.WithMaxBurstSize(128) ++
        new chipyard.clocking.WithSingleClockBroadcastClockGenerator ++
        new testchipip.serdes.WithNoSerialTL ++
        new testchipip.boot.WithNoCustomBootPin ++
        new testchipip.boot.WithNoBootAddrReg ++
        new testchipip.soc.WithNoScratchpads ++                // remove subsystem scratchpads, confusingly named, does not remove the L1D$ scratchpads
        new chipyard.config.WithL2TLBs(0) ++
        new freechips.rocketchip.subsystem.WithClockGateModel("/vsrc/TSMC7ClockGate.v") ++
        new freechips.rocketchip.subsystem.WithNBanks(0) ++    // remove L2$
        new freechips.rocketchip.subsystem.WithNoMemPort ++    // remove backing memory
        new freechips.rocketchip.rocket.WithScratchpadsOnly ++ // use rocket l1 DCache scratchpad as base phys mem
        new freechips.rocketchip.rocket.WithNHugeCores(1) ++   // single rocket-core
        new chipyard.config.WithNoUART ++                      // remove UART
        new chipyard.config.AbstractConfig
    )
