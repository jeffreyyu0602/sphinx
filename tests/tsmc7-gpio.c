#include <stdio.h>
#include <stdint.h>
#include "mmio.h"

#define GPIODRIVER 0x69000

int main (void) {
    reg_write32(GPIODRIVER, 3); // 3'b011

    printf("Wrote to GPIO driver\n");

    reg_write32(GPIODRIVER, 5); // 3'b101

    printf("Wrote to GPIO driver\n");

    reg_write32(GPIODRIVER, 7); // 3'b111

    printf("Wrote to GPIO driver\n");

    return 0;
}
