package riscvconsole.devices.sdram

import chisel3._
import chisel3.experimental.{Analog, IntParam, StringParam, attach}
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.prci.{ClockGroup, ClockSinkDomain}
import freechips.rocketchip.subsystem.{Attachable, BaseSubsystem, MBUS}
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.HeterogeneousBag

case class sdram_bb_cfg
(
  SDRAM_MHZ: Int = 50,
  SDRAM_ADDR_W: Int = 24,
  SDRAM_COL_W: Int = 9,
  SDRAM_BANK_W: Int = 2,
  SDRAM_DQM_W: Int = 2,
  SDRAM_TARGET: String = "GENERIC"
) {
  val SDRAM_BANKS = 1 << SDRAM_BANK_W
  val SDRAM_ROW_W = SDRAM_ADDR_W - SDRAM_COL_W - SDRAM_BANK_W
  val SDRAM_REFRESH_CNT = 1 << SDRAM_ROW_W
  val SDRAM_START_DELAY = 100000 / (1000 / SDRAM_MHZ)
  val SDRAM_REFRESH_CYCLES = (64000*SDRAM_MHZ) / SDRAM_REFRESH_CNT-1
  val SDRAM_READ_LATENCY = 2
}

trait HasSDRAMIf{
  this: Bundle =>
  val sdram_clk_o = Output(Bool())
  val sdram_cke_o = Output(Bool())
  val sdram_cs_o = Output(Bool())
  val sdram_ras_o = Output(Bool())
  val sdram_cas_o = Output(Bool())
  val sdram_we_o = Output(Bool())
  val sdram_dqm_o = Output(UInt(2.W))
  val sdram_addr_o = Output(UInt(13.W))
  val sdram_ba_o = Output(UInt(2.W))
  val sdram_data_io = Analog(15.W)
}

class SDRAMIf extends Bundle with HasSDRAMIf

trait HasWishboneIf{
  this: Bundle =>
  val stb_i = Input(Bool())
  val we_i = Input(Bool())
  val sel_i = Input(UInt(4.W))
  val cyc_i = Input(Bool())
  val addr_i = Input(UInt(32.W))
  val data_i = Input(UInt(32.W))
  val data_o = Output(UInt(32.W))
  val stall_o = Output(Bool())
  val ack_o = Output(Bool())
}

class sdram(val cfg: sdram_bb_cfg) extends BlackBox(
  Map(
    "SDRAM_MHZ" -> IntParam(cfg.SDRAM_MHZ),
    "SDRAM_ADDR_W" -> IntParam(cfg.SDRAM_ADDR_W),
    "SDRAM_COL_W" -> IntParam(cfg.SDRAM_COL_W),
    "SDRAM_BANK_W" -> IntParam(cfg.SDRAM_BANK_W),
    "SDRAM_DQM_W" -> IntParam(cfg.SDRAM_DQM_W),
    "SDRAM_BANKS" -> IntParam(cfg.SDRAM_BANKS),
    "SDRAM_ROW_W" -> IntParam(cfg.SDRAM_ROW_W),
    "SDRAM_REFRESH_CNT" -> IntParam(cfg.SDRAM_REFRESH_CNT),
    "SDRAM_START_DELAY" -> IntParam(cfg.SDRAM_START_DELAY),
    "SDRAM_REFRESH_CYCLES" -> IntParam(cfg.SDRAM_REFRESH_CYCLES),
    "SDRAM_READ_LATENCY" -> IntParam(cfg.SDRAM_READ_LATENCY),
    "SDRAM_TARGET" -> StringParam(cfg.SDRAM_TARGET),
  )
) {
  val io = IO(new Bundle with HasSDRAMIf with HasWishboneIf {
    val clk_i = Input(Clock())
    val rst_i = Input(Bool())
  })
}

// Periphery

case class SDRAMConfig // Periphery Config
(
  address: BigInt,
  size: BigInt = 0x10000000L, // 1/16th of 32-bit
  sdcfg: sdram_bb_cfg = sdram_bb_cfg()
)

class SDRAM(cfg: SDRAMConfig, blockBytes: Int, beatBytes: Int)(implicit p: Parameters) extends LazyModule {

  val device = new MemoryDevice
  val tlcfg = TLSlaveParameters.v1(
    address             = AddressSet.misaligned(cfg.address, cfg.size),
    resources           = device.reg,
    regionType          = RegionType.UNCACHED, // cacheable
    executable          = true,
    supportsGet         = TransferSizes(1, 4), // 1, 128
    supportsPutFull     = TransferSizes(1, 4),
    supportsPutPartial  = TransferSizes(1, 4),
    fifoId              = Some(0)
  )
  val tlportcfg = TLSlavePortParameters.v1(
    managers = Seq(tlcfg),
    beatBytes = 4
  )
  val sdramnode = TLManagerNode(Seq(tlportcfg))
  val node = TLBuffer()

  // Create the IO node, and stop trying to get something from elsewhere
  val ioNode = BundleBridgeSource(() => (new SDRAMIf).cloneType)
  val port = InModuleBody { ioNode.bundle }

  // Connections of the node
  sdramnode := TLFragmenter(4, blockBytes) := TLWidthWidget(beatBytes) := node

  lazy val module = new LazyModuleImp(this) {
    val sdramimp = Module(new sdram(cfg.sdcfg))

    // Clock and Reset
    sdramimp.io.clk_i := clock
    sdramimp.io.rst_i := reset.asBool()

    // SDRAM side
    port.sdram_clk_o := sdramimp.io.sdram_clk_o
    port.sdram_cke_o := sdramimp.io.sdram_cke_o
    port.sdram_cs_o := sdramimp.io.sdram_cs_o
    port.sdram_ras_o := sdramimp.io.sdram_ras_o
    port.sdram_cas_o := sdramimp.io.sdram_cas_o
    port.sdram_we_o := sdramimp.io.sdram_we_o
    port.sdram_dqm_o := sdramimp.io.sdram_dqm_o
    port.sdram_addr_o := sdramimp.io.sdram_addr_o
    port.sdram_ba_o := sdramimp.io.sdram_ba_o
    attach(port.sdram_data_io, sdramimp.io.sdram_data_io)

    // WB side
    // Obtain the TL bundle
    val (tl_in, tl_edge) = sdramnode.in(0) // Extract the port from the node

    // Flow control
    val d_full = RegInit(false.B) // Transaction pending
    val d_size = Reg(UInt()) // Saved size
    val d_source = Reg(UInt()) // Saved source
    val d_hasData = Reg(Bool()) // Saved source
    val d_data = Reg(UInt()) // Saved data

    // hasData helds if there is a write transaction
    val hasData = tl_edge.hasData(tl_in.a.bits)

    // A channel to the Wishbone (Same handshake)
    sdramimp.io.stb_i := tl_in.a.valid && !d_full
    sdramimp.io.cyc_i := tl_in.a.valid && !d_full
    tl_in.a.ready := sdramimp.io.ack_o
    // Replicate the bits as best as we can
    sdramimp.io.addr_i := tl_in.a.bits.address
    sdramimp.io.data_i := tl_in.a.bits.data
    sdramimp.io.we_i := hasData
    sdramimp.io.sel_i := tl_in.a.bits.mask

    // Save relevant information from a to the response in d
    when (tl_in.a.fire()) { // sdramimp.io.cyc_i && sdramimp.io.ack_o equivalent
      d_size   := tl_in.a.bits.size
      d_source := tl_in.a.bits.source
      d_hasData := hasData
      d_data    := sdramimp.io.data_o
    }

    // d_full logic: It is full if there is 1 transaction not completed
    // this is, of course, waiting until D responses for every individual A transaction
    when (tl_in.d.fire()) { d_full := false.B }
    when (tl_in.a.fire()) { d_full := true.B }

    // Respond to D
    tl_in.d.bits := tl_edge.AccessAck(d_source, d_size, d_data)
    tl_in.d.bits.opcode := Mux(d_hasData, TLMessages.AccessAck, TLMessages.AccessAckData)
    tl_in.d.valid := d_full

    // Tie off unused channels
    tl_in.b.valid := false.B
    tl_in.c.ready := true.B
    tl_in.e.ready := true.B
  }
}

object SDRAMObject {
  val nextId = {
    var i = -1; () => {
      i += 1; i
    }
  }
}

case class SDRAMAttachParams
(
  device: SDRAMConfig,
  controlXType: ClockCrossingType = NoCrossing
){

  def attachTo(where: Attachable)(implicit p: Parameters): SDRAM = where {
    val name = s"sdram_${SDRAMObject.nextId()}"
    val mbus = where.locateTLBusWrapper(MBUS)
    val sdramClockDomainWrapper = LazyModule(new ClockSinkDomain(take = None))
    val sdram = sdramClockDomainWrapper { LazyModule(new SDRAM(device, mbus.blockBytes, mbus.beatBytes)) }
    sdram.suggestName(name)

    mbus.coupleTo(s"mem_${name}") { bus =>
      sdramClockDomainWrapper.clockNode := (controlXType match {
        case _: SynchronousCrossing =>
          mbus.dtsClk.map(_.bind(sdram.device))
          mbus.fixedClockNode
        case _: RationalCrossing =>
          mbus.clockNode
        case _: AsynchronousCrossing =>
          val sdramClockGroup = ClockGroup()
          sdramClockGroup := where.asyncClockGroupsNode
          sdramClockGroup
      })

      sdram.node := bus
    }

    sdram
  }
}

case object SDRAMKey extends Field[Seq[SDRAMConfig]](Nil)

trait HasSDRAM { this: BaseSubsystem =>
  val sdramNodes = p(SDRAMKey).map { ps =>
    SDRAMAttachParams(ps).attachTo(this).ioNode.makeSink()
  }
}

trait HasSDRAMModuleImp extends LazyModuleImp {
  val outer: HasSDRAM
  val sdramio = outer.sdramNodes.zipWithIndex.map { case(n,i) => n.makeIO()(ValName(s"sdram_$i")) }
}