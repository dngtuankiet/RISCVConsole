(* dont_touch = "true" *)
module XPRSlice(
input iR,
input i1,
input i2,
output out1,
output out2
);

(* dont_touch = "true" *) wire DA_xor_top;
(* dont_touch = "true" *) wire DA_and_top;
(* dont_touch = "true" *) wire DA_xor_bot;
(* dont_touch = "true" *) wire DA_and_bot;

assign out1 = DA_and_top;

//1-bit PUF
(* dont_touch = "true" *)
LUT5_L #(
    .INIT(32'h0FF0_0FF0)  // Specify LUT Contents
) LUT5_DA_xor_top (
    .LO(DA_xor_top),   // LUT general output
    //and
    .I0(iR), // LUT input
    .I1(DA_xor_top), // LUT input
    //xor
    .I2(i1), // LUT input
    .I3(DA_and_bot), // LUT input
    .I4(1'b0)  // LUT input
);

(* dont_touch = "true" *)
LUT5_L #(
    .INIT(32'h2222_2222)  // Specify LUT Contents
) LUT5_DA_and_top (
    .LO(DA_and_top),   // LUT general output
    //and
    .I0(iR), // LUT input
    .I1(DA_xor_top), // LUT input
    //xor
    .I2(i1), // LUT input
    .I3(DA_and_bot), // LUT input
    .I4(1'b0)  // LUT input
);

(* dont_touch = "true" *)
LUT5_L #(
    .INIT(32'h0FF0_0FF0)  // Specify LUT Contents
) LUT5_DA_xor_bot (
    .LO(DA_xor_bot),   // LUT general output
    //and
    .I0(iR), // LUT input
    .I1(DA_xor_bot), // LUT input
    //xor
    .I2(i2), // LUT input
    .I3(DA_and_top), // LUT input
    .I4(1'b0)  // LUT input
);

(* dont_touch = "true" *)
LUT5_L #(
    .INIT(32'h2222_2222)  // Specify LUT Contents
) LUT5_DA_and_bot (
    .LO(DA_and_bot),   // LUT general output
    //and
    .I0(iR), // LUT input
    .I1(DA_xor_bot), // LUT input
    //xor
    .I2(i2), // LUT input
    .I3(DA_and_top), // LUT input
    .I4(1'b0)  // LUT input
);


//=======================================================================//

(* dont_touch = "true" *) wire CB_xor_top;
(* dont_touch = "true" *) wire CB_and_top;
(* dont_touch = "true" *) wire CB_xor_bot;
(* dont_touch = "true" *) wire CB_and_bot;

assign out2 = CB_and_top;

//1-bit PUF
(* dont_touch = "true" *)
LUT5_L #(
    .INIT(32'h0FF0_0FF0)  // Specify LUT Contents
) LUT5_CB_xor_top (
    .LO(CB_xor_top),   // LUT general output
    //and
    .I0(iR), // LUT input
    .I1(CB_xor_top), // LUT input
    //xor
    .I2(i1), // LUT input
    .I3(CB_and_bot), // LUT input
    .I4(1'b0)  // LUT input
);

(* dont_touch = "true" *)
LUT5_L #(
    .INIT(32'h2222_2222)  // Specify LUT Contents
) LUT5_CB_and_top (
    .LO(CB_and_top),   // LUT general output
    //and
    .I0(iR), // LUT input
    .I1(CB_xor_top), // LUT input
    //xor
    .I2(i1), // LUT input
    .I3(CB_and_bot), // LUT input
    .I4(1'b0)  // LUT input
);

(* dont_touch = "true" *)
LUT5_L #(
    .INIT(32'h0FF0_0FF0)  // Specify LUT Contents
) LUT5_CB_xor_bot (
    .LO(CB_xor_bot),   // LUT general output
    //and
    .I0(iR), // LUT input
    .I1(CB_xor_bot), // LUT input
    //xor
    .I2(i2), // LUT input
    .I3(CB_and_top), // LUT input
    .I4(1'b0)  // LUT input
);

(* dont_touch = "true" *)
LUT5_L #(
    .INIT(32'h2222_2222)  // Specify LUT Contents
) LUT5_CB_and_bot (
    .LO(CB_and_bot),   // LUT general output
    //and
    .I0(iR), // LUT input
    .I1(CB_xor_bot), // LUT input
    //xor
    .I2(i2), // LUT input
    .I3(CB_and_top), // LUT input
    .I4(1'b0)  // LUT input
);



endmodule
