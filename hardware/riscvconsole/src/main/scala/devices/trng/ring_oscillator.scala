package riscvconsole.devices.trng

import chisel3._
import chisel3.util._
import freechips.rocketchip.util._
import riscvconsole.devices.primitives._

case class roLocHints(
  loc_x: Int = 29,
  loc_y: Int = 149
)

class RingOscillator(val stage: Int = 4 /*number of inverters*/, val useXDC: Boolean = false, val hints : roLocHints = roLocHints()) extends Module {
  val io = IO(new Bundle{
    val i_en  = Input(Bool())
    val o_out = Output(UInt((stage+1).W)) //output signal at each stage
  })

  val out = Wire(Vec(stage+1, Bool()))

  /* Generate elements */
  /* This code essentially creates a vector "ro_invs" containting "stage" instances of the "xilinx_not" module
  * and provides access to their IO ports for further connections or usage */

  val ro_invs = VecInit.tabulate(stage) { i =>
    val m = Module(new xilinx_not())
    m.suggestName(s"not_gate_${i}")
    m.io
  }

  val ro_nand = VecInit.tabulate(1){ i =>
    val m = Module(new xilinx_nand())
    m.suggestName((s"nand_gate_${i}"))
    m.io
  }

  /* Structure of the ring osc */
  for(i <- 0 until (stage-1)){
    if(i==0){
      ro_nand(i).i1 := ro_invs(stage-1).out
      ro_nand(i).i0 := io.i_en.asUInt
      ro_invs(i).i0 := ro_nand(i).out
    }
    ro_invs(i+1).i0 := ro_invs(i).out
  }

  out(0) := ro_nand(0).out
  for(i <- 0 until stage){
    out(i+1) := ro_invs(i).out
  }
  io.o_out := out.asUInt

  /* Create constraints based on location hint */
//  Test RO ref in ArtyA7Top.platform.trng_0.mod.ro <> platform/trng_0/mod/ro/
//  platform/trng_0/mod/ro/ro_invs_not_gate_3/inst/LUT6_inst
//  platform/trng_0/mod/ro/ro_nand_nand_gate_0/inst/LUT6_inst
  if (useXDC) {
    ElaborationArtefacts.add(
      "ring_oscillator" + ".vivado.xdc",
      {
        val xdcPath = pathName.split("\\.").drop(1).mkString("/") + "/"
        println(s"Test Ring Oscillator ref in ${pathName} <> ${xdcPath}")
        val nand_xdc =
          s"""set_property LOC SLICE_X${hints.loc_x}Y${hints.loc_y} [get_cells ${xdcPath}ro_nand_nand_gate_0/inst/LUT6_inst]
             |set_property DONT_TOUCH true [get_cells ${xdcPath}ro_nand_nand_gate_0/inst/LUT6_inst]
             |set_property BEL D6LUT [get_cells ${xdcPath}ro_nand_nand_gate_0/inst/LUT6_inst]
             |""".stripMargin

        val not_xdc = (for(i <- 0 until stage) yield {
          s"""
             |set_property LOC SLICE_X${hints.loc_x}Y${hints.loc_y - i - 1} [get_cells ${xdcPath}ro_invs_not_gate_${i}/inst/LUT6_inst]
             |set_property DONT_TOUCH true [get_cells ${xdcPath}ro_invs_not_gate_${i}/inst/LUT6_inst]
             |set_property BEL D6LUT [get_cells ${xdcPath}ro_invs_not_gate_${i}/inst/LUT6_inst]
             |""".stripMargin
        }).reduce(_+_)

        val extra =
          s"""create_clock -name clk_ro -period 10 [get_pins ${xdcPath}ro_invs_not_gate_${stage-1}/inst/LUT6_inst/O]
             |set_property SEVERITY {Warning} [get_drc_checks LUTLP-1]
             |""".stripMargin

        nand_xdc + not_xdc + extra
      }
    ) // ElaborationArtefacts
  }
}