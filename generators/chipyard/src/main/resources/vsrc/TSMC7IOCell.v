// See LICENSE for license details

`timescale 1ns/1ps

module TSMC7DigitalGPIOCell(
    inout pad,
    output i,
    input ie,
    input o,
    input oe
);

    PDDWUWSWEWCDGS_H iocell(
        .C(i),
        .DS0(1'b0),
        .DS1(1'b0),
        .DS2(1'b0),
        .DS3(1'b0),
        .I(o),
        .IE(ie),
        .OE(oe),
        .PAD(pad),
        .PU(1'b0),
        .PD(1'b0),
        .ST(1'b0),
        .RTE(1'b0)
    );

endmodule

module TSMC7DigitalInIOCell(
    input pad,
    output i,
    input ie
);

    PDDWUWSWEWCDGS_H iocell(
        .C(i),
        .DS0(1'b0),
        .DS1(1'b0),
        .DS2(1'b0),
        .DS3(1'b0),
        .I(1'b0),
        .IE(ie),
        .OE(1'b0),
        .PAD(pad),
        .PU(1'b0),
        .PD(1'b0),
        .ST(1'b0),
        .RTE(1'b0)
    );

endmodule

module TSMC7DigitalOutIOCell(
    output pad,
    input o,
    input oe
);

    PDDWUWSWEWCDGS_H iocell(
        .DS0(1'b0),
        .DS1(1'b0),
        .DS2(1'b0),
        .DS3(1'b0),
        .IE(1'b0),
        .OE(oe),
        .I(o),
        .PAD(pad),
        .PU(1'b0),
        .PD(1'b0),
        .ST(1'b0),
        .RTE(1'b0)
    );

endmodule
