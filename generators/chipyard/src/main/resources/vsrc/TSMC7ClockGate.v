module EICG_wrapper(
  output out,
  input en,
  input test_en,
  input in
);

`ifdef SYNTHESIS
    CKLNQD10BWP240H8P57PDLVT inst(
      .CP(in),
      .E(en),
      .TE(test_en),
      .Q(out)
    );
`else
  reg en_latched /*verilator clock_enable*/;

  always @(*) begin
     if (!in) begin
        en_latched = en || test_en;
     end
  end

  assign out = en_latched && in;
`endif

endmodule