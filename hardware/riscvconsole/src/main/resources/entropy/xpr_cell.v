(*dont_touch="true"*)
module xpr_cell(
input i_trigger,
input i_i0,
input i_i1,
output out
);

// =======================USING LUT5 UPDATE PORT======================================

wire top_out;
wire top_xor;

wire bot_out;
wire bot_xor;

assign out = top_out;

//top path

//LUT 5 XOR
LUT5_L #(
      .INIT(32'h6666_6666) 
   ) LUT5_L_xor_top (
      .LO(top_xor),
      //XOR
      .I0(bot_out), //loop
      .I1(i_i0), //input 0
      .I2(1'b0), //dont use
      //AND
      .I3(), 
      .I4() 
   );

//LUT 5 AND gate with inverted i0
LUT5_L #(
      .INIT(32'h00FF_0000)
   ) LUT5_L_and_top (
      .LO(top_out),
      //XOR
      .I0(), 
      .I1(), 
      .I2(1'b0), //dont use
      //AND
      .I3(i_trigger), //invert
      .I4(top_xor)
   );


//bot path

//LUT 5 XOR
LUT5_L #(
      .INIT(32'h6666_6666) 
   ) LUT5_L_xor_bot (
      .LO(bot_xor),
      //XOR
      .I0(top_out), //loop
      .I1(i_i1), //input 1
      .I2(1'b0), //dont use
      //AND
      .I3(),
      .I4()
   );

//LUT 5 AND gate
LUT5_L #(
      .INIT(32'h00FF_0000)
   ) LUT5_L_and_bot (
      .LO(bot_out),
      //XOR   
      .I0(),
      .I1(), 
      .I2(1'b0), //dont use
      //AND
      .I3(i_trigger), //invert
      .I4(bot_xor)  
   );


endmodule
