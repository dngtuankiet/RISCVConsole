
//Full entropy sources for poly x^32 + x^25 + x^15 + x^7 + 1

//-----Ring Generator Base polynomial-----
// x^32 + x^25 + x^15 + x^7 + 1
// val poly = Seq(25,15,7)
// val src = Seq(3,8,12)


module RingGeneratorBaseVerilog(
  input         iClk,
  input         iRst,
  input         iEn,
  input         iInit,
  input  [23:0] iEntropy,
  input  [31:0] iChallenge,
  output [31:0] oState,
  output        oSerial
);
  reg [31:0] state;

  assign oState = state;
  assign oSerial = state[0];

  always @(posedge iClk) begin
    if (iRst) begin
      state[0] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[0] <= iChallenge[0]; else state[0] <= state[1] ^ iEntropy[23];
    end

    if (iRst) begin
      state[1] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[1] <= iChallenge[1]; else state[1] <= state[2] ^ iEntropy[22];
    end

    if (iRst) begin
      state[2] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[2] <= iChallenge[2]; else state[2] <= state[3];
    end

    if (iRst) begin
      state[3] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[3] <= iChallenge[3]; else state[3] <= state[4] ^ iEntropy[21];
    end

    if (iRst) begin
      state[4] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[4] <= iChallenge[4]; else state[4] <= state[5] ^ iEntropy[20];
    end

    if (iRst) begin
      state[5] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[5] <= iChallenge[5]; else state[5] <= state[6] ^ iEntropy[19];
    end

    if (iRst) begin
      state[6] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[6] <= iChallenge[6]; else state[6] <= state[7] ^ iEntropy[18];
    end

    if (iRst) begin
      state[7] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[7] <= iChallenge[7]; else state[7] <= state[8];
    end

    if (iRst) begin
      state[8] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[8] <= iChallenge[8]; else state[8] <= state[9] ^ iEntropy[17];
    end

    if (iRst) begin
      state[9] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[9] <= iChallenge[9]; else state[9] <= state[10] ^ iEntropy[16];
    end

    if (iRst) begin
      state[10] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[10] <= iChallenge[10]; else state[10] <= state[11] ^ iEntropy[15];
    end

    if (iRst) begin
      state[11] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[11] <= iChallenge[11]; else state[11] <= state[12];
    end

    if (iRst) begin
      state[12] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[12] <= iChallenge[12]; else state[12] <= state[13] ^ iEntropy[14];
    end

    if (iRst) begin
      state[13] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[13] <= iChallenge[13]; else state[13] <= state[14] ^ iEntropy[13];
    end

    if (iRst) begin
      state[14] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[14] <= iChallenge[14]; else state[14] <= state[15] ^ iEntropy[12];
    end

    if (iRst) begin
      state[15] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[15] <= iChallenge[15]; else state[15] <= state[16];
    end

    if (iRst) begin
      state[16] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[16] <= iChallenge[16]; else state[16] <= state[17] ^ iEntropy[11];
    end

    if (iRst) begin
      state[17] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[17] <= iChallenge[17]; else state[17] <= state[18] ^ iEntropy[10];
    end

    if (iRst) begin
      state[18] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[18] <= iChallenge[18]; else state[18] <= state[12] ^ state[19];
    end

    if (iRst) begin
      state[19] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[19] <= iChallenge[19]; else state[19] <= state[20] ^ iEntropy[9];
    end

    if (iRst) begin
      state[20] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[20] <= iChallenge[20]; else state[20] <= state[21] ^ iEntropy[8];
    end

    if (iRst) begin
      state[21] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[21] <= iChallenge[21]; else state[21] <= state[22] ^ iEntropy[7];
    end

    if (iRst) begin
      state[22] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[22] <= iChallenge[22]; else state[22] <= state[8] ^ state[23];
    end
    
    if (iRst) begin
      state[23] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[23] <= iChallenge[23]; else state[23]<= state[24] ^ iEntropy[6];
    end

    if (iRst) begin
      state[24] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[24] <= iChallenge[24]; else state[24] <= state[25] ^ iEntropy[5];
    end

    if (iRst) begin
      state[25] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[25] <= iChallenge[25]; else state[25] <= state[26] ^ iEntropy[4];
    end

    if (iRst) begin
      state[26] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[26] <= iChallenge[26]; else state[26] <= state[27] ^ iEntropy[3];
    end

    if (iRst) begin
      state[27] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[27] <= iChallenge[27]; else state[27] <= state[3] ^ state[28];
    end

    if (iRst) begin
      state[28] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[28] <= iChallenge[28]; else state[28] <= state[29] ^ iEntropy[2];
    end

    if (iRst) begin
      state[29] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[29] <= iChallenge[29]; else state[29] <= state[30] ^ iEntropy[1];
    end

    if (iRst) begin
      state[30] <= 1'h0;
    end else if (iEn) begin
        if (iInit) state[30] <= iChallenge[30]; else state[30] <= state[31] ^ iEntropy[0];
    end

    if (iRst) begin
      state[31] <= 1'h0;
    end else if (iEn) begin
      if (iInit) state[31] <= iChallenge[31]; else state[31] <= state[0];
    end
  end

endmodule