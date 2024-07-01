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
import riscvconsole.devices.primitives._

// case class baseLocHint(
//     x1: Int = 3,
//     y1: Int = 118,
//     x2: Int = 7,
//     y2: Int = 124,
//     // loc_x: Int = 2,
//     // loc_y: Int = 122
//     loc_x: Int = 30,
//     loc_y: Int = 149
// )

class RingGeneratorBaseCDC(val size: Int = 16, val poly: Seq[Int], val src: Seq[Int], val entropy: Seq[Int], val locHint: baseLocHint = baseLocHint()) extends Module{
    val io = IO(new Bundle{
        val iClk0 = Input(Clock())
        val iClk1 = Input(Clock())
        val iClk2 = Input(Clock())
        val iClk3 = Input(Clock())
        //control
        val iRst = Input(Bool())
        val iEn = Input(Bool())
        val iInit = Input(Bool())
        //random
        val iEntropy = Input(Vec(entropy.size, Bool()))
        //data
        val iBit = Input(Bool())
        val oState = Output(UInt(size.W))
        val oSerial = Output(Bool())
    })

    val w_3_to_2 = WireDefault(0.U(1.W))
    val w_4_to_3 = WireDefault(0.U(1.W))
    val w_6_to_5 = WireDefault(0.U(1.W))
    
    val w_state_10 = WireDefault(0.U(1.W))
    val w_state_11 = WireDefault(0.U(1.W))
    val w_state_13 = WireDefault(0.U(1.W))
    val w_state_0 = WireDefault(0.U(1.W))

    

    withClockAndReset(clock, reset){
        val r_state_0 = RegInit(0.U(1.W))
        val r_state_1 = RegInit(0.U(1.W))
        val r_state_2 = RegInit(0.U(1.W))
        val r_state_13 = RegInit(0.U(1.W))
        val r_state_14 = RegInit(0.U(1.W))
        val r_state_15 = RegInit(0.U(1.W))
        w_state_0 := r_state_0
        w_state_13 := r_state_13

        when(io.iRst){
            r_state_0 := 0.U
            r_state_1 := 0.U
            r_state_2 := 0.U
            r_state_13 := 0.U
            r_state_14 := 0.U
            r_state_15 := 0.U
        }.otherwise{
            when(io.iEn){
                r_state_0 := r_state_1
                r_state_1 := r_state_2
                r_state_2 := w_3_to_2

                r_state_15 := Mux(io.iInit, io.iBit, r_state_0)
                r_state_14 := r_state_15 ^ io.iEntropy(0)
                r_state_13 := r_state_14
            }
        }
       
    }

    withClockAndReset(clock, reset){
        val r_state_3 = RegInit(0.U(1.W))
        val r_state_11 = RegInit(0.U(1.W))
        val r_state_12 = RegInit(0.U(1.W))
        w_3_to_2 := r_state_3
        w_state_11 := r_state_11

        when(io.iRst){
            r_state_3 := 0.U
            r_state_11 := 0.U
            r_state_12 := 0.U
        }.otherwise{
            when(io.iEn){
                r_state_3 := w_4_to_3

                r_state_12 := r_state_3 ^ w_state_13
                r_state_11 := r_state_12 ^ io.iEntropy(1)
            }
        }
    }

    withClockAndReset(clock, reset){
        val r_state_4 = RegInit(0.U(1.W))
        val r_state_5 = RegInit(0.U(1.W))
        val r_state_10 = RegInit(0.U(1.W))
        w_state_10 := r_state_10
        w_4_to_3 := r_state_4

        when(io.iRst){
            r_state_4 := 0.U
            r_state_5 := 0.U
            r_state_10 := 0.U
        }.otherwise{
            when(io.iEn){
                r_state_4 := r_state_5 ^ io.iEntropy(2)
                r_state_5 := w_6_to_5
                
                r_state_10 := r_state_4 ^ w_state_11
            }
        }
    }

    withClockAndReset(clock, reset){
        val r_state_6 = RegInit(0.U(1.W))
        val r_state_7 = RegInit(0.U(1.W))
        val r_state_8 = RegInit(0.U(1.W))
        val r_state_9 = RegInit(0.U(1.W))
        w_6_to_5 := r_state_6

        when(io.iRst){
            r_state_6 := 0.U
            r_state_7 := 0.U
            r_state_8 := 0.U
            r_state_9 := 0.U
        }.otherwise{
            when(io.iEn){
                r_state_6 := r_state_7 ^ io.iEntropy(3)
                r_state_7 := r_state_8

                r_state_8 := r_state_9
                r_state_9 := r_state_6 ^ w_state_10
            }
        }
        
    }

    io.oSerial := w_state_0
    // io.oState := Cat(r_state_0, r_state_1, r_state_2, r_state_3, r_state_4, r_state_5, r_state_6, r_state_7, r_state_8,
    //                  r_state_9, r_state_10, r_state_11, r_state_12, r_state_13, r_state_14, r_state_15)
    io.oState := 0.U
    
    
    
    
    
    
    


    // val xor_poly = VecInit.tabulate(poly.length){ i =>
    //     val xp = Module(new xilinx_xor())
    //     xp.suggestName(s"xp_${i}")
    //     xp.io    
    // }

    // val xor_etp = VecInit.tabulate(entropy.length){ i =>
    //     val xe = Module(new xilinx_xor())
    //     xe.suggestName(s"xe_${i}")
    //     xe.io    
    // }

    // var poly_count = 0
    // var etp_count = 0

    // val state = RegInit(VecInit(Seq.fill(size)(0.U(1.W))))

    // val entropies = entropy.zip(io.iEntropy).toMap
    // val taps = poly.zip(src)
    // //Connect inputs to XOR gate
    // (0 until size).reverse.foreach {i =>
    //     entropies.get(i) match{
    //         case Some(etp) => {
    //             xor_etp(etp_count).i0 := state(i)
    //             xor_etp(etp_count).i1 := etp
    //             etp_count += 1
    //             // state(i-1) := xor_etp(etp_count).out
    //         }
    //         case None =>
    //     }

    //     taps.find(tap => tap._1 + tap._2 == i) match {
    //         case Some(tap) => {
    //             xor_poly(poly_count).i0 := state(tap._2)
    //             xor_poly(poly_count).i1 := state(i)
    //             poly_count += 1
    //             // state(i-1) := state(tap._2) ^ state(i)
    //         }
    //         case None =>
    //     }
    // }


    // //Connect output of XOR to the ring
    // poly_count = 0
    // etp_count = 0
    // withReset(io.iRst){
    //     when(io.iEn) {
    //         (1 until size).reverse.foreach { i =>
    //             entropies.get(i) match {
    //                 case Some(etp) => {
    //                     state(i-1) := xor_etp(etp_count).out
    //                     etp_count += 1
    //                 }
    //                 case None => state(i-1) := state(i)
    //             }

    //             taps.find(tap => tap._1 + tap._2 == i) match {
    //                 case Some(tap) => {
    //                     state(i-1) := xor_poly(poly_count).out
    //                     poly_count += 1
    //                 }
    //                 case None =>
    //             }
    //         }

    //         // Inject state from last register
    //         state(size-1) := Mux(io.iInit, io.iBit, state(0))
    //     }

    //     io.oState := state.asUInt
    //     io.oSerial := state(0)
    // }




    // withReset(io.iRst){
    //     val state = RegInit(VecInit(Seq.fill(size)(0.U(1.W))))

    //     when(io.iEn) {
    //         // Top path
    //         val entropies = entropy.zip(io.iEntropy).toMap
    //         (0 until size/2).reverse.foreach { i =>
    //             entropies.get(i+1) match {
    //                 case Some(etp) => state(i) := state(i+1) ^ etp
    //                 case None => state(i) := state(i+1)
    //             }
    //         }

    //         // Bottom path
    //         val taps = poly.zip(src)
    //         (size/2 until size).reverse.foreach { i =>
    //             taps.find(tap => tap._1 + tap._2 == i) match {
    //                 case Some(tap) => state(i-1) := state(tap._2) ^ state(i)
    //                 case None => state(i-1) := state(i)
    //             }
    //             entropies.get(i) match {
    //                 case Some(etp) => state(i-1) := state(i) ^ etp
    //                 case None =>
    //             }
    //         }

    //         // Inject state from last register
    //         state(size-1) := Mux(io.iInit, io.iBit, state(0))
    //     }
    // }
    
    // io.oState := state.asUInt
    // io.oSerial := state(0)

    var FDRE_name = List("D5FF", "DFF","C5FF", "CFF","B5FF", "BFF","A5FF", "AFF")
    var LUT_name = List("D6LUT", "C6LUT", "B6LUT", "A6LUT")

    ElaborationArtefacts.add(
        "ring_generator_base_cdc" + ".vivado.xdc",
        {
            val xdcPath = pathName.split("\\.").drop(1).mkString("/")+"/"
            println(s"Test ring gennerator base in ${pathName} <> ${xdcPath}")

            val variations =(for (i <- 0 until (size/8)) yield {
            (for(j <- 0 until 8) yield{
              s"""set_property LOC SLICE_X${locHint.loc_x}Y${locHint.loc_y-i} [get_cells ${xdcPath}state_${i*8+j}_reg]
                 |set_property DONT_TOUCH true [get_cells ${xdcPath}state_${i*8+j}_reg]
                 |set_property BEL ${FDRE_name(j)} [get_cells ${xdcPath}state_${i*8+j}_reg]
                 |""".stripMargin
                }).reduce(_+_)
            }).reduce(_+_)

            var poly_offset = (poly.length/4).toInt + 1
            val xor_poly = poly.zipWithIndex.map { case (_, i) =>
            //   val index = i
              val LUT_index = i % 4
              val LUT_inst = s"${xdcPath}xor_poly_xp_${i}/inst/LUT6_inst"
              s"""set_property LOC SLICE_X${locHint.loc_x+1}Y${locHint.loc_y-i/4} [get_cells $LUT_inst]
                 |set_property DONT_TOUCH true [get_cells $LUT_inst]
                 |set_property BEL ${LUT_name(LUT_index)} [get_cells $LUT_inst]
                 |""".stripMargin
            }.mkString("\n")

            val xor_etp = entropy.zipWithIndex.map { case (_, i) =>
            //   val index = i
              val LUT_index = i % 4
              val LUT_inst = s"${xdcPath}xor_etp_xe_${i}/inst/LUT6_inst"
              s"""set_property LOC SLICE_X${locHint.loc_x+1}Y${locHint.loc_y-i/4-poly_offset} [get_cells $LUT_inst]
                 |set_property DONT_TOUCH true [get_cells $LUT_inst]
                 |set_property BEL ${LUT_name(LUT_index)} [get_cells $LUT_inst]
                 |""".stripMargin
            }.mkString("\n")

            // // var LUT_sel = 0
            // val xor_etp = (for (i <- 0 until entropy.length) yield {
            //   s"""set_property LOC SLICE_X${locHint.loc_x+1}Y${locHint.loc_y-i-poly_offset} [get_cells ${xdcPath}xor_etp_xe_${i}/inst/LUT6_inst]
            //      |set_property DONT_TOUCH true [get_cells ${xdcPath}xor_etp_xe_${i}/inst/LUT6_inst]
            //      |set_property BEL ${LUT_name(LUT_sel)} [get_cells ${xdcPath}xor_etp_xe_${i}/inst/LUT6_inst]
            //      |""".stripMargin
            //     LUT_sel += 1
            //     if (LUT_sel == 4) {
            //         LUT_sel = 0
            //     }
            // }).reduce(_+_)

            val pblock =
                s"""create_pblock ring_generator_base
                |add_cells_to_pblock [get_pblocks ring_generator_base] [get_cells ${xdcPath}*]
                |add_cells_to_pblock [get_pblocks ring_generator_base] [get_cells ${xdcPath}]
                |resize_pblock [get_pblocks ring_generator_base] -add {SLICE_X${locHint.loc_x}Y${locHint.loc_y}:SLICE_X${locHint.loc_x+5}Y${locHint.loc_y-5}}
                |set_property IS_SOFT FALSE [get_pblocks ring_generator_base]
                |set_property EXCLUDE_PLACEMENT TRUE [get_pblocks ring_generator_base]
                |""".stripMargin

            // xor_poly + xor_etp + variations
            pblock
        }
    )//ElaborationArtefacts
}