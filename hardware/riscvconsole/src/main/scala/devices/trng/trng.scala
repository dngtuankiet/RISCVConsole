package riscvconsole.devices.trng

import chipsalliance.rocketchip.config.{Field, Parameters}
import chisel3._
import chisel3.util._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.diplomaticobjectmodel.model._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tilelink._

// hardware wrapper for Verilog file: module name & ports MUST MATCH the Verilog's

class TRNGIO() extends Bundle {
  val iClk = Input(Clock())
  val iRst = Input(Bool())
  val iEn = Input(Bool())
  val iDelay = Input(UInt(32.W))
  val iNext = Input(Bool())
  val oValid = Output(Bool())
  val oRand = Output(UInt(32.W))
}

trait hasTRNGIO {
  this: Module =>
  val io = IO(new TRNGIO())
}

class TRNG extends Module with hasTRNGIO {

  /* TODO: implement the core here */
  // var injectList = List(16, 19, 22, 25, 28)
  // var feedbackSrc = List(29, 26, 23, 20, 18)
  // var feedbackDst = List(2, 5, 7, 10, 13)

  // val rg = Module(new RingGenerator(32, 5, injectList, feedbackSrc, feedbackDst, true))
  // val ro = Module(new RingOscillator(4, true))


  val injectList = List(8,10,13)
  val feedbackSrc = List(12,11,9)
  val feedbackDst = List(2,4,5)

  val rg = Module(new RingGenerator(16, 3, injectList, feedbackSrc, feedbackDst, true))
  val ro = Module(new RingOscillator(2, true))

  //Default
  val rg_bit    = WireDefault(0.U(1.W))
  rg_bit := rg.io.o_bit
  
  /* Pins */
  rg.io.i_inject := ro.io.o_out
  rg.io.i_en := io.iEn
  ro.io.i_en := io.iEn

  //delay counter - initial wait time for calibration
  val tick = RegInit(false.B)
  val delayCnt = RegInit(0.U(32.W))

  when(io.iRst){
    delayCnt := 0.U
    tick := false.B
  }.otherwise{
    when(io.iEn && !tick) {
      delayCnt := delayCnt + 1.U
      when(delayCnt === io.iDelay) {
        tick := true.B
      }.otherwise{
        tick := tick
      }
    }.otherwise{
      delayCnt := delayCnt
      tick := tick
    }
  }

  val shiftReg = RegInit(0.U(32.W))
  val collectCnt = RegInit(0.U(5.W))
  val valid = RegInit(false.B)
  val ready = RegInit(true.B)

  def risingedge(x: Bool) = x && !RegNext(x)
  val nextTrigger = risingedge(io.iNext)

  when(io.iRst || nextTrigger){ //restart sampling when reset or ready to sample
    collectCnt := 0.U //reset counter when sampling is disable and restart another sampling
    valid := false.B // reset valid when not sampling
  }.otherwise{
    when(tick && !valid) {
      shiftReg := rg_bit ## shiftReg(31, 1)
      collectCnt := collectCnt + 1.U
      when(collectCnt === 31.U) {
        valid := true.B //valid read data
      }.otherwise{
        valid := valid
      }
    }.otherwise{
      collectCnt := collectCnt
    }
  }

  io.oRand := Mux(valid, shiftReg, 0.U)
  io.oValid := valid
}


// declare params
case class TRNGParams(address: BigInt)

// declare register-map structure
object TRNGCtrlRegs {
  val control     = 0x00
  val status      = 0x04
  val delay       = 0x08
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
    val rst    = RegInit(false.B)
    val enable = RegInit(false.B)
    val next   = RegInit(false.B)
    val delay  = RegInit(0.U(32.W))
    // mapping inputs
    mod.io.iClk   := clock
    mod.io.iRst   := reset.asBool || rst
    mod.io.iEn    := enable
    mod.io.iNext  := next
    mod.io.iDelay := delay

    // declare outputs
    val valid  = Wire(Bool())
    val rand = Wire(UInt(32.W))
    // mapping outputs
    valid  := mod.io.oValid
    // rand := RegEnable(mod.io.oRand, valid)
    rand := mod.io.oRand

    // map inputs & outputs to register positions
    val mapping = Seq(
      TRNGCtrlRegs.control -> Seq(
        RegField(1, enable, RegFieldDesc("trigger", "TRNG enable")),
        RegField(1, next, RegFieldDesc("trigger", "TRNG next")),
        RegField(6),
        RegField(1, rst, RegFieldDesc("rst", "TRNG reset", reset = Some(0)))
      ),
      TRNGCtrlRegs.status -> Seq(
        RegField.r(1, valid, RegFieldDesc("valid", "TRNG data valid", volatile = true))
      ),
      TRNGCtrlRegs.delay -> Seq(RegField(32, delay, RegFieldDesc("delay", "delay time for calibrartion TRNG"))),
      TRNGCtrlRegs.random -> Seq(RegField(32, rand, RegFieldDesc("random", "random output for TRNG", volatile = true))),
    )
    regmap(mapping :_*)
    val omRegMap = OMRegister.convert(mapping:_*)
  }
}

// declare TileLink-wrapper class for TRNG-module
class TLTRNG(busWidthBytes: Int, params: TRNGParams)(implicit p: Parameters)
  extends TRNGmod(busWidthBytes, params) with HasTLControlRegMap

// this will auto +1 ID if there are many TRNG modules
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
    val trng = LazyModule(new TLTRNG(cbus.beatBytes, device))
    trng.suggestName(name)

    cbus.coupleTo(s"device_named_$name") { bus =>
      (trng.controlXing(NoCrossing)
        := TLFragmenter(cbus)
        := bus )
    }
    trng
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
