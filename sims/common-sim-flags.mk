#----------------------------------------------------------------------------------------
# common gcc configuration/optimization
#----------------------------------------------------------------------------------------
SIM_OPT_CXXFLAGS := -O3
LRISCV=-lriscv

export USE_CHISEL6=1

SIM_CXXFLAGS = \
	$(CXXFLAGS) \
	$(SIM_OPT_CXXFLAGS) \
	-std=c++17 \
	-I$(RISCV)/include \
	-I$(dramsim_dir) \
	-I$(GEN_COLLATERAL_DIR) \
	-I$(base_dir)/generators/voyager/accelerator \
	-I$(base_dir)/generators/voyager/accelerator/test/common \
	-I$(base_dir)/generators/voyager/accelerator/src/ \
	-I$(base_dir)/generators/voyager/accelerator/test/compiler/proto/ \
	-I$(base_dir)/generators/voyager/accelerator/lib/xtensor/include \
	-I$(base_dir)/generators/voyager/accelerator/lib/xtl/include \
	-I$(base_dir)/generators/voyager/accelerator/lib/spdlog/include \
	-I/cad/mentor/2024.2_1/Mgc_home/shared/include \
	-I$(CONDA_PREFIX)/include \
	$(EXTRA_SIM_CXXFLAGS)

SIM_LDFLAGS = \
	$(LDFLAGS) \
	-L$(RISCV)/lib \
	-Wl,-rpath,$(RISCV)/lib \
	-L$(sim_dir) \
	-L$(dramsim_dir) \
	-L$(CATAPULT_ROOT)/shared/lib \
	$(LRISCV) \
	-lfesvr \
	-ldramsim \
	-lsystemc \
	-lstdc++fs \
	-labsl_hash \
	-labsl_log_internal_message \
	-labsl_log_internal_check_op \
	-labsl_log_internal_nullguard \
	-lprotobuf \
	-Wl,-rpath,$(CONDA_PREFIX)/lib \
	-L$(CONDA_PREFIX)/lib \
	$(EXTRA_SIM_LDFLAGS)

CLOCK_PERIOD ?= 1.0
RESET_DELAY ?= 777.7

SIM_PREPROC_DEFINES = \
	+define+CLOCK_PERIOD=$(CLOCK_PERIOD) \
	+define+RESET_DELAY=$(RESET_DELAY) \
	+define+PRINTF_COND=$(TB).printf_cond \
	+define+STOP_COND=!$(TB).reset \
	+define+MODEL=$(MODEL) \
	+define+RANDOMIZE_MEM_INIT \
	+define+RANDOMIZE_REG_INIT \
	+define+RANDOMIZE_GARBAGE_ASSIGN \
	+define+RANDOMIZE_INVALID_ASSIGN
