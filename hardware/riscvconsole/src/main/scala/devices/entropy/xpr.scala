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
import riscvconsole.devices.entropy._

class XPR(val size: Int = 16, val xpr_slices_num: Int = 4) extends Module{
    val io = IO(new Bundle{
        //for XPRSlice
        val iR = Vec(xpr_slices_num, Input(Bool()))
        val i1 = Vec(xpr_slices_num, Input(Bool()))
        val i2 = Vec(xpr_slices_num, Input(Bool()))
        //for base
        val iRst = Input(Bool())
        val iEn = Input(Bool())
        // val iInit = Input(Bool()) //use seed or not
        // val iSeed = Input(UInt(size.W))
        val iDelay = Input(UInt(32.W)) //for stablizing
        //read out
        val iNext = Input(Bool())
        val oValid = Output(Bool())
        // val oReady = Output(Bool())
        val oValue = Output(UInt(32.W))
    })

    //-----Ring Generator Base polynomial-----
    // x^16 + x^10 + x^7 + x^4 + 1
    val poly = Seq(10,7,4)
    val src = Seq(3,4,6)

    //x^16 + x^13 + x^12 + x^9 + x^6 + x^3 + 1
    // val poly = Seq(13,12,9,6,3)
    // val src = Seq(2,2,3,5,6)

    //-----Config settings-----

    //Ring Generator base
    // val entropy = Seq(15,14,12,9,7,5,2,1) //Full entropy sources for poly x^16 + x^10 + x^7 + x^4 + 1
    // val entropy = Seq(13,10,7,4) //Full entropy sources for poly x^16 + x^13 + x^12 + x^9 + x^6 + x^3 + 1
    val entropy = Seq(15,12,7,5)

    val baseLocHint = new baseLocHint(loc_x = 30, loc_y = 149)

    //XPRSlice
    val x = 28
    val y = 147
    val sliceLocHints = Seq.tabulate(xpr_slices_num)(i => {
      val newX = if (i % 2 == 1) x + 1 else x
      val rows = i / 2
      val newY = y - rows
      new sliceLocHint(newX, newY)
    })


    //-----Instantiate modules-----
    //Ring Generator Base
    // val xpr_base_cdc = Module(new RingGeneratorBase(size, poly, src, entropy, baseLocHint))
    val xpr_base_cdc = Module(new RingGeneratorBaseCDC(size, poly, src, entropy, baseLocHint))

    //XPR
    val xpr_slice = Seq.tabulate(xpr_slices_num)(i =>
        Module(new XPRSlice(true, sliceLocHints(i), s"xpr_slice_$i")).suggestName(s"xpr_slice_$i")
    )

    //-----Connect modules-----
    //Ring Generator Base
    xpr_base_cdc.io.iRst := io.iRst
    xpr_base_cdc.io.iEn := io.iEn //TODO: use FSM to inject seed later
    xpr_base_cdc.io.iInit := false.B //TODO: use FSM to inject seed later
    xpr_base_cdc.io.iBit := false.B //TODO: use FSM to inject seed later

    xpr_base_cdc.io.iClk0 := xpr_slice(1).io.out1.asClock()
    xpr_base_cdc.io.iClk1 := xpr_slice(1).io.out2.asClock()
    xpr_base_cdc.io.iClk2 := xpr_slice(0).io.out1.asClock()
    xpr_base_cdc.io.iClk3 := xpr_slice(0).io.out2.asClock()  

    //XPR
    (0 until xpr_slices_num).foreach { i =>
        xpr_slice(i).io.iR := io.iR(i)
        xpr_slice(i).io.i1 := io.i1(i)
        xpr_slice(i).io.i2 := io.i2(i)
    }
  
    // auto route multiple ECs
    xpr_base_cdc.io.iEntropy.zip(xpr_slice.flatMap(slice => Seq(slice.io.out1, slice.io.out2))).foreach { case (input, output) =>
      input := output
    }

    //-----Control FSM-----
    //delay counter - initial wait time for calibration
    val tick = RegInit(false.B)
    val delayCnt = RegInit(0.U(32.W))

    when(io.iRst){
        delayCnt := 0.U
        tick := false.B
    }.elsewhen(io.iEn && !tick) {
        delayCnt := Mux(delayCnt === io.iDelay, 0.U, delayCnt + 1.U)
        tick := delayCnt === io.iDelay
    }

    //shift reg to collect outputs
    val shiftReg = RegInit(0.U(32.W))
    val collectCnt = RegInit(0.U(5.W))
    val valid = RegInit(false.B)
    // val ready = RegInit(true.B)

    def risingedge(x: Bool) = x && !RegNext(x)
    val nextTrigger = risingedge(io.iNext)

    when(io.iRst || nextTrigger){ //restart sampling when reset or ready to sample
        collectCnt := 0.U //reset counter when sampling is disable and restart another sampling
        valid := false.B // reset valid when not sampling
        shiftReg := 0.U
    }.otherwise{
        when(tick && !valid) {
            shiftReg := xpr_base_cdc.io.oSerial ## shiftReg(31, 1)
            collectCnt := collectCnt + 1.U
            valid := collectCnt === 31.U //valid read data when counter reaches 31
        }
    }

    io.oValue := Mux(valid, shiftReg, 0.U)
    io.oValid := valid

    //TODO:
    //Control FSM to initialize challenge and read outputs
}






case class XPRParams(address: BigInt)

// declare register-map structure
object XPRCtrlRegs {
  val control     = 0x00
  val status      = 0x04
  val delay       = 0x08
  val random      = 0x0C
  val ctrl_ir     = 0x10
  val ctrl_i1     = 0x14
  val ctrl_i2     = 0x18
}

// mapping between HW ports and register-map
abstract class XPRmod(busWidthBytes: Int, c: XPRParams)(implicit p: Parameters)
  extends RegisterRouter(
    RegisterRouterParams(
      name = "xpr",
      compat = Seq("console,xpr0"),
      base = c.address,
      beatBytes = busWidthBytes))
{
  lazy val module = new LazyModuleImp(this) {
    // HW instantiation
    val xpr_size = 16
    val xpr_slices = 2
    val mod = Module(new XPR(size = xpr_size, xpr_slices_num = xpr_slices))

    // declare inputs
    val ir     = VecInit(Seq.fill(xpr_slices)(RegInit(false.B)))
    val i1     = VecInit(Seq.fill(xpr_slices)(RegInit(false.B)))
    val i2     = VecInit(Seq.fill(xpr_slices)(RegInit(false.B)))
    val rst    = RegInit(false.B)
    val enable = RegInit(false.B)
    val next   = RegInit(false.B)
    val delay  = RegInit(0.U(32.W))
    // mapping inputs
    for (i <- 0 until xpr_slices) {
      mod.io.iR(i) := ir(i)
      mod.io.i1(i) := i1(i)
      mod.io.i2(i) := i2(i)
    }
    // mod.io.iR := ir
    // mod.io.i1 := i1
    // mod.io.i2 := i2
    mod.io.iRst   := reset.asBool || rst
    mod.io.iEn    := enable
    mod.io.iNext  := next
    mod.io.iDelay := delay

    // declare outputs
    val valid  = Wire(Bool())
    val value = Wire(UInt(32.W))
    // mapping outputs
    valid  := mod.io.oValid
    // rand := RegEnable(mod.io.oRand, valid)
    value := mod.io.oValue

    // map inputs & outputs to register positions
    val mapping = Seq(
      XPRCtrlRegs.control -> Seq(
      RegField(1, enable, RegFieldDesc("trigger", "XPR enable")),
      RegField(1, next, RegFieldDesc("trigger", "XPR next")),
      RegField(1),
      RegField(5),
      RegField(1, rst, RegFieldDesc("rst", "XPR reset", reset = Some(0)))
      ),
      XPRCtrlRegs.status -> Seq(
      RegField.r(1, valid, RegFieldDesc("valid", "XPR data valid", volatile = true))
      ),
      XPRCtrlRegs.delay -> Seq(RegField(32, delay, RegFieldDesc("delay", "delay time for calibrartion XPR"))),
      XPRCtrlRegs.random -> Seq(RegField.r(32, value, RegFieldDesc("output", "output for XPR", volatile = true))),
      XPRCtrlRegs.ctrl_ir -> Seq.tabulate(xpr_slices) { i =>
        RegField(1, ir(i), RegFieldDesc("ctrl_ir", s"control ir $i for XPR"))
      },
      XPRCtrlRegs.ctrl_i1 -> Seq.tabulate(xpr_slices) { i =>
        RegField(1, i1(i), RegFieldDesc("ctrl_i1", s"control i1 $i for XPR"))
      },
      XPRCtrlRegs.ctrl_i2 -> Seq.tabulate(xpr_slices) { i =>
        RegField(1, i2(i), RegFieldDesc("ctrl_i2", s"control i2 $i for XPR"))
      }
    )
    regmap(mapping :_*)
    val omRegMap = OMRegister.convert(mapping:_*)
  }
}

// declare TileLink-wrapper class for XPR-module
class TLXPR(busWidthBytes: Int, params: XPRParams)(implicit p: Parameters)
  extends XPRmod(busWidthBytes, params) with HasTLControlRegMap

// this will auto +1 ID if there are many XPR modules
object XPRID {
  val nextId = {
    var i = -1; () => {
      i += 1; i
    }
  }
}

// attach TLXPR to a bus
case class XPRAttachParams
(
  device: XPRParams,
  controlWhere: TLBusWrapperLocation = PBUS)
{
  def attachTo(where: Attachable)(implicit p: Parameters): TLXPR = where {
    val name = s"xpr_${XPRID.nextId()}"
    val cbus = where.locateTLBusWrapper(controlWhere)
    val xpr = LazyModule(new TLXPR(cbus.beatBytes, device))
    xpr.suggestName(name)

    cbus.coupleTo(s"device_named_$name") { bus =>
      (xpr.controlXing(NoCrossing)
        := TLFragmenter(cbus)
        := bus )
    }
    xpr
  }
}

// declare trait to be called in a system
case object PeripheryXPRKey extends Field[Seq[XPRParams]](Nil)

// trait to be called in a system
trait HasPeripheryXPR { this: BaseSubsystem =>
  val xprNodes = p(PeripheryXPRKey).map { ps =>
    XPRAttachParams(ps).attachTo(this)
  }
}
