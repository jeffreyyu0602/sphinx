module D2Top (
  input	HCLK,
  input	PORESETn,
  output	CRG_DIAG0,
  input	UART_M_RXD,
  output	UART_M_TXD,
  output	UART_M_RTS,
  input	UART_M_CTS,
  input	UART2_RXD,
  output	UART2_TXD,
  input	NUART0_RXD,
  output	NUART0_TXD,
  input	GPIO0_0_PORTIN,
  output	GPIO0_0_PORTOUT,
  output	GPIO0_0_PORTEN,
  input	GPIO0_1_PORTIN,
  output	GPIO0_1_PORTOUT,
  output	GPIO0_1_PORTEN,
  input	GPIO0_2_PORTIN,
  output	GPIO0_2_PORTOUT,
  output	GPIO0_2_PORTEN,
  input	HYPERRAM_DQ_0_PORTIN,
  output	HYPERRAM_DQ_0_PORTOUT,
  output	HYPERRAM_DQ_0_PORTEN,
  input	HYPERRAM_DQ_1_PORTIN,
  output	HYPERRAM_DQ_1_PORTOUT,
  output	HYPERRAM_DQ_1_PORTEN,
  input	HYPERRAM_DQ_2_PORTIN,
  output	HYPERRAM_DQ_2_PORTOUT,
  output	HYPERRAM_DQ_2_PORTEN,
  input	HYPERRAM_DQ_3_PORTIN,
  output	HYPERRAM_DQ_3_PORTOUT,
  output	HYPERRAM_DQ_3_PORTEN,
  input	HYPERRAM_DQ_4_PORTIN,
  output	HYPERRAM_DQ_4_PORTOUT,
  output	HYPERRAM_DQ_4_PORTEN,
  input	HYPERRAM_DQ_5_PORTIN,
  output	HYPERRAM_DQ_5_PORTOUT,
  output	HYPERRAM_DQ_5_PORTEN,
  input	HYPERRAM_DQ_6_PORTIN,
  output	HYPERRAM_DQ_6_PORTOUT,
  output	HYPERRAM_DQ_6_PORTEN,
  input	HYPERRAM_DQ_7_PORTIN,
  output	HYPERRAM_DQ_7_PORTOUT,
  output	HYPERRAM_DQ_7_PORTEN,
  input	HYPERRAM_RWDS_PORTIN,
  output	HYPERRAM_RWDS_PORTOUT,
  output	HYPERRAM_RWDS_PORTEN,
  output	HYPERRAM_RESETN,
  output	HYPERRAM_CSN0,
  output	HYPERRAM_CK,
  output	HYPERRAM_CKN
);

// Empty module
  assign CRG_DIAG0 = 1'b0;
  assign UART_M_TXD = 1'b0;
  assign UART_M_RTS = 1'b0;
  assign UART2_TXD = 1'b0;
  assign NUART0_TXD = 1'b0;
  assign GPIO0_0_PORTOUT = 1'b0;
  assign GPIO0_0_PORTEN = 1'b0;
  assign GPIO0_1_PORTOUT = 1'b0;
  assign GPIO0_1_PORTEN = 1'b0;
  assign GPIO0_2_PORTOUT = 1'b0;
  assign GPIO0_2_PORTEN = 1'b0;
  assign HYPERRAM_DQ_0_PORTOUT = 1'b0;
  assign HYPERRAM_DQ_0_PORTEN = 1'b0;
  assign HYPERRAM_DQ_1_PORTOUT = 1'b0;
  assign HYPERRAM_DQ_1_PORTEN = 1'b0;
  assign HYPERRAM_DQ_2_PORTOUT = 1'b0;
  assign HYPERRAM_DQ_2_PORTEN = 1'b0;
  assign HYPERRAM_DQ_3_PORTOUT = 1'b0;
  assign HYPERRAM_DQ_3_PORTEN = 1'b0;
  assign HYPERRAM_DQ_4_PORTOUT = 1'b0;
  assign HYPERRAM_DQ_4_PORTEN = 1'b0;
  assign HYPERRAM_DQ_5_PORTOUT = 1'b0;
  assign HYPERRAM_DQ_5_PORTEN = 1'b0;
  assign HYPERRAM_DQ_6_PORTOUT = 1'b0;
  assign HYPERRAM_DQ_6_PORTEN = 1'b0;
  assign HYPERRAM_DQ_7_PORTOUT = 1'b0;
  assign HYPERRAM_DQ_7_PORTEN = 1'b0;
  assign HYPERRAM_RWDS_PORTOUT = 1'b0;
  assign HYPERRAM_RWDS_PORTEN = 1'b0;
  assign HYPERRAM_RESETN = 1'b0;
  assign HYPERRAM_CSN0 = 1'b0;
  assign HYPERRAM_CK = 1'b0;
  assign HYPERRAM_CKN = 1'b0;

endmodule