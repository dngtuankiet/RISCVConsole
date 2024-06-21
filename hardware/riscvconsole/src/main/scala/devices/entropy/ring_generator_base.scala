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

case class baseLocHint(
    x1: Int = 3,
    y1: Int = 118,
    x2: Int = 7,
    y2: Int = 124,
    // loc_x: Int = 2,
    // loc_y: Int = 122
    loc_x: Int = 30,
    loc_y: Int = 149
)

class RingGeneratorBase(val size: Int = 16, val poly: Seq[Int], val src: Seq[Int], val entropy: Seq[Int], val locHint: baseLocHint = baseLocHint()) extends Module{
    val io = IO(new Bundle{
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

    val xor_poly = VecInit.tabulate(poly.length){ i =>
        val xp = Module(new xilinx_xor())
        xp.suggestName(s"xp_${i}")
        xp.io    
    }

    val xor_etp = VecInit.tabulate(entropy.length){ i =>
        val xe = Module(new xilinx_xor())
        xe.suggestName(s"xe_${i}")
        xe.io    
    }

    var poly_count = 0
    var etp_count = 0

    val state = RegInit(VecInit(Seq.fill(size)(0.U(1.W))))

    val entropies = entropy.zip(io.iEntropy).toMap
    val taps = poly.zip(src)
    //Connect inputs to XOR gate
    (0 until size).reverse.foreach {i =>
        entropies.get(i) match{
            case Some(etp) => {
                xor_etp(etp_count).i0 := state(i)
                xor_etp(etp_count).i1 := etp
                etp_count += 1
                // state(i-1) := xor_etp(etp_count).out
            }
            case None =>
        }

        taps.find(tap => tap._1 + tap._2 == i) match {
            case Some(tap) => {
                xor_poly(poly_count).i0 := state(tap._2)
                xor_poly(poly_count).i1 := state(i)
                poly_count += 1
                // state(i-1) := state(tap._2) ^ state(i)
            }
            case None =>
        }
    }


    //Connect output of XOR to the ring
    poly_count = 0
    etp_count = 0
    withReset(io.iRst){
        when(io.iEn) {
            (1 until size).reverse.foreach { i =>
                entropies.get(i) match {
                    case Some(etp) => {
                        state(i-1) := xor_etp(etp_count).out
                        etp_count += 1
                    }
                    case None => state(i-1) := state(i)
                }

                taps.find(tap => tap._1 + tap._2 == i) match {
                    case Some(tap) => {
                        state(i-1) := xor_poly(poly_count).out
                        poly_count += 1
                    }
                    case None =>
                }
            }

            // Inject state from last register
            state(size-1) := Mux(io.iInit, io.iBit, state(0))
        }

        io.oState := state.asUInt
        io.oSerial := state(0)
    }




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
        "ring_generator_base" + ".vivado.xdc",
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
            
            var ept_offset = (entropy.length/4).toInt + 1
            val pblock =
                s"""create_pblock ring_generator_base
                |add_cells_to_pblock [get_pblocks ring_generator_base] [get_cells ${xdcPath}*]
                |add_cells_to_pblock [get_pblocks ring_generator_base] [get_cells ${xdcPath}]
                |resize_pblock [get_pblocks ring_generator_base] -add {SLICE_X${locHint.loc_x}Y${locHint.loc_y}:SLICE_X${locHint.loc_x+3}Y${locHint.loc_y-poly_offset-ept_offset}}
                |set_property IS_SOFT FALSE [get_pblocks ring_generator_base]
                |set_property EXCLUDE_PLACEMENT TRUE [get_pblocks ring_generator_base]
                |""".stripMargin

            pblock //xor_poly + xor_etp + variations
        }
    )//ElaborationArtefacts
}