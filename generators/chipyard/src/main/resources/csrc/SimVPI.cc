#include <svdpi.h>
#include <vpi_user.h>

#include <map>
#include <string>

#include "testchip_tsi.h"

class testchip_vpi_tsi_t : public testchip_tsi_t {
 public:
  static const int NUM_BANKS = 1;
  static const int NUM_SUBBANKS = 32;
  static const int NUM_BYTES = 8;

  static const uint64_t MEM_SIZE = 2 * 1024 * 1024;
  static const uint64_t BASE_ADDR = 0x40000000;
  static const uint64_t END_ADDR = BASE_ADDR + MEM_SIZE - 1;
  static const uint64_t SUBBANK_SIZE = MEM_SIZE / NUM_SUBBANKS;

  vpiHandle banks[NUM_SUBBANKS] = {nullptr};

  testchip_vpi_tsi_t(int argc, char** argv, bool has_loadmem);
  ~testchip_vpi_tsi_t() {};

  void write_chunk(addr_t taddr, size_t nbytes, const void* src) override;
  void read_chunk(addr_t taddr, size_t nbytes, void* dst) override;
};

testchip_vpi_tsi_t::testchip_vpi_tsi_t(int argc, char** argv, bool has_loadmem)
    : testchip_tsi_t(argc, argv, has_loadmem) {
  char pathbuf[512];
  for (int sub = 0; sub < NUM_SUBBANKS; ++sub) {
    if (sub == 0) {
      snprintf(pathbuf, sizeof(pathbuf),
               "TestDriver.testHarness.chiptop0.system.bank.ram.mem.mem_ext."
               "mem_0_0.u_ram_core.memory");
    } else {
      snprintf(pathbuf, sizeof(pathbuf),
               "TestDriver.testHarness.chiptop0.system.bank.ram_%d.mem.mem_ext."
               "mem_0_0.u_ram_core.memory",
               sub);
    }

    banks[sub] = vpi_handle_by_name((PLI_BYTE8*)pathbuf, NULL);
    if (!banks[sub]) {
      vpi_printf((PLI_BYTE8*)"ERROR: Could not get handle to %s\n", pathbuf);
      throw std::runtime_error("Failed to get handle to memory");
    }
  }
  vpi_printf((PLI_BYTE8*)"Successfully got handles to all banks\n");
}

void testchip_vpi_tsi_t::write_chunk(addr_t taddr, size_t nbytes,
                                     const void* src) {
  const uint8_t* src_data = static_cast<const uint8_t*>(src);

  if (taddr == 0x2000000) {
    vpi_printf((PLI_BYTE8*)"Setting MSIP to %d\n", src_data[0]);

    const char* path =
        "TestDriver.testHarness.chiptop0.system.clint_domain.clint.ipi_0";

    vpiHandle h_msip = vpi_handle_by_name((PLI_BYTE8*)path, NULL);
    if (!h_msip) {
      vpi_printf((PLI_BYTE8*)"ERROR: Could not get handle to %s\n", path);
      throw std::runtime_error("Failed to set MSIP");
    }

    s_vpi_value val;
    val.format = vpiIntVal;
    val.value.integer = src_data[0] ? 1 : 0;
    vpi_put_value(h_msip, &val, NULL, vpiNoDelay);

    return;
  }

  if (taddr < BASE_ADDR || taddr > END_ADDR) {
    vpi_printf((PLI_BYTE8*)"ERROR: Address %08x is out of range\n", taddr);
    throw std::runtime_error("Write address out of range!");
  }

  vpi_printf((PLI_BYTE8*)"Writing %zu bytes to address %08x\n", nbytes, taddr);

  taddr -= BASE_ADDR;  // Adjust address to match the memory map

  for (size_t i = 0; i < nbytes; i++) {
    uint8_t byte = src_data[i];

    addr_t addr = taddr + i;
    int bank_idx = addr / SUBBANK_SIZE;
    int index = (addr % SUBBANK_SIZE) / NUM_BYTES;
    int byte_offset = addr % NUM_BYTES;

    vpiHandle elem = vpi_handle_by_index(banks[bank_idx], index);
    if (!elem) {
      vpi_printf((PLI_BYTE8*)"ERROR: Could not get handle to "
      "sub-bank %d index %d at addr %08x\n", bank_idx, index, addr);
      throw std::runtime_error("Failed to get handle to scratchpad element");
    }

    s_vpi_value value;
    value.format = vpiVectorVal;
    vpi_get_value(elem, &value);

    uint64_t reg_val =
        (static_cast<uint64_t>(value.value.vector[1].aval) << 32) |
        static_cast<uint64_t>(value.value.vector[0].aval);

    // Modify only the targeted byte
    reg_val &= ~(0xFFULL << (byte_offset * 8));
    reg_val |= (static_cast<uint64_t>(byte) << (byte_offset * 8));

    value.value.vector[0].aval = static_cast<uint32_t>(reg_val);
    value.value.vector[1].aval = static_cast<uint32_t>((reg_val >> 32));

    vpi_put_value(elem, &value, NULL, vpiNoDelay);

    // vpi_printf((PLI_BYTE8*)"Wrote 0x%02x to mem[%d][%d] byte offset %d\n", byte,
    //            bank_idx, index, byte_offset);
  }
}

void testchip_vpi_tsi_t::read_chunk(addr_t taddr, size_t nbytes, void* dst) {
  uint8_t* result = static_cast<uint8_t*>(dst);

  if (taddr < BASE_ADDR || taddr > END_ADDR) {
    vpi_printf((PLI_BYTE8*)"ERROR: Address %08x is out of range\n", taddr);
    throw std::runtime_error("Read address out of range!");
  }

  taddr -= BASE_ADDR;  // Adjust address to match the memory map

  for (int i = 0; i < nbytes; ++i) {
    addr_t addr = taddr + i;
    int bank_idx = addr / SUBBANK_SIZE;
    int index = (addr % SUBBANK_SIZE) / NUM_BYTES;
    int byte_offset = addr % NUM_BYTES;

    vpiHandle elem = vpi_handle_by_index(banks[bank_idx], index);
    if (!elem) {
      vpi_printf((PLI_BYTE8*)"ERROR: Could not get handle to "
      "sub-bank %d index %d at addr %08x\n", bank_idx, index, addr);
      throw std::runtime_error("Failed to get handle to bank");
    }

    s_vpi_value value;
    value.format = vpiVectorVal;
    vpi_get_value(elem, &value);

    uint64_t reg_val =
        (static_cast<uint64_t>(value.value.vector[1].aval) << 32) |
        static_cast<uint64_t>(value.value.vector[0].aval);

    uint8_t byte_val = (reg_val >> (byte_offset * 8)) & 0xFF;

    // vpi_printf((PLI_BYTE8*)"Read mem[%d][%d] = %08x\n", bank_idx, index,
    //            byte_val);

    result[i] = byte_val;
  }
}

testchip_tsi_t* tsi = nullptr;

// Remove VCS simv option from argv if it match pattern -X???=
void remove_vcs_simv_opt(int& argc, char**& argv) {
  int idx = 0;
  while (idx < argc) {
    std::string str = std::string(argv[idx]);
    if (str.length() > 1 && str[0] == '-' && str[1] != '-' &&
        str.find('=') != std::string::npos) {
      // Found -????=???? as VCS simv option
      for (int i = idx; i < argc - 1; i++) {
        // Remove the current option
        argv[i] = argv[i + 1];
      }
      argc--;
    } else {
      idx++;
    }
  }
}

extern "C" int vpi_tick() {
  if (tsi == nullptr) {
    s_vpi_vlog_info info;
    if (!vpi_get_vlog_info(&info)) abort();

    // Prevent simv option enter htif
    remove_vcs_simv_opt(info.argc, info.argv);

    tsi = new testchip_vpi_tsi_t(info.argc, info.argv, true);
  }

  tsi->switch_to_host();

  return tsi->done() ? (tsi->exit_code() << 1 | 1) : 0;
}
