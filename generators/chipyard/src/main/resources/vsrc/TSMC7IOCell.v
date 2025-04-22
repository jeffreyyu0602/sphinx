// See LICENSE for license details

`timescale 1ns/1ps

module TSMC7DigitalGPIOCell(
    inout pad,
    output i,
    input ie,
    input o,
    input oe,
    input [3:0] ds
);

    PDDWUWSWEWCDGS_H iocell(
        .C(i),
        .DS0(ds[0]),
        .DS1(ds[1]),
        .DS2(ds[2]),
        .DS3(ds[3]),
        .I(o),
        .IE(ie),
        .OE(oe),
        .PAD(pad),
        .PU(1'b0),
        .PD(1'b1),
        .ST(1'b0),
        .RTE(1'b0)
    );

endmodule

module TSMC7DigitalInIOCell(
    input pad,
    output i,
    input ie,
    input [3:0] ds
);

    PDDWUWSWEWCDGS_H iocell(
        .C(i),
        .DS0(ds[0]),
        .DS1(ds[1]),
        .DS2(ds[2]),
        .DS3(ds[3]),
        .I(1'b0),
        .IE(ie),
        .OE(1'b0),
        .PAD(pad),
        .PU(1'b0),
        .PD(1'b1),
        .ST(1'b0),
        .RTE(1'b0)
    );

endmodule

module TSMC7DigitalOutIOCell(
    output pad,
    input o,
    input oe,
    input [3:0] ds
);

    PDDWUWSWEWCDGS_H iocell(
        .DS0(ds[0]),
        .DS1(ds[1]),
        .DS2(ds[2]),
        .DS3(ds[3]),
        .IE(1'b0),
        .OE(oe),
        .I(o),
        .PAD(pad),
        .PU(1'b0),
        .PD(1'b1),
        .ST(1'b0),
        .RTE(1'b0)
    );

endmodule
