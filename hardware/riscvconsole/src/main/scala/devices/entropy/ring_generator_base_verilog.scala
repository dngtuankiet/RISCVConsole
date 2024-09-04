package riscvconsole.devices.entropy

import chipsalliance.rocketchip.config.{Field, Parameters}
import chisel3._
import chisel3.util._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.diplomaticobjectmodel.model._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.ElaborationArtefacts

// case class baseLocHint(
//     loc_x: Int = 0,
//     loc_y: Int = 121
// )

class RingGeneratorBaseVerilog_IO() extends Bundle {
    val iClk = Input(Clock())
    val iRst = Input(Bool())
    val iEn = Input(Bool())
    val iInit = Input(Bool())
    val iEntropy = Input(UInt(24.W))
    val iChallenge = Input(UInt(32.W))
    val oState = Output(UInt(32.W))
    val oSerial = Output(Bool())
}

class RingGeneratorBaseVerilog(useXDC: Boolean=false, val locHint: baseLocHint=baseLocHint(), val instName: String) extends BlackBox with HasBlackBoxResource {
    val io = IO(new RingGeneratorBaseVerilog_IO())

    addResource("./entropy/ring_generator_base.v")
    if(useXDC){
        ElaborationArtefacts.add(
            instName + ".vivado.xdc",
            {
                val xdcPath = pathName.split("\\.").drop(1).mkString("/")+"/"
                println(s"Test xpr_slice in ${pathName} <> ${xdcPath}")

                val pblock =
                s"""create_pblock ring_generator_base
                |add_cells_to_pblock [get_pblocks ring_generator_base] [get_cells ${xdcPath}*]
                |add_cells_to_pblock [get_pblocks ring_generator_base] [get_cells ${xdcPath}]
                |resize_pblock [get_pblocks ring_generator_base] -add {SLICE_X${locHint.loc_x}Y${locHint.loc_y}:SLICE_X${locHint.loc_x+3}Y${locHint.loc_y-8}}
                |set_property IS_SOFT FALSE [get_pblocks ring_generator_base]
                |set_property EXCLUDE_PLACEMENT TRUE [get_pblocks ring_generator_base]
                |""".stripMargin

                pblock
            }
        )
    }

}
