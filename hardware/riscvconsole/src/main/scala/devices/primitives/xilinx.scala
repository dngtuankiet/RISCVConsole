package riscvconsole.devices.primitives

import chisel3._
import chisel3.util._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.diplomaticobjectmodel.DiplomaticObjectModelAddressing
import freechips.rocketchip.diplomaticobjectmodel.logicaltree._
import freechips.rocketchip.diplomaticobjectmodel.model._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.prci._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tilelink._

class xilinx_nand() extends Module {
  val io = IO(new Bundle{
    val i0 = Input(Bool())
    val i1 = Input(Bool())
    val out = Output(Bool())
  })
  val inst = Module(new xilinx_primitive_nand())
  inst.io.i0 := io.i0
  inst.io.i1 := io.i1
  io.out := inst.io.out
}

class xilinx_not() extends Module {
  val io = IO(new Bundle{
    val i0 = Input(Bool())
    val out = Output(Bool())
  })
  val inst = Module(new xilinx_primitive_not())
  inst.io.i0 := io.i0
  io.out := inst.io.out
}

class xilinx_primitive_nand() extends BlackBox with HasBlackBoxInline{
  val io = IO(new Bundle(){
    //Inputs
    val i0 = Input(Bool())
    val i1 = Input(Bool())
    //Output
    val out = Output(Bool())
  })

  /*
  * NAND truth table
  * out - i0 i1
  * 1      0  0
  * 1      0  1
  * 1      1  0
  * 0      1  1
  * The config for INIT is 0x07
  * */

  setInline("xilinx_primitive_nand.v",
  s"""(*don_touch = "true"*) module xilinx_primitive_nand(
     |  input i0,
     |  input i1,
     |  output out
     |  );
     |  LUT6 #(
     |      .INIT(64'h0000000000000007)  // Specify LUT Contents
     |  ) LUT6_inst (
     |     .O(out),   // LUT general output
     |     .I0(i0), // LUT input
     |     .I1(i1), // LUT input
     |     .I2(0), // LUT input
     |     .I3(0), // LUT input
     |     .I4(0), // LUT input
     |     .I5(0)  // LUT input
     |  );
     |""".stripMargin
  )
}

class xilinx_primitive_not() extends BlackBox with HasBlackBoxInline{
  val io = IO(new Bundle(){
    //Inputs
    val i0 = Input(Bool())
    //Output
    val out = Output(Bool())
  })

  /*
  * NAND truth table
  * out - i0
  * 1      0
  * 0      1
  * The config for INIT is 0x01
  * */

  setInline("xilinx_primitive_nand.v",
    s"""(*don_touch = "true"*) module xilinx_primitive_nand(
       |  input i0,
       |  output out
       |  );
       |  LUT6 #(
       |      .INIT(64'h0000000000000001)  // Specify LUT Contents
       |  ) LUT6_inst (
       |     .O(out),   // LUT general output
       |     .I0(i0), // LUT input
       |     .I1(0), // LUT input
       |     .I2(0), // LUT input
       |     .I3(0), // LUT input
       |     .I4(0), // LUT input
       |     .I5(0)  // LUT input
       |  );
       |""".stripMargin
  )
}
