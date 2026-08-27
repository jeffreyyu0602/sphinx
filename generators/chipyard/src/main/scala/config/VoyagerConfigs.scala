package chipyard

import org.chipsalliance.cde.config.Config

// Voyager-integrated configs
class E4M3x16x16
    extends Config(
      new voyager.WithVoyager("E4M3", 16, 16, 1024, 1024, 1024, 5.0, "generic") ++
        new chipyard.harness.WithVoyagerVerification ++
        new freechips.rocketchip.subsystem.WithoutTLMonitors ++
        new chipyard.config.WithSystemBusWidth(128) ++
        new VoyagerRocketConfig
    )

class Posit8x16x16
    extends Config(
      new voyager.WithVoyager("P8_1", 16, 16, 1024, 1024, 1024, 5.0, "generic") ++
        new chipyard.harness.WithVoyagerVerification ++
        new freechips.rocketchip.subsystem.WithoutTLMonitors ++
        new chipyard.config.WithSystemBusWidth(128) ++
        new VoyagerRocketConfig
    )

class INT8x16x16
    extends Config(
      new voyager.WithVoyager("INT8", 16, 16, 1024, 1024, 1024, 5.0, "generic") ++
        new chipyard.harness.WithVoyagerVerification ++
        new freechips.rocketchip.subsystem.WithoutTLMonitors ++
        new chipyard.config.WithSystemBusWidth(128) ++
        new VoyagerRocketConfig
    )

class MXINT8x16x16
    extends Config(
      new voyager.WithVoyager("MXINT8", 16, 16, 1024, 1024, 1024, 5.0, "generic", "E8M0") ++
        new chipyard.harness.WithVoyagerVerification ++
        new freechips.rocketchip.subsystem.WithoutTLMonitors ++
        new chipyard.config.WithSystemBusWidth(128) ++
        new VoyagerRocketConfig
    )

class MXNF4x64x64
    extends Config(
      new voyager.WithVoyager("MXNF4", 64, 64, 1024, 1024, 1024, 5.0, "generic", "E5M3", true, true, true) ++
        new chipyard.harness.WithVoyagerVerification ++
        new freechips.rocketchip.subsystem.WithoutTLMonitors ++
        new chipyard.config.WithSystemBusWidth(256) ++
        new chipyard.sphinx.WithMaxBurstSize(128) ++
        new VoyagerRocketConfig
    )

class E4M3x32x64
    extends Config(
      new voyager.WithVoyager("E4M3", 32, 64, 1024, 1024, 1024, 5.0, "generic") ++
        new chipyard.harness.WithVoyagerVerification ++
        new freechips.rocketchip.subsystem.WithoutTLMonitors ++
        new chipyard.config.WithSystemBusWidth(256) ++
        new VoyagerRocketConfig
    )

class E4M3x32x32
    extends Config(
      new voyager.WithVoyager("E4M3", 32, 32, 1024, 1024, 1024, 5.0, "generic") ++
        new chipyard.harness.WithVoyagerVerification ++
        new freechips.rocketchip.subsystem.WithoutTLMonitors ++
        new chipyard.config.WithSystemBusWidth(256) ++
        new VoyagerRocketConfig
    )

class E4M3x16x32
    extends Config(
      new voyager.WithVoyager("E4M3", 16, 32, 1024, 1024, 1024, 5.0, "generic") ++
        new chipyard.harness.WithVoyagerVerification ++
        new freechips.rocketchip.subsystem.WithoutTLMonitors ++
        new chipyard.config.WithSystemBusWidth(128) ++
        new VoyagerRocketConfig
    )

class E4M3x8x16
    extends Config(
      new voyager.WithVoyager("E4M3", 8, 16, 1024, 1024, 1024, 5.0, "generic") ++
        new chipyard.harness.WithVoyagerVerification ++
        new freechips.rocketchip.subsystem.WithoutTLMonitors ++
        new chipyard.config.WithSystemBusWidth(64) ++
        new VoyagerRocketConfig
    )

class E4M3x8x8
    extends Config(
      new voyager.WithVoyager("E4M3", 8, 8, 1024, 1024, 1024, 5.0, "generic") ++
        new chipyard.harness.WithVoyagerVerification ++
        new freechips.rocketchip.subsystem.WithoutTLMonitors ++
        new chipyard.config.WithSystemBusWidth(64) ++
        new VoyagerRocketConfig
    )

class E4M3x4x8
    extends Config(
      new voyager.WithVoyager("E4M3", 4, 8, 1024, 1024, 1024, 5.0, "generic") ++
        new chipyard.harness.WithVoyagerVerification ++
        new freechips.rocketchip.subsystem.WithoutTLMonitors ++
        new chipyard.config.WithSystemBusWidth(32) ++
        new VoyagerRocketConfig
    )

class VoyagerDebugConfig
    extends Config(
      new voyager.WithVoyager("E4M3", 16, 16, 1024, 1024, 1024, 5.0, "generic") ++
        new chipyard.harness.WithVoyagerVerification ++
        new VoyagerRocketConfig
    )

class VoyagerRocketConfig
    extends Config(
      new chipyard.sphinx.WithDefaultMaxBurstSize ++
        new freechips.rocketchip.rocket.WithNHugeCores(1) ++ // single rocket-core
        new AbstractConfig
    )

class AbstractConfig
    extends Config(
      // ================================================
      //   Set up TestHarness
      // ================================================
      // The HarnessBinders control generation of hardware in the TestHarness
      new chipyard.harness.WithUARTAdapter ++
        /** add UART adapter to display UART on stdout, if uart is present */
        new chipyard.harness.WithBlackBoxSimMem ++
        /** add SimDRAM DRAM model for axi4 backing memory, if axi4 mem is enabled */
        new chipyard.harness.WithSimTSIOverSerialTL ++
        /** add external serial-adapter and RAM */
        new chipyard.harness.WithSimJTAGDebug ++
        /** add SimJTAG if JTAG for debug exposed */
        new chipyard.harness.WithSimDMI ++
        /** add SimJTAG if DMI exposed */
        new chipyard.harness.WithGPIOPinsTiedOff ++
        /** tie-off chiptop GPIO-pins, if GPIO-punchthrough is used */
        new chipyard.harness.WithGPIOTiedOff ++
        /** tie-off chiptop GPIOs, if GPIOs are present */
        new chipyard.harness.WithSimSPIFlashModel ++
        /** add simulated SPI flash memory, if SPI is enabled */
        new chipyard.harness.WithSimAXIMMIO ++
        /** add SimAXIMem for axi4 mmio port, if enabled */
        new chipyard.harness.WithTieOffInterrupts ++
        /** tie-off interrupt ports, if present */
        new chipyard.harness.WithTieOffL2FBusAXI ++
        /** tie-off external AXI4 master, if present */
        new chipyard.harness.WithCustomBootPinPlusArg ++
        /** drive custom-boot pin with a plusarg, if custom-boot-pin is present */
        new chipyard.harness.WithDriveChipIdPin ++
        /** drive chip id pin from harness binder, if chip id pin is present */
        new chipyard.harness.WithSimUARTToUARTTSI ++
        /** connect a SimUART to the UART-TSI port */
        new chipyard.harness.WithClockFromHarness ++
        /** all Clock I/O in ChipTop should be driven by harnessClockInstantiator */
        new chipyard.harness.WithResetFromHarness ++
        /** reset controlled by harness */
        new chipyard.harness.WithAbsoluteFreqHarnessClockInstantiator ++
        /** generate clocks in harness with unsynthesizable ClockSourceAtFreqMHz */

        // ================================================
        //   Set up I/O cells + punch I/Os in ChipTop
        // ================================================
        // The IOBinders instantiate ChipTop IOs to match desired digital IOs
        // IOCells are generated for "Chip-like" IOs
        new chipyard.iobinders.WithSerialTLIOCells ++
        new chipyard.iobinders.WithDebugIOCells ++
        new chipyard.iobinders.WithUARTIOCells ++
        new chipyard.iobinders.WithGPIOCells ++
        new chipyard.iobinders.WithSPIFlashIOCells ++
        new chipyard.iobinders.WithExtInterruptIOCells ++
        new chipyard.iobinders.WithChipIdIOCells ++
        new chipyard.iobinders.WithCustomBootPin ++
        // The "punchthrough" IOBInders below don't generate IOCells, as these interfaces shouldn't really be mapped to ASIC IO
        // Instead, they directly pass through the DigitalTop ports to ports in the ChipTop
        new chipyard.iobinders.WithI2CPunchthrough ++
        new chipyard.iobinders.WithSPIIOPunchthrough ++
        new chipyard.iobinders.WithAXI4MemPunchthrough ++
        new chipyard.iobinders.WithAXI4MMIOPunchthrough ++
        new chipyard.iobinders.WithTLMemPunchthrough ++
        new chipyard.iobinders.WithL2FBusAXI4Punchthrough ++
        new chipyard.iobinders.WithBlockDeviceIOPunchthrough ++
        new chipyard.iobinders.WithNICIOPunchthrough ++
        new chipyard.iobinders.WithTraceIOPunchthrough ++
        new chipyard.iobinders.WithUARTTSIPunchthrough ++
        new chipyard.iobinders.WithGCDBusyPunchthrough ++
        new chipyard.iobinders.WithNMITiedOff ++

        // ================================================
        //   Set up External Memory and IO Devices
        // ================================================
        // External memory section
        new testchipip.serdes.WithSerialTL(
          Seq(
            /** add a serial-tilelink interface */
            testchipip.serdes.SerialTLParams(
              client    = Some(
                testchipip.serdes.SerialTLClientParams(totalIdBits = 4)
              ), // serial-tilelink interface will master the FBUS, and support 4 idBits
              phyParams = testchipip.serdes.ExternalSyncSerialPhyParams(
                phitWidth = 32,
                flitWidth = 32,
              ), // serial-tilelink interface with 32 lanes
            )
          )
        ) ++
        new freechips.rocketchip.subsystem.WithNMemoryChannels(1) ++
        /** Default 1 AXI-4 memory channels */
        new freechips.rocketchip.subsystem.WithNoMMIOPort ++
        /** no top-level MMIO master port (overrides default set in rocketchip) */
        new freechips.rocketchip.subsystem.WithNoSlavePort ++
        /** no top-level MMIO slave port (overrides default set in rocketchip) */

        // MMIO device section
        new chipyard.config.WithUART ++
        /** add a UART */

        // ================================================
        //   Set up Debug/Bringup/Testing Features
        // ================================================
        // JTAG
        new freechips.rocketchip.subsystem.WithDebugSBA ++
        /** enable the SBA (system-bus-access) feature of the debug module */
        new chipyard.config.WithDebugModuleAbstractDataWords(8) ++
        /** increase debug module data word capacity */
        new freechips.rocketchip.subsystem.WithJtagDTM ++
        /** set the debug module to expose a JTAG port */

        // Boot Select Pins
        new testchipip.boot.WithCustomBootPin ++
        /** add a custom-boot-pin to support pin-driven boot address */
        new testchipip.boot.WithBootAddrReg ++
        /** add a boot-addr-reg for configurable boot address */

        // ================================================
        //   Set up Interrupts
        // ================================================
        // CLINT and PLIC related settings goes here
        new freechips.rocketchip.subsystem.WithNExtTopInterrupts(0) ++
        /** no external interrupts */

        // ================================================
        //   Set up Tiles
        // ================================================
        // tile-local settings goes here

        // ================================================
        //   Set up Memory system
        // ================================================
        // On-chip memory section
        new freechips.rocketchip.subsystem.WithDTS("ucb-bar,chipyard", Nil) ++
        /** custom device name for DTS (embedded in BootROM) */
        new chipyard.config.WithBootROM ++
        /** use default bootrom */
        new testchipip.soc.WithSbusScratchpad(
          base     = 0x40000000L,
          size     = sys.env.get("SCRATCHPAD_SIZE").map(v => BigInt(v)).getOrElse(BigInt(8 * 1024 * 1024)),
          banks    = sys.env.get("NUM_BANKS").map(_.toInt).getOrElse(8),
          subBanks = sys.env.get("NUM_SUBBANKS").map(_.toInt).getOrElse(1),
        ) ++

        // Coherency settings
        new freechips.rocketchip.subsystem.WithInclusiveCache ++
        /** use Sifive LLC cache as root of coherence */

        // Bus/interconnect settings
        new freechips.rocketchip.subsystem.WithCoherentBusTopology ++
        /** hierarchical buses including sbus/mbus/pbus/fbus/cbus/l2 */

        // ================================================
        //   Set up power, reset and clocking
        // ================================================

        // ChipTop clock IO/PLL/Divider/Mux settings
        new chipyard.clocking.WithClockTapIOCells ++
        /** Default generate a clock tapio */
        new chipyard.clocking.WithPassthroughClockGenerator ++

        // DigitalTop-internal clocking settings
        new freechips.rocketchip.subsystem.WithDontDriveBusClocksFromSBus ++
        /** leave the bus clocks undriven by sbus */
        new freechips.rocketchip.subsystem.WithClockGateModel ++
        /** add default EICG_wrapper clock gate model */
        new chipyard.clocking.WithClockGroupsCombinedByName(
          (
            "uncore",
            /** create a "uncore" clock group tieing all the bus clocks together */
            Seq("sbus", "mbus", "pbus", "fbus", "cbus", "obus", "implicit", "clock_tap"),
            Seq("tile"),
          )
        ) ++

        new chipyard.config.WithPeripheryBusFrequency(500.0) ++
        /** Default 500 MHz pbus */
        new chipyard.config.WithMemoryBusFrequency(500.0) ++
        /** Default 500 MHz mbus */
        new chipyard.config.WithControlBusFrequency(500.0) ++
        /** Default 500 MHz cbus */
        new chipyard.config.WithSystemBusFrequency(500.0) ++
        /** Default 500 MHz sbus */
        new chipyard.config.WithFrontBusFrequency(500.0) ++
        /** Default 500 MHz fbus */
        new chipyard.config.WithOffchipBusFrequency(500.0) ++
        /** Default 500 MHz obus */
        new chipyard.config.WithInheritBusFrequencyAssignments ++
        /** Unspecified clocks within a bus will receive the bus frequency if set */
        new chipyard.config.WithNoSubsystemClockIO ++
        /** drive the subsystem diplomatic clocks from ChipTop instead of using implicit clocks */

        // reset

        // power

        // ==================================
        //   Base Settings
        // ==================================
        new freechips.rocketchip.system.BaseConfig
        /** "base" rocketchip system */
    )
