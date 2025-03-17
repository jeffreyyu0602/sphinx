#include <stdio.h>
#include <stdint.h>

#define HYPER_RAM_BASE 0x4000000


int main(void) {
  printf("Waiting for HyperRAM to initialize\n");

  // wait for 150 us
  for (int i = 0; i < 10000; i++) {
    asm volatile ("");
  }

  volatile uint64_t* hyper_ram = (uint64_t*)HYPER_RAM_BASE;

  printf("Writing to HyperRAM\n");
  hyper_ram[0] = 0xdeadbeef;
  hyper_ram[1] = 0x12345678;
  hyper_ram[2] = 0x87654321;
  hyper_ram[3] = 0xcafebabe;

  printf("Reading from HyperRAM\n");
  printf("0x%lx\n", hyper_ram[0]);
  printf("0x%lx\n", hyper_ram[1]);
  printf("0x%lx\n", hyper_ram[2]);
  printf("0x%lx\n", hyper_ram[3]);

  return 0;
}
