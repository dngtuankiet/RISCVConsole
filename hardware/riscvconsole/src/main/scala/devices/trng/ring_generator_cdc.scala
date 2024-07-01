package riscvconsole.devices.trng

import chipsalliance.rocketchip.config.{Field, Parameters}
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
import freechips.rocketchip.util.ElaborationArtefacts
import riscvconsole.devices.primitives._

// case class rgLocHints(
//  loc_x: Int = 30,
//  loc_y: Int = 149
// )

class RingGeneratorCDC(val stage: Int = 16, val InjectNum: Int = 7, val injectList: List[Int], val feedbackSrc: List[Int], val feedbackDst: List[Int], val useXDC: Boolean = false, val hints: rgLocHints = rgLocHints()) extends Module {
  val io = IO(new Bundle {
    val iClk0 = Input(Clock())
    val iClk1 = Input(Clock())
    val iClk2 = Input(Clock())
    val iRst = Input(Bool())
    val i_inject = Input(UInt(InjectNum.W))
    val i_en     = Input(Bool())
    val o_bit1    = Output(UInt(1.W))
    val o_bit2   = Output(UInt(1.W))
  })
  val w_state_0 = WireDefault(0.U(1.W))
  val w_state_1 = WireDefault(0.U(1.W))
  val w_state_2 = WireDefault(0.U(1.W))
  val w_state_3 = WireDefault(0.U(1.W))
  val w_state_4 = WireDefault(0.U(1.W))
  val w_state_5 = WireDefault(0.U(1.W))
  val w_state_6 = WireDefault(0.U(1.W))
  val w_state_7 = WireDefault(0.U(1.W))
  val w_state_8 = WireDefault(0.U(1.W))
  val w_state_9 = WireDefault(0.U(1.W))
  val w_state_10 = WireDefault(0.U(1.W))
  val w_state_11 = WireDefault(0.U(1.W))
  val w_state_12 = WireDefault(0.U(1.W))
  val w_state_13 = WireDefault(0.U(1.W))
  val w_state_14 = WireDefault(0.U(1.W))
  val w_state_15 = WireDefault(0.U(1.W))

  withClockAndReset(io.iClk0, reset){
    val r_state_6 = RegInit(0.U(1.W))
    val r_state_7 = RegInit(0.U(1.W))
    val r_state_8 = RegInit(0.U(1.W))
    val r_state_9 = RegInit(0.U(1.W))

    w_state_6 := r_state_6
    w_state_7 := r_state_7
    w_state_8 := r_state_8
    w_state_9 := r_state_9

    when(io.iRst){
        r_state_6 := 0.U
        r_state_7 := 0.U
        r_state_8 := 0.U
        r_state_9 := 0.U
    }.otherwise{
        when(io.i_en){
            r_state_6 := w_state_9 ^ w_state_5
            r_state_7 := w_state_6 ^ io.i_inject(6)
            r_state_8 := w_state_7
            r_state_9 := w_state_8 ^ io.i_inject(5)
        }
    }
  }

  withClockAndReset(io.iClk1, reset){
    val r_state_5 = RegInit(0.U(1.W))
    val r_state_10 = RegInit(0.U(1.W))
    val r_state_11 = RegInit(0.U(1.W))

    w_state_5 := r_state_5
    w_state_10 := r_state_10
    w_state_11 := r_state_11

    when(io.iRst){
        r_state_5 := 0.U
        r_state_10 := 0.U
        r_state_11 := 0.U
    }.otherwise{
        when(io.i_en){
            r_state_5 := w_state_11 ^ w_state_4
            r_state_10 := w_state_9
            r_state_11 := w_state_10 ^ io.i_inject(3)
        }
    }
  }

  withClockAndReset(io.iClk2, reset){
    val r_state_3 = RegInit(0.U(1.W))
    val r_state_4 = RegInit(0.U(1.W))
    val r_state_12 = RegInit(0.U(1.W))

    w_state_3 := r_state_3
    w_state_4 := r_state_4
    w_state_12 := r_state_12

    when(io.iRst){
        r_state_3 := 0.U
        r_state_4 := 0.U
        r_state_12 := 0.U
    }.otherwise{
        when(io.i_en){
            r_state_3 := w_state_12 ^ w_state_2
            r_state_4 := w_state_3 ^ io.i_inject(4)
            r_state_12 := w_state_11
        }
    }
  }

  val r_state_0= RegInit(0.U(1.W))
  val r_state_1 = RegInit(0.U(1.W))
  val r_state_2 = RegInit(0.U(1.W))
  val r_state_13= RegInit(0.U(1.W))
  val r_state_14 = RegInit(0.U(1.W))
  val r_state_15 = RegInit(0.U(1.W))

  w_state_0 := r_state_0
  w_state_1 := r_state_1
  w_state_2 := r_state_2
  w_state_13 := r_state_13
  w_state_14 := r_state_14
  w_state_15 := r_state_15

    when(io.iRst){
        r_state_0 := 0.U
        r_state_1 := 0.U
        r_state_2 := 0.U
        r_state_13 := 0.U
        r_state_14 := 0.U
        r_state_15 := 0.U
    }.otherwise{
        when(io.i_en){
            r_state_0 := w_state_15
            r_state_1 := w_state_0 ^ io.i_inject(2)
            r_state_2 := w_state_1 ^ io.i_inject(1)
            r_state_13 := w_state_12
            r_state_14 := w_state_13 ^ io.i_inject(0)
            r_state_15 := w_state_14
        }
    }
    io.o_bit1 := w_state_15
    io.o_bit2 := w_state_7

//   //default value
//   val stateReg    = RegInit(VecInit(Seq.fill(stage)(0.U(1.W)))) // Creating a 32-bit register as a vector of 1-bit elements
//   var injectNext   = 0;
//   var feedbackNext = 0;

//   stateReg := stateReg
//   when(io.i_en){
//     stateReg(1) := stateReg(0)
//     stateReg(0) := stateReg(stage-1)
//     for (i <- 1 until (stage - 1)) {
//       if ((injectNext < injectList.length) && (i == injectList(injectNext))) {
//         stateReg(i + 1) := stateReg(i) ^ io.i_inject(injectNext)
//         injectNext += 1
//       } else if ((feedbackNext < feedbackDst.length) && (i == feedbackDst(feedbackNext))) {
//         stateReg(i + 1) := stateReg(i) ^ stateReg(feedbackSrc(feedbackNext))
//         feedbackNext += 1
//       } else {
//         stateReg(i + 1) := stateReg(i)
//       }
//     }
//   }.otherwise{
//     for (i <- 0 until stage) {
//       stateReg(i) := stateReg(i)
//     }
//   }

//   io.o_bit1 := stateReg(stage-1)
//   io.o_bit2 := stateReg((stage/2)-1)
//  when(io.i_en){
//    io.o_bit := stateReg(31)
//  }.otherwise(
//    io.o_bit := 0.U
//  )


  var FDRE_name = List("D5FF", "DFF","C5FF", "CFF","B5FF", "BFF","A5FF", "AFF")

  /* Create constraints based on location hint */
//  Test Ring Generator ref in ArtyA7Top.platform.trng_0.mod.rg <> platform/trng_0/mod/rg/
  if (useXDC) {
    ElaborationArtefacts.add(
      "ring_generator" + ".vivado.xdc",
      {
        val xdcPath = pathName.split("\\.").drop(1).mkString("/")
        // println(s"Test Ring Generator ref in ${pathName} <> ${xdcPath}")
        // println(s"stateReg pathName: ${stateReg.pathName}")
        // val variations =(for (i <- 0 until (stage/8)) yield {
        //     (for(j <- 0 until 8) yield{
        //       s"""set_property LOC SLICE_X${hints.loc_x}Y${hints.loc_y-i} [get_cells ${xdcPath}/stateReg_${i*8+j}_reg]
        //          |set_property DONT_TOUCH true [get_cells ${xdcPath}/stateReg_${i*8+j}_reg]
        //          |set_property BEL ${FDRE_name(j)} [get_cells ${xdcPath}/stateReg_${i*8+j}_reg]
        //          |""".stripMargin
        //     }).reduce(_+_)
        // }).reduce(_+_)

        // val extra =
        //   s"""set_property ALLOW_COMBINATORIAL_LOOPS true [get_nets ${xdcPath}/stateReg_fdre_${stage-1}/inst/FDRE_inst/Q]
        //      |set_property SEVERITY {Warning} [get_drc_checks LUTLP-1]
        //      |set_property SEVERITY {Warning} [get_drc_checks NSTD-1]
        //      |""".stripMargin

        val pblock =
            s"""create_pblock ring_generator_base
            |add_cells_to_pblock [get_pblocks ring_generator_base] [get_cells ${xdcPath}*]
            |add_cells_to_pblock [get_pblocks ring_generator_base] [get_cells ${xdcPath}]
            |resize_pblock [get_pblocks ring_generator_base] -add {SLICE_X${hints.loc_x}Y${hints.loc_y}:SLICE_X${hints.loc_x+5}Y${hints.loc_y-5}}
            |set_property IS_SOFT FALSE [get_pblocks ring_generator_base]
            |set_property EXCLUDE_PLACEMENT TRUE [get_pblocks ring_generator_base]
            |""".stripMargin

        // variations+extra
        pblock
      }
    ) // ElaborationArtefacts
  }
}