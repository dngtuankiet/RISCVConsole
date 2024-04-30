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

case class sliceLocHint(
    x: Int = 0,
    y: Int = 121
)


class XPRSLICE_IO() extends Bundle {
    val iR = Input(Bool())
    val i1 = Input(Bool())
    val i2 = Input(Bool())
    val out1 = Output(Bool())
    val out2 = Output(Bool())
}


class XPRSlice(useXDC: Boolean = false, val locHint: sliceLocHint = sliceLocHint(), val instName: String) extends BlackBox with HasBlackBoxResource {
    val io = IO(new Bundle{
        val iR = Input(Bool())
        val i1 = Input(Bool())
        val i2 = Input(Bool())
        val out1 = Output(Bool())
        val out2 = Output(Bool())
    })

    addResource("./entropy/xpr_slice.v")
    if(useXDC){
        ElaborationArtefacts.add(
            instName + ".vivado.xdc",
            {
                val xdcPath = pathName.split("\\.").drop(1).mkString("/")+"/"
                println(s"Test xpr_slice in ${pathName} <> ${xdcPath}")

                val placement =
                    s"""
                    |set_property BEL D5LUT [get_cells ${xdcPath}LUT5_DA_and_top]
                    |set_property LOC SLICE_X${locHint.x}Y${locHint.y} [get_cells ${xdcPath}LUT5_DA_and_top]
                    |
                    |set_property BEL D6LUT [get_cells ${xdcPath}LUT5_DA_xor_top]
                    |set_property LOC SLICE_X${locHint.x}Y${locHint.y} [get_cells ${xdcPath}LUT5_DA_xor_top]
                    |
                    |set_property BEL A6LUT [get_cells ${xdcPath}LUT5_DA_xor_bot]
                    |set_property LOC SLICE_X${locHint.x}Y${locHint.y} [get_cells ${xdcPath}LUT5_DA_xor_bot]
                    |
                    |set_property BEL A5LUT [get_cells ${xdcPath}LUT5_DA_and_bot]
                    |set_property LOC SLICE_X${locHint.x}Y${locHint.y} [get_cells ${xdcPath}LUT5_DA_and_bot]
                    |
                    |
                    |set_property BEL C5LUT [get_cells ${xdcPath}LUT5_CB_and_top]
                    |set_property LOC SLICE_X${locHint.x}Y${locHint.y} [get_cells ${xdcPath}LUT5_CB_and_top]
                    |
                    |set_property BEL C6LUT [get_cells ${xdcPath}LUT5_CB_xor_top]
                    |set_property LOC SLICE_X${locHint.x}Y${locHint.y} [get_cells ${xdcPath}LUT5_CB_xor_top]
                    |
                    |set_property BEL B6LUT [get_cells ${xdcPath}LUT5_CB_xor_bot]
                    |set_property LOC SLICE_X${locHint.x}Y${locHint.y} [get_cells ${xdcPath}LUT5_CB_xor_bot]
                    |
                    |set_property BEL B5LUT [get_cells ${xdcPath}LUT5_CB_and_bot]
                    |set_property LOC SLICE_X${locHint.x}Y${locHint.y} [get_cells ${xdcPath}LUT5_CB_and_bot]
                    |""".stripMargin

                val loop =
                    s"""
                    |set_property ALLOW_COMBINATORIAL_LOOPS TRUE [get_nets ${xdcPath}DA_and_top]
                    |set_property ALLOW_COMBINATORIAL_LOOPS TRUE [get_nets ${xdcPath}DA_and_bot]
                    |set_property ALLOW_COMBINATORIAL_LOOPS TRUE [get_nets ${xdcPath}CB_and_top]
                    |set_property ALLOW_COMBINATORIAL_LOOPS TRUE [get_nets ${xdcPath}CB_and_bot]
                    |""".stripMargin

                val lockPin =
                    s"""
                    |set_property LOCK_PINS "I3:A4" [get_cells ${xdcPath}LUT5_DA_xor_top]
                    |set_property LOCK_PINS "I3:A4" [get_cells ${xdcPath}LUT5_DA_xor_bot]
                    |set_property LOCK_PINS "I3:A5" [get_cells ${xdcPath}LUT5_CB_xor_top]
                    |set_property LOCK_PINS "I3:A5" [get_cells ${xdcPath}LUT5_CB_xor_bot]
                    |""".stripMargin

                val buffer =
                    s"""
                    |set_property CLOCK_BUFFER_TYPE none [get_nets ${xdcPath}out1]
                    |set_property CLOCK_BUFFER_TYPE none [get_nets ${xdcPath}out2]
                    """.stripMargin

                placement +  loop + lockPin + buffer
            }
        )//ElaborationArtefacts
    }
}