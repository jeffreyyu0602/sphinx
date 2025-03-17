### Tools ###
CC=riscv64-unknown-elf-gcc
OBJDUMP=riscv64-unknown-elf-objdump

### Flags ###
ARCH_FLAGS = -march=rv64imafd -mabi=lp64d -mcmodel=medany
COMPILE_FLAGS = -std=gnu99 -O2 -Wall -Wextra -fno-common -fno-builtin-printf -Icommon -I../ -specs=htif_nano.specs
CFLAGS=$(ARCH_FLAGS) $(COMPILE_FLAGS)
LDFLAGS= -static -specs=htif_nano.specs -T../htif.ld

### Files ###
SRC = $(shell find -L networks -name "*.c" )
OBJ = $(SRC:.c=.o)
BIN = $(SRC:.c=.riscv)

common/run_voyager_operation.o: common/run_voyager_operation.c common/run_voyager_operation.h common/voyager_address.h common/voyager_params.h
	$(CC) $(CFLAGS) -c $< -o $@

common/traps.o: common/traps.c common/traps.h
	$(CC) $(CFLAGS) -c $< -o $@

$(OBJ): %.o: %.c common/run_voyager_operation.h common/voyager_address.h common/voyager_params.h
	$(CC) $(CFLAGS) -c $< -o $@

$(BIN): %.riscv: %.o common/run_voyager_operation.o common/traps.o
	$(CC) $(LDFLAGS) $^ -o $@

%.o: %.c mmio.h address.h
	$(CC) $(CFLAGS) -c $< -o $@

%.riscv: %.o
	$(CC) $(LDFLAGS) $^ -o $@
