import "DPI-C" context function int vpi_tick();

module SimVPI #(parameter CHIPID) (
    input         clock,
    input         reset,
    output [31:0] exit
);

    int __exit;
    reg [31:0] __exit_reg;

    assign exit = __exit_reg;

    // Evaluate the signals on the positive edge
    always @(posedge clock) begin
        if (reset) begin
            __exit_reg <= 0;
        end else begin
            __exit = vpi_tick();
            __exit_reg <= __exit;
        end
    end

endmodule
