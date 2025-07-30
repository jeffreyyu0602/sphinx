package chipyard.sphinx

import chisel3._
import chipyard.iobinders._

import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy.{InModuleBody}
import freechips.rocketchip.subsystem.{PBUS, HasTileLinkLocations}
import freechips.rocketchip.subsystem.BaseSubsystem
import chipyard._
import chipyard.harness.{BuildTop}
import sifive.blocks.devices.uart._


class TSMC7ChipTop(implicit p: Parameters) extends ChipTop {
  // making the module name ChipTop instead of CustomChipTop means
  // we don't have to set the TOP make variable to CustomChipTop
  override lazy val desiredName = "ChipTop"

  // InModuleBody blocks are executed within the LazyModuleImp of this block
  InModuleBody {
    lazySystem match { case lsys: DigitalTop =>
      iocellMap.foreach { case (interface, cells) => {
        val ds: Data = lsys.ioDriveStrength match {
          case Some(d) => d
          case None => {
            require(false, "TSMC7 IO cells require drive strength")
            false.B
          }
        }
        cells.foreach { _ match {
          case c: TSMC7AnalogIOCell => {
            // do nothing
          }
          case c: TSMC7DigitalGPIOCellH => {
            c.io.ds := ds
          }
          case c: TSMC7DigitalInIOCellH => {
            c.io.ds := ds
          }
          case c: TSMC7DigitalOutIOCellH => {
            c.io.ds := ds
          }
          case c: TSMC7DigitalGPIOCellV => {
            c.io.ds := ds
          }
          case c: TSMC7DigitalInIOCellV => {
            c.io.ds := ds
          }
          case c: TSMC7DigitalOutIOCellV => {
            c.io.ds := ds
          }
          case c => {
            require(false, s"Unsupported iocell type ${c.getClass}")
          }
        }}
      }}
    }
  }
}

class WithTSMC7IOCells extends Config((site, here, up) => {
  case IOCellKey => TSMC7IOCellParams()
})

class WithTSMC7ChipTop extends Config((site, here, up) => {
  case BuildTop => (p: Parameters) => new TSMC7ChipTop()(p)
})
