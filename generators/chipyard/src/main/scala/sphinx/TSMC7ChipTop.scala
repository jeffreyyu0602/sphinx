package chipyard.sphinx

import chisel3._
import chipyard.iobinders._

import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy.InModuleBody
import freechips.rocketchip.subsystem.{HasTileLinkLocations, PBUS}
import freechips.rocketchip.subsystem.BaseSubsystem
import chipyard._
import chipyard.harness.BuildTop
import sifive.blocks.devices.uart._
import chisel3.experimental.noPrefix

class TSMC7ChipTop(
  implicit p: Parameters)
    extends ChipTop {
  // making the module name ChipTop instead of CustomChipTop means
  // we don't have to set the TOP make variable to CustomChipTop
  override lazy val desiredName = "ChipTop"

  InModuleBody {
    lazySystem match {
      case lsys: DigitalTop =>
        val driverIO = lsys.gpioDriverIO.getOrElse {
          require(false, "TSMC7 IO cells require the GPIO Driver to be enabled in Config!")
          throw new Exception("Missing GPIO Driver") // Stop compiler here
        }

        // -------------------------------
        // Instantiate RTE cells for sides
        // -------------------------------
        val sides = Seq("WEST", "NORTH", "EAST", "SOUTH")

        val domainRteMap = sides.zipWithIndex.map { case (side, idx) =>
          noPrefix {
            val rteCell = Module(new PRTEDISABLE)
            rteCell.suggestName(s"iocell_dmn${idx}_rte")

            val rteSignal = side match {
              case "NORTH" | "SOUTH" =>
                val hCell = Module(new TSMC7POCMCellH)
                hCell.suggestName(s"iocell_dmn${idx}_pocm")
                hCell.io.rte := rteCell.io.RTE
                rteCell.io.RTE
              case "EAST" | "WEST"   =>
                val vCell = Module(new TSMC7POCMCellV)
                vCell.suggestName(s"iocell_dmn${idx}_pocm")
                vCell.io.rte := rteCell.io.RTE
                rteCell.io.RTE
            }

            side -> rteSignal
          }
        }.toMap

        // -------------------------------
        // Name → Side mapping
        // -------------------------------
        val nameToSide: Map[String, String] = Map(
          "iocell_jtag_TDO"  -> "NORTH",
          "iocell_jtag_TDI"  -> "NORTH",
          "iocell_jtag_TMS"  -> "NORTH",
          "iocell_jtag_TCK"  -> "NORTH",
          "iocell_clock"     -> "SOUTH",
          "iocell_reset"     -> "SOUTH",
          "iocell_clock_tap" -> "SOUTH",
        )

        iocellMap.foreach {
          case (interface, cells) => {
            cells.foreach {
              _ match {
                case c: HasCellNameAndRTE => {
                  val name = c.cellName.getOrElse("unknown")
                  val side = nameToSide.getOrElse(name, "SOUTH") // Default to SOUTH if not found
                  println(s"[DEBUG] IOCell = ${name} mapped to side = ${side}")

                  val rte = domainRteMap(side)

                  c.io.ds  := driverIO.ds
                  c.io.rte := rte
                }
                case other                => {
                  require(false, s"Unsupported iocell type ${other.getClass}")
                }
              }
            }
          }
        }
    }
  }
}

class WithTSMC7IOCells extends Config((site, here, up) => { case IOCellKey => TSMC7IOCellParams() })

class WithTSMC7ChipTop extends Config((site, here, up) => { case BuildTop => (p: Parameters) => new TSMC7ChipTop()(p) })
