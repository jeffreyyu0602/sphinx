// See LICENSE for license details

`timescale 1ns/1ps

module TSMC7DigitalGPIOCellH(
    inout pad,
    output i,
    input ie,
    input o,
    input oe,
    input [3:0] ds,
    input rte
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
        .RTE(rte)
    );

endmodule

module TSMC7DigitalGPIOCellV(
    inout pad,
    output i,
    input ie,
    input o,
    input oe,
    input [3:0] ds,
    input rte
);

    PDDWUWSWEWCDGS_V iocell(
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
        .RTE(rte)
    );

endmodule

module TSMC7DigitalInIOCellH(
    input pad,
    output i,
    input ie,
    input [3:0] ds,
    input rte
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
        .RTE(rte)
    );

endmodule

module TSMC7DigitalInIOCellV(
    input pad,
    output i,
    input ie,
    input [3:0] ds,
    input rte
);

    PDDWUWSWEWCDGS_V iocell(
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
        .RTE(rte)
    );

endmodule

module TSMC7DigitalOutIOCellH(
    output pad,
    input o,
    input oe,
    input [3:0] ds,
    input rte
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
        .RTE(rte)
    );

endmodule

module TSMC7DigitalOutIOCellV(
    output pad,
    input o,
    input oe,
    input [3:0] ds,
    input rte
);

    PDDWUWSWEWCDGS_V iocell(
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
        .RTE(rte)
    );

endmodule

module TSMC7POCMCellV(
    input rte
);

    PVDD2POCM_V pocmcell(
        .RTE(rte)
    );

endmodule

module TSMC7POCMCellH(
    input rte
);

    PVDD2POCM_H pocmcell(
        .RTE(rte)
    );

endmodule
