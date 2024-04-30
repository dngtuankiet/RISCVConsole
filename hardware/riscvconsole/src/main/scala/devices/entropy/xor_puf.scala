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

//hardware wrapper for Verilog file: module name & ports MUST MATCH the Verilog module

class XPRCELL_IO() extends Bundle {
    val i_trigger = Input(Bool())
    val i_i0 = Input(Bool())
    val i_i1 = Input(Bool())
    val out = Output(Bool())
}

// trait hasXPRCELL_IO{
//     this: Module =>
//     val io = IO(new XPRCELL_IO())
// }

class xpr_cell(useXDC: Boolean = false) extends BlackBox with HasBlackBoxResource {
    val io = IO(new XPRCELL_IO())
    addResource("./entropy/xpr_cell.v")
    if(useXDC){
        ElaborationArtefacts.add(
            "xpr_cell" + ".vivado.xdc",
            {
                val xdcPath = pathName.split("\\.").drop(1).mkString("/")+"/"
                println(s"Test xpr_cell in ${pathName} <> ${xdcPath}")

                val lockPin =
                    s"""
                    |set_property LOCK_PINS "I0:A4" [get_cells LUT5_L_and_top]
                    |set_property LOCK_PINS "I0:A4" [get_cells LUT5_L_and_bot]
                    """.stripMargin

                val placement =
                    s"""
                    |set_property BEL C5LUT [get_cells ${xdcPath}LUT5_L_and_top]
                    |set_property LOC SLICE_X0Y4 [get_cells ${xdcPath}LUT5_L_and_top]
                    |set_property BEL C6LUT [get_cells ${xdcPath}LUT5_L_xor_top]
                    |set_property LOC SLICE_X0Y4 [get_cells ${xdcPath}LUT5_L_xor_top]
                    |
                    |set_property BEL B5LUT [get_cells ${xdcPath}LUT5_L_and_bot]
                    |set_property LOC SLICE_X0Y4 [get_cells ${xdcPath}LUT5_L_and_bot]
                    |set_property BEL B6LUT [get_cells ${xdcPath}LUT5_L_xor_bot]
                    |set_property LOC SLICE_X0Y4 [get_cells ${xdcPath}LUT5_L_xor_bot]
                    """.stripMargin

                val loop =
                    s"""
                    |set_property ALLOW_COMBINATORIAL_LOOPS true [get_nets ${xdcPath}top_xor]
                    |set_property ALLOW_COMBINATORIAL_LOOPS true [get_nets ${xdcPath}bot_xor]
                    |set_property ALLOW_COMBINATORIAL_LOOPS true [get_cells ${xdcPath}LUT5_L_xor_top]
                    |set_property ALLOW_COMBINATORIAL_LOOPS true [get_cells ${xdcPath}LUT5_L_and_top]
                    |set_property ALLOW_COMBINATORIAL_LOOPS true [get_cells ${xdcPath}LUT5_L_xor_bot]
                    |set_property ALLOW_COMBINATORIAL_LOOPS true [get_cells ${xdcPath}LUT5_L_and_bot]
                    |""".stripMargin
                val clock =
                    s"""
                    |create_clock -period 10.000 -name puf_out -waveform {0.000 5.000} -add [get_ports ${xdcPath}out]
                    """.stripMargin

                placement + lockPin + loop
            }
        )//ElaborationArtefacts
    }
}

class xor_puf(implicit p: Parameters) extends Module {
    val io = IO(new Bundle {
        //for xpr_cell
        val iTrigger = Input(Bool())
        val iI0 = Input(Bool())
        val iI1 = Input(Bool())
        //for counter
        val iEn = Input(Bool())
        val oCount = Output(UInt(32.W))
    })

    //Connect xpr_cell
    val xpr = Module(new xpr_cell(true))
    val xpr_bit = WireDefault(false.B)
    xpr_bit := xpr.io.out
    xpr.io.i_i0 := io.iI0
    xpr.io.i_i1 := io.iI1
    xpr.io.i_trigger := io.iTrigger

    //Use xpr_bit as clock for counter
    withClock(xpr_bit.asClock()) {
        val counter = RegInit(0.U(32.W))
        when(io.iEn) {
            counter := counter + 1.U
        }
        io.oCount := counter
    }
    
}


//Mapping the module to register control
case class XORPUFParams(address: BigInt)

// Register layout
object XORPUFCtrlRegs {
    val trigger = 0x00
    val i0 = 0x04
    val i1 = 0x08
    val enable = 0x0C
    val count = 0x10
}

//Register mapping
abstract class XORPUFmod(busWidthBytes: Int, c: XORPUFParams)(implicit p: Parameters)
extends RegisterRouter(
    RegisterRouterParams(
        name = "xorpuf",
        compat = Seq("console,xorpuf0"),
        base = c.address,
        beatBytes = busWidthBytes))
{
    lazy val module = new LazyModuleImp(this){
        //Instantiate
        val mod = Module(new xor_puf())

        //Declare registers
        val trigger = RegInit(false.B)
        val i0 = RegInit(false.B)
        val i1 = RegInit(false.B)
        val enable = RegInit(false.B)
        val count = Wire(UInt(32.W))
        // val trigger = RegField(Bool(), RegFieldDesc(name = "trigger", desc = "Trigger for XORPUF"))
        // val i0 = RegField(Bool(), RegFieldDesc(name = "i0", desc = "Input 0 for XORPUF"))
        // val i1 = RegField(Bool(), RegFieldDesc(name = "i1", desc = "Input 1 for XORPUF"))
        // val enable = RegField(Bool(), RegFieldDesc(name = "enable", desc = "Enable for counter"))
        // val count = RegField(UInt(32.W), RegFieldDesc(name = "count", desc = "Counter value"))

        //mapping inputs
        mod.io.iTrigger := trigger
        mod.io.iI0 := i0
        mod.io.iI1 := i1
        mod.io.iEn := enable
        //mapping outputs
        count := mod.io.oCount

        val mapping = Seq(
            XORPUFCtrlRegs.trigger -> Seq(
                RegField(1, trigger, RegFieldDesc(name = "trigger", desc = "Trigger for XORPUF"))
            ),
            XORPUFCtrlRegs.i0 -> Seq(
                RegField(1, i0, RegFieldDesc(name = "i0", desc = "Input 0 for XORPUF"))
            ),
            XORPUFCtrlRegs.i1 -> Seq(
                RegField(1, i1, RegFieldDesc(name = "i1", desc = "Input 1 for XORPUF"))
            ),
            XORPUFCtrlRegs.enable -> Seq(
                RegField(1, enable, RegFieldDesc(name = "enable", desc = "Enable for counter"))
            ),
            XORPUFCtrlRegs.count -> Seq(
                RegField.r(32, count, RegFieldDesc(name = "count", desc = "Counter value"))
            )
        )

        regmap(mapping:_*)
        val omRegMap = OMRegister.convert(mapping:_*)
    }
}

//Declare TileLink-wrapper for XORPUF module
class TLXORPUF(busWidthBytes: Int, c: XORPUFParams)(implicit p: Parameters)
extends XORPUFmod(busWidthBytes, c) with HasTLControlRegMap

//auto +1 ID if there are many XORPUF module
object XORPUFID{
    val nextID = {var i = -1; () => {i += 1; i}}
}

//attach TLXORPUF to TL bus
case class TLXORPUFAttachParams(
    device: XORPUFParams,
    controlWhere: TLBusWrapperLocation = PBUS)
{
    def attachTo(where: Attachable)(implicit p: Parameters): TLXORPUF = where {
        val name = s"xorpuf_${XORPUFID.nextID()}"
        val cbus = where.locateTLBusWrapper(controlWhere)
        val xorpuf = LazyModule(new TLXORPUF(cbus.beatBytes, device))
        xorpuf.suggestName(name)

        cbus.coupleTo(s"device_named_$name") { bus =>
            (xorpuf.controlXing(NoCrossing)
            := TLFragmenter(cbus)
                := bus)
            }
        xorpuf
    }
}

//declare trait to be called in the subsystem
case object PeripheryXORPUFKey extends Field[Seq[XORPUFParams]](Nil)

//trait to be called in the subsystem
trait HasPeripheryXORPUF { this: BaseSubsystem =>
    val xorpufNodes = p(PeripheryXORPUFKey).map { ps =>
        TLXORPUFAttachParams(ps).attachTo(this)
    }
}