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

case class baseLocHint(
    x1: Int = 3,
    y1: Int = 118,
    x2: Int = 7,
    y2: Int = 124
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

    withReset(io.iRst){
        val state = RegInit(VecInit(Seq.fill(size)(0.U(1.W))))

        when(io.iEn) {
            // Top path
            val entropies = entropy.zip(io.iEntropy).toMap
            (0 until size/2).reverse.foreach { i =>
                entropies.get(i+1) match {
                    case Some(etp) => state(i) := state(i+1) ^ etp
                    case None => state(i) := state(i+1)
                }
            }

            // Bottom path
            val taps = poly.zip(src)
            (size/2 until size).reverse.foreach { i =>
                taps.find(tap => tap._1 + tap._2 == i) match {
                    case Some(tap) => state(i-1) := state(tap._2) ^ state(i)
                    case None => state(i-1) := state(i)
                }
                entropies.get(i) match {
                    case Some(etp) => state(i-1) := state(i) ^ etp
                    case None =>
                }
            }

            // Inject state from last register
            state(size-1) := Mux(io.iInit, io.iBit, state(0))
        }

        io.oState := state.asUInt
        io.oSerial := state(0)
    }
    

    ElaborationArtefacts.add(
        "ring_generator_base" + ".vivado.xdc",
        {
            val xdcPath = pathName.split("\\.").drop(1).mkString("/")+"/"
            println(s"Test ring gennerator base in ${pathName} <> ${xdcPath}")

            val pblock =
                s"""create_pblock ring_generator_base
                |add_cells_to_pblock [get_pblocks ring_generator_base] [get_cells ${xdcPath}*]
                |add_cells_to_pblock [get_pblocks ring_generator_base] [get_cells ${xdcPath}]
                |resize_pblock [get_pblocks ring_generator_base] -add {SLICE_X${locHint.x1}Y${locHint.y1}:SLICE_X${locHint.x2}Y${locHint.y2}}
                |set_property IS_SOFT FALSE [get_pblocks ring_generator_base]
                |""".stripMargin

            pblock
        }
    )//ElaborationArtefacts
}