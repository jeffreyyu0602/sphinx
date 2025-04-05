#include <vpi_user.h>
#include <svdpi.h>
#include <map>
#include <string>
#include "testchip_tsi.h"

class testchip_vpi_tsi_t : public testchip_tsi_t {
 public:
  static const int NUM_BANKS = 8;
  vpiHandle banks[NUM_BANKS] = {nullptr};

  testchip_vpi_tsi_t(int argc, char** argv, bool has_loadmem);
  ~testchip_vpi_tsi_t() {};

  void write_chunk(addr_t taddr, size_t nbytes, const void* src) override;
  void read_chunk(addr_t taddr, size_t nbytes, void* dst) override;
};

testchip_vpi_tsi_t::testchip_vpi_tsi_t(int argc, char** argv, bool has_loadmem)
    : testchip_tsi_t(argc, argv, has_loadmem) {
  char pathbuf[512];
  for (int i = 0; i < NUM_BANKS; ++i) {
    snprintf(pathbuf, sizeof(pathbuf),
             "TestDriver.testHarness.chiptop0.system.bank.ram.mem.mem_ext.mem_"
             "0_%d.ram",
             i);
    banks[i] = vpi_handle_by_name((PLI_BYTE8*)pathbuf, NULL);
    if (!banks[i]) {
      vpi_printf((PLI_BYTE8*)"ERROR: Could not get handle to %s\n", pathbuf);
      throw std::runtime_error("Failed to get handle to on-chip scratchpad");
    }
  }
  vpi_printf((PLI_BYTE8*)"Found %d banks\n", NUM_BANKS);
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

  if (taddr < 0x40000000 || taddr > 0x44000000) {
    vpi_printf((PLI_BYTE8*)"ERROR: Address %08x is out of range\n", taddr);
    throw std::runtime_error("Write address out of range!");
  }

  vpi_printf((PLI_BYTE8*)"Writing %zu bytes to address %08x\n", nbytes, taddr);

  taddr -= 0x40000000;  // Adjust address to match the memory map

  for (size_t i = 0; i < nbytes; i++) {
    uint8_t byte = src_data[i];
    int bank = (taddr + i) % NUM_BANKS;
    int index = (taddr + i) / NUM_BANKS;

    vpiHandle elem = vpi_handle_by_index(banks[bank], index);
    if (!elem) {
      vpi_printf((PLI_BYTE8*)"ERROR: Could not get handle to bank %d index %d at addr %08x\n",
                 bank, index, taddr + i);
      throw std::runtime_error("Failed to get handle to scratchpad element");
    }

    s_vpi_value val;
    val.format = vpiIntVal;
    val.value.integer = byte;
    vpi_put_value(elem, &val, NULL, vpiNoDelay);

    vpi_printf((PLI_BYTE8*)"Wrote 0x%02x to bank_%d[%d]\n", byte, bank, index);
  }
}

void testchip_vpi_tsi_t::read_chunk(addr_t taddr, size_t nbytes, void* dst) {
  uint8_t* result = static_cast<uint8_t*>(dst);

  if (taddr < 0x40000000 || taddr > 0x44000000) {
    vpi_printf((PLI_BYTE8*)"ERROR: Address %08x is out of range\n", taddr);
    throw std::runtime_error("Read address out of range!");
  }

  taddr -= 0x40000000;  // Adjust address to match the memory map

  for (int i = 0; i < nbytes; ++i) {
    int bank = (taddr + i) % NUM_BANKS;
    int index = (taddr + i) / NUM_BANKS;

    vpiHandle elem = vpi_handle_by_index(banks[bank], index);
    if (!elem) {
      vpi_printf((PLI_BYTE8*)"ERROR: Could not get handle to bank %d index %d at addr %08x\n",
                 bank, index, taddr + i);
      throw std::runtime_error("Failed to get handle to bank");
    }

    s_vpi_value value;
    value.format = vpiIntVal;
    vpi_get_value(elem, &value);

    vpi_printf((PLI_BYTE8*)"Read mem[%d] = %08x\n", index, value.value.integer);

    result[i] = value.value.integer;
  }
}

testchip_tsi_t* tsi = nullptr;

// Remove VCS simv option from argv if it match pattern -X???=
void remove_vcs_simv_opt(int & argc, char **& argv){
    int idx = 0;
    while(idx < argc){
        std::string str = std::string(argv[idx]);
        if(str.length() > 1 && str[0] == '-' && str[1] != '-' && str.find('=') != std::string::npos){
            // Found -????=???? as VCS simv option
            for(int i = idx; i < argc - 1; i++){
                // Remove the current option
                argv[i] = argv[i + 1];
            }
            argc--;
        }else{
            idx++;
        }
    }
}

extern "C" int vpi_tick()
{
    if (tsi == nullptr) {
        s_vpi_vlog_info info;
        if (!vpi_get_vlog_info(&info))
          abort();

        // Prevent simv option enter htif
        remove_vcs_simv_opt(info.argc, info.argv);

        tsi = new testchip_vpi_tsi_t(info.argc, info.argv, true);
    }

    tsi->switch_to_host();

    return tsi->done() ? (tsi->exit_code() << 1 | 1) : 0;
}
