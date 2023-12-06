package riscvconsole.devices.custom

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

// hardware wrapper for Verilog file: module name & ports MUST MATCH the Verilog's

//class TRNG extends BlackBox with HasBlackBoxResource {
//  val io = IO(new Bundle{
//    val iClk   = Input(Clock())
//    val iRst   = Input(Bool())
//    val iA     = Input(UInt(16.W))
//    val iB     = Input(UInt(16.W))
//    val iValid = Input(Bool())
//    val oReady = Output(Bool())
//    val oValid = Output(Bool())
//    val oC     = Output(UInt(16.W))
//  })
//  addResource("GCD.v")
//}

class TRNG extends Module {
  val io = IO(new Bundle {
    val iClk   = Input(Clock())
    val iRst   = Input(Bool())
    val iDelay  = Input(UInt(16.W))
    val oReady = Output(Bool())
    val oValid = Output(Bool())
    val oRand     = Output(UInt(32.W))
  })


  /* TODO: implement the core here */

}


class RingGenerator(flipFlops: Int, polynomial: String) extends Module {
  val io = IO(new Bundle {
    val out = Output(UInt(flipFlops.W))
  })

  val stateReg = RegInit(1.U(flipFlops.W))

  // Function to compute the feedback value based on the polynomial
  def computeFeedback(state: UInt): UInt = {
    val feedback = (0 until polynomial.length).map(i => {
      val bit = state(i)
      val polyBit = polynomial(i).asDigit.U
      bit & polyBit
    }).reduce(_ ^ _)

    feedback
  }

  // Update the state register with the feedback value
  val feedback = computeFeedback(stateReg)
  stateReg := Cat(feedback, stateReg(flipFlops - 1, 1))

  io.out := stateReg
}

//object RingGeneratorMain extends App {
//  val flipFlops = 8 // Number of flip-flops
//  val polynomial = "100011101" // Polynomial function x^8 + x^4 + x^3 + x^2 + 1
//
//  chisel3.Driver.execute(args, () => new RingGenerator(flipFlops, polynomial))
//}


// declare params
case class TRNGParams(address: BigInt)

// declare register-map structure
object TRNGCtrlRegs {
  val control     = 0x00
  val status      = 0x04
  val delay        = 0x08
  val random      = 0x0C
}

// mapping between HW ports and register-map
abstract class TRNGmod(busWidthBytes: Int, c: TRNGParams)(implicit p: Parameters)
  extends RegisterRouter(
    RegisterRouterParams(
      name = "trng",
      compat = Seq("console,trng0"),
      base = c.address,
      beatBytes = busWidthBytes))
{
  lazy val module = new LazyModuleImp(this) {
    // HW instantiation
    val mod = Module(new TRNG)

    // declare inputs
//    val data_a = Reg(UInt(16.W))
//    val data_b = Reg(UInt(16.W))
    val rst    = RegInit(false.B)
    val trig   = WireInit(false.B)
    // mapping inputs
    mod.io.iClk   := clock
    mod.io.iRst   := reset.asBool || rst
    mod.io.iValid := trig
    mod.io.iA     := data_a
    mod.io.iB     := data_b

    // declare outputs
    val ready  = Wire(Bool())
    val valid  = Wire(Bool())
    val data_c = Wire(UInt(16.W))
    // mapping outputs
    ready  := mod.io.oReady
    valid  := mod.io.oValid
    data_c := RegEnable(mod.io.oC, valid)

    // map inputs & outputs to register positions
    val mapping = Seq(
      TRNGCtrlRegs.trigger -> Seq(
        RegField(1, trig, RegFieldDesc("trigger", "TRNG trigger/start")),
        RegField(7),
        RegField(1, rst, RegFieldDesc("rst", "GCD Reset", reset = Some(0)))
      ),
      TRNGCtrlRegs.data_a -> Seq(RegField(16, data_a, RegFieldDesc("data_a", "A data for GCD"))),
      TRNGCtrlRegs.data_b -> Seq(RegField(16, data_b, RegFieldDesc("data_b", "B data for GCD"))),
      TRNGCtrlRegs.data_c -> Seq(RegField.r(16, data_c, RegFieldDesc("data_c", "C output for GCD", volatile = true))),
      TRNGCtrlRegs.status -> Seq(RegField.r(1, ready, RegFieldDesc("ready", "GCD data ready", volatile = true))),
    )
    regmap(mapping :_*)
    val omRegMap = OMRegister.convert(mapping:_*)
  }
}

// declare TileLink-wrapper class for TRNG-module
class TLTRNG(busWidthBytes: Int, params: TRNGParams)(implicit p: Parameters)
  extends TRNGmod(busWidthBytes, params) with HasTLControlRegMap

// this will auto +1 ID if there are many GCD modules
object TRNGID {
  val nextId = {
    var i = -1; () => {
      i += 1; i
    }
  }
}

// attach TLTRNG to a bus
case class TRNGAttachParams
(
  device: TRNGParams,
  controlWhere: TLBusWrapperLocation = PBUS)
{
  def attachTo(where: Attachable)(implicit p: Parameters): TLTRNG = where {
    val name = s"trng_${TRNGID.nextId()}"
    val cbus = where.locateTLBusWrapper(controlWhere)
    val gcd = LazyModule(new TLTRNG(cbus.beatBytes, device))
    gcd.suggestName(name)

    cbus.coupleTo(s"device_named_$name") { bus =>
      (gcd.controlXing(NoCrossing)
        := TLFragmenter(cbus)
        := bus )
    }
    gcd
  }
}

// declare trait to be called in a system
case object PeripheryTRNGKey extends Field[Seq[TRNGParams]](Nil)

// trait to be called in a system
trait HasPeripheryTRNG { this: BaseSubsystem =>
  val trngNodes = p(PeripheryTRNGKey).map { ps =>
    TRNGAttachParams(ps).attachTo(this)
  }
}
