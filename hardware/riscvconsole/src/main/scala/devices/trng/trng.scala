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
  val iDelay = Input(UInt(16.W))
  val oReady = Output(Bool())
  val oValid = Output(Bool())
  val oRand = Output(UInt(32.W))
}

trait hasTRNGIO {
  this: Module =>
  val io = IO(new TRNGIO())
}

class TRNG extends Module with hasTRNGIO {

  /* TODO: implement the core here */
  var injectList = List(16, 19, 22, 25, 28)
  var feedbackSrc = List(29, 26, 23, 20, 18)
  var feedbackDst = List(2, 5, 7, 10, 13)

  val rg = Module(new RingGenerator(32, 5, injectList, feedbackSrc, feedbackDst, true))
  val ro = Module(new RingOscillator(4, true))

  //Default
  val rg_inject = WireDefault(0.U(5.W))
  val rg_enable = WireDefault(0.U(1.W))
  val rg_bit    = WireDefault(0.U(1.W))

  val ro_enable = WireDefault(0.U(1.W))
  val ro_out    = WireDefault(0.U(5.W))

  rg_inject := ro_out
  rg_enable := io.iEn
  rg_bit := rg.io.o_bit

  ro_enable := io.iEn
  ro_out := ro.io.o_out

  /* Pins */
  rg.io.i_inject := rg_inject
  rg.io.i_en := rg_enable

  ro.io.i_en := rg_enable

  //delay counter - initial wait time for calibration
  val tick = RegInit(0.U(1.W))
  val delayCnt = RegInit(0.U(16.W))
  when(io.iRst === 0.U){
    delayCnt := 0.U
  }.elsewhen(tick.asBool){
    delayCnt := 0.U
  }.elsewhen(io.iEn){
    delayCnt := delayCnt + 1.U
  }.otherwise{
    delayCnt := delayCnt
  }
  tick := Mux((delayCnt === io.iDelay) & (tick =/= 1.U), 1.U, 0.U)

  //32 counter - only counts when delay is finished
  val collectCnt = RegInit(0.U(5.W))
  when(io.iRst === 0.U) {
    collectCnt := 0.U
  }.elsewhen(tick.asBool) {
    collectCnt := collectCnt + 1.U
  }.otherwise {
    collectCnt := collectCnt
  }
  val overflow = (collectCnt === 32.U)
  io.oValid := overflow

  //serial to parallel, with enable
  val outReg = RegInit(0.U(32.W))
  when(io.iEn){
    outReg := rg_bit ## outReg(31, 1)
  }
  io.oRand := outReg

  //output
  io.oReady <> DontCare
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
    val enable = RegInit(false.B) //enable signal for RO
    val delay  = RegInit(0.U(32.W))
    // mapping inputs
    mod.io.iClk   := clock
    mod.io.iRst   := reset.asBool || rst
    mod.io.iEn    := enable
    mod.io.iDelay := delay

    // declare outputs
    val ready  = Wire(Bool())
    val valid  = Wire(Bool())
    val rand = Wire(UInt(16.W))
    // mapping outputs
    ready  := mod.io.oReady
    valid  := mod.io.oValid
    rand := RegEnable(mod.io.oRand, valid)

    // map inputs & outputs to register positions
    val mapping = Seq(
      TRNGCtrlRegs.control -> Seq(
        RegField(1, enable, RegFieldDesc("trigger", "TRNG trigger/start")),
        RegField(7),
        RegField(1, rst, RegFieldDesc("rst", "TRNG Reset", reset = Some(0)))
      ),
      TRNGCtrlRegs.status -> Seq(
        RegField.r(1, ready, RegFieldDesc("ready", "TRNG ready", volatile = true)),
        RegField(7),
        RegField.r(1, valid, RegFieldDesc("valid", "TRNG data valid", volatile = true)),
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
