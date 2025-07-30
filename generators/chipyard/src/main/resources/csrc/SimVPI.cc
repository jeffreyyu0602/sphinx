#include <svdpi.h>
#include <vpi_user.h>

#include <iostream>
#include <map>
#include <string>

#include "testchip_tsi.h"

class testchip_vpi_tsi_t : public testchip_tsi_t {
 public:
  static const int NUM_BANKS = 6;
  static const int NUM_SUBBANKS = 2;
  static const int NUM_BYTES = 32;
  static const int MACRO_WIDTH = 8;

  static const uint64_t CACHE_SIZE = 0.75 * 1024 * 1024;
  static const uint64_t BASE_ADDR = 0x40000000;
  static const uint64_t END_ADDR = BASE_ADDR + CACHE_SIZE - 1;

  testchip_vpi_tsi_t(int argc, char** argv, bool has_loadmem)
      : testchip_tsi_t(argc, argv, has_loadmem) {};
  ~testchip_vpi_tsi_t() {};

  std::string decode_address(const long long address, int& word_index);
  void write_chunk(addr_t taddr, size_t nbytes, const void* src) override;
  void read_chunk(addr_t taddr, size_t nbytes, void* dst) override;
};

std::string testchip_vpi_tsi_t::decode_address(const long long address,
                                               int& word_index) {
  uint32_t bank_size = CACHE_SIZE / NUM_BANKS;
  uint32_t subbank_size = bank_size / NUM_SUBBANKS;

  int bank_index = address / bank_size;
  int subbank_index = (address % bank_size) / subbank_size;
  word_index = (address % subbank_size) / NUM_BYTES;
  int macro_index = (address % NUM_BYTES) / MACRO_WIDTH;

  std::string bank_prefix =
      bank_index ? "bank_" + std::to_string(bank_index) : "bank";
  std::string subbank_prefix =
      subbank_index ? "ram_" + std::to_string(subbank_index) : "ram";
  std::string macro_prefix = "mem_0_" + std::to_string(macro_index);

  std::string path_to_ram = "TestDriver.testHarness.chiptop0.system." +
                            bank_prefix + "." + subbank_prefix +
                            ".mem.mem_ext." + macro_prefix + ".u_ram_core.memory";
  return path_to_ram;
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

    int word_index;
    std::string path_to_ram = decode_address(taddr + i, word_index);
    std::cerr << "Writing byte to RAM at: " << path_to_ram << std::endl;

    vpiHandle ram_handle =
        vpi_handle_by_name(strdup(path_to_ram.c_str()), NULL);
    if (ram_handle == NULL) {
      std::cerr << "Could not find memory handle: " << path_to_ram << std::endl;
      std::abort();
    }

    vpiHandle elem = vpi_handle_by_index(ram_handle, word_index);
    if (!elem) {
      std::cerr << "Could not find word handle: " << word_index << std::endl;
      std::abort();
    }

    s_vpi_value value;
    value.format = vpiVectorVal;
    vpi_get_value(elem, &value);

    uint64_t reg_val =
        (static_cast<uint64_t>(value.value.vector[1].aval) << 32) |
        static_cast<uint64_t>(value.value.vector[0].aval);

    // Modify only the targeted byte
    int byte_offset = (taddr + i) % NUM_BYTES;
    reg_val &= ~(0xFFULL << (byte_offset * 8));
    reg_val |= (static_cast<uint64_t>(byte) << (byte_offset * 8));

    value.value.vector[0].aval = static_cast<uint32_t>(reg_val);
    value.value.vector[1].aval = static_cast<uint32_t>((reg_val >> 32));

    vpi_put_value(elem, &value, NULL, vpiNoDelay);
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
    int word_index;
    std::string path_to_ram = decode_address(taddr + i, word_index);

    vpiHandle ram_handle =
        vpi_handle_by_name(strdup(path_to_ram.c_str()), NULL);
    if (ram_handle == NULL) {
      std::cerr << "Could not find memory handle: " << path_to_ram << std::endl;
      std::abort();
    }

    vpiHandle elem = vpi_handle_by_index(ram_handle, word_index);
    if (!elem) {
      std::cerr << "Could not find word handle: " << word_index << std::endl;
      std::abort();
    }

    s_vpi_value value;
    value.format = vpiVectorVal;
    vpi_get_value(elem, &value);

    uint64_t reg_val =
        (static_cast<uint64_t>(value.value.vector[1].aval) << 32) |
        static_cast<uint64_t>(value.value.vector[0].aval);

    int byte_offset = (taddr + i) % NUM_BYTES;
    result[i] = (reg_val >> (byte_offset * 8)) & 0xFF;
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
