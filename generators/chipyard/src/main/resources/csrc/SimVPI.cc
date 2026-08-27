#include <svdpi.h>
#include <vpi_user.h>

#include <iostream>
#include <map>
#include <string>

#include "testchip_tsi.h"

static uint64_t vpi_time_now() {
  s_vpi_time t;
  t.type = vpiSimTime;
  vpi_get_time(nullptr, &t);
  return (uint64_t(t.high) << 32) | uint64_t(t.low);
}

class testchip_vpi_tsi_t : public testchip_tsi_t {
 public:
  static const int NUM_BANKS = 16;
  static const int NUM_SUBBANKS = 1;
  static const int BANK_WIDTH = 32;
  static const int MACRO_WIDTH = 16;

  static const uint64_t CACHE_SIZE = 2 * 1024 * 1024;
  static const uint64_t BASE_ADDR = 0x40000000;
  static const uint64_t END_ADDR = BASE_ADDR + CACHE_SIZE - 1;

  testchip_vpi_tsi_t(int argc, char** argv, bool has_loadmem)
      : testchip_tsi_t(argc, argv, has_loadmem) {};
  ~testchip_vpi_tsi_t() {};

  vpiHandle get_handle(const uint64_t address);
  uint8_t read_byte(uint64_t addr);
  void write_byte(uint64_t addr, uint8_t byte);

  void read_chunk(addr_t taddr, size_t nbytes, void* dst) override;
  void write_chunk(addr_t taddr, size_t nbytes, const void* src) override;
};

vpiHandle testchip_vpi_tsi_t::get_handle(const uint64_t address) {
  uint32_t bank_size = CACHE_SIZE / NUM_BANKS;
  uint32_t subbank_size = bank_size / NUM_SUBBANKS;

  int bank_index = address / bank_size;
  int subbank_index = (address % bank_size) / subbank_size;
  int row_index = (address % subbank_size) / BANK_WIDTH;
  int macro_index = (address % BANK_WIDTH) / MACRO_WIDTH;

  std::string bank_prefix =
      bank_index ? "bank_" + std::to_string(bank_index) : "bank";
  std::string subbank_prefix =
      subbank_index ? "ram_" + std::to_string(subbank_index) : "ram";
  std::string macro_prefix = std::to_string(macro_index);

  std::string chiptop_path = "TestDriver.testHarness.chiptop0.";
  std::string ram_path = "system." + bank_prefix + "." + subbank_prefix +
                         ".mem.mem_ext.mem_0_" + macro_prefix;

#ifndef GL_SIM
  std::string path_to_ram = chiptop_path + ram_path + ".u_ram_core.memory";
#else
  if (bank_index < 4) {
    std::replace(ram_path.begin(), ram_path.end(), '.', '_');
  } else {
    std::string subbank_path =
        subbank_prefix + ".mem.mem_ext.mem_0_" + macro_prefix;
    std::replace(subbank_path.begin(), subbank_path.end(), '.', '_');
    ram_path = "system_" + bank_prefix + "." + subbank_path;
  }
  std::string path_to_ram = chiptop_path + ram_path + ".u_ram_core.memory";
#endif

  vpiHandle ram_handle =
      vpi_handle_by_name((PLI_BYTE8*)path_to_ram.c_str(), NULL);
  if (!ram_handle) {
    std::cerr << "Could not find memory handle: " << path_to_ram << std::endl;
    std::abort();
  }

  vpiHandle elem = vpi_handle_by_index(ram_handle, row_index);
  if (!elem) {
    std::cerr << "Could not find element handle: " << row_index << std::endl;
    std::abort();
  }

  return elem;
}

void testchip_vpi_tsi_t::write_byte(uint64_t addr, uint8_t byte) {
  vpiHandle elem = get_handle(addr);

  int byte_offset = addr % MACRO_WIDTH;
  int chunk_index = byte_offset / 4;
  int bit_in_chunk = (byte_offset % 4) * 8;

  s_vpi_value value;
  value.format = vpiVectorVal;
  vpi_get_value(elem, &value);

  uint32_t chunk = value.value.vector[chunk_index].aval;
  chunk &= ~(0xFFu << bit_in_chunk);
  chunk |= (static_cast<uint32_t>(byte) << bit_in_chunk);
  value.value.vector[chunk_index].aval = chunk;

  std::cerr << "[TB t=" << vpi_time_now() << "] writing " << byte << " to "
            << addr << "\n";
  vpi_put_value(elem, &value, NULL, vpiNoDelay);
}

uint8_t testchip_vpi_tsi_t::read_byte(uint64_t addr) {
  vpiHandle elem = get_handle(addr);

  int byte_offset = addr % MACRO_WIDTH;
  int chunk_index = byte_offset / 4;         // which 32-bit word in the row
  int bit_in_chunk = (byte_offset % 4) * 8;  // bit offset inside the chunk

  s_vpi_value value;
  value.format = vpiVectorVal;
  vpi_get_value(elem, &value);

  // Extract the correct byte
  uint32_t chunk = value.value.vector[chunk_index].aval;
  uint8_t result = static_cast<uint8_t>((chunk >> bit_in_chunk) & 0xFFu);

  return result;
}

void testchip_vpi_tsi_t::write_chunk(addr_t taddr, size_t nbytes,
                                     const void* src) {
  const uint8_t* src_data = static_cast<const uint8_t*>(src);

  if (taddr == 0x2000000) {
    vpi_printf((PLI_BYTE8*)"Setting MSIP to %d\n", src_data[0]);

#ifndef GL_SIM
    const char* path =
        "TestDriver.testHarness.chiptop0.system.clint_domain.clint.ipi_0";
#else
    const char* path =
        "TestDriver.testHarness.chiptop0.system_clint_domain_clint_ipi_0_reg.Q";
#endif

    const char* env = std::getenv("SKIP_GDB_LOAD");
    if (env && std::string(env) == "1") {
      std::cerr << "SKIP_GDB_LOAD is set, skipping write to MSIP\n";
      return;
    }

    vpiHandle h_msip = vpi_handle_by_name((PLI_BYTE8*)path, NULL);
    if (!h_msip) {
      vpi_printf((PLI_BYTE8*)"ERROR: Could not get handle to %s\n", path);
      return;
    }

    s_vpi_value val;
    val.format = vpiIntVal;
    val.value.integer = src_data[0] ? 1 : 0;

    std::cerr << "[TB t=" << vpi_time_now() << "] Setting MSIP to "
              << static_cast<int>(src_data[0]) << "\n";
    vpi_put_value(h_msip, &val, NULL, vpiNoDelay);
    std::cerr << "[TB t=" << vpi_time_now() << "] Done setting MSIP\n";

    return;
  }

  if (taddr < BASE_ADDR || taddr > END_ADDR) {
    std::cerr << "ERROR: Address " << std::hex << taddr << " is out of range"
              << std::dec << std::endl;
    throw std::runtime_error("Write address out of range!");
  }

  taddr -= BASE_ADDR;  // Adjust address to match the memory map

  for (size_t i = 0; i < nbytes; i++) {
    write_byte(taddr + i, src_data[i]);
  }
}

void testchip_vpi_tsi_t::read_chunk(addr_t taddr, size_t nbytes, void* dst) {
  uint8_t* result = static_cast<uint8_t*>(dst);

  if (taddr < BASE_ADDR || taddr > END_ADDR) {
    std::cerr << "ERROR: Address " << std::hex << taddr << " is out of range"
              << std::dec << std::endl;
    throw std::runtime_error("Read address out of range!");
  }

  taddr -= BASE_ADDR;  // Adjust address to match the memory map

  for (int i = 0; i < nbytes; ++i) {
    result[i] = read_byte(taddr + i);
  }
}

testchip_tsi_t* tsi = nullptr;

// Remove VCS simv option from argv if it match pattern -X???=
// Static: testchipip's SimTSI.cc links its own copy of this helper, and both
// objects end up in generic-config simv builds.
static void remove_vcs_simv_opt(int& argc, char**& argv) {
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
