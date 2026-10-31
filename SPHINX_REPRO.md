# Reproducing the Sphinx chip firmware (2026-09-17 kit)

The firmware that runs on the Sphinx chip is the 2026-09-17 kit, committed on
this branch as `sphinx-repro/kits/sphinx-tests.zip` (graded: ELFs, dump lists
and expected readbacks) and `sphinx-repro/kits/sphinx-power.zip`
(`POWER_LOOP` ELFs). Later Voyager work drops Sphinx support, so the kit is
rebuilt from the commits below, never from a newer tree. This branch is the
SoC superrepo commit `473ff85e` plus this file and `sphinx-repro/`; it changes
no tracked source. All paths here are relative to the root of a clone.

Following this file rebuilds the compiled params and both kits byte for byte
and checks every file against `sphinx-repro/md5/`.

## What the kit holds

Twelve networks, all MXNF4 on the SphinxConfig geometry (64x64 PE array,
buffers 512/576/256, 2 MB / 16-bank scratchpad at `0x4000_0000`, 256 KB
reserved at its base), one decoder layer, context 1024, priced at 1 GHz:

| network | graph | model | qconfig | ELFs |
| --- | --- | --- | --- | --- |
| `llama_prefill` | `llama_prefill` | Llama-3.1-8B | `mxnf4` | 18 |
| `llama_prefill_mp` | `llama_prefill` | Llama-3.1-8B | `mxnf4_attn_head_int6` | 20 |
| `llama_prefill_spmm` | `llama_prefill` | Llama-3.1-8B | `mxnf4_outlier` | 23 |
| `llama_decode` | `llama_decode` | Llama-3.1-8B | `mxnf4` | 22 |
| `llama_decode_kivi` | `llama_decode_kivi` | Llama-3.1-8B | `mxnf4` | 23 |
| `llama2_7b_prefill` | `llama_prefill` | Llama-2-7b-hf | `mxnf4_attn_head_int6` | 16 |
| `llama2_7b_verify` | `llama_verify` (`--spec_length 14`) | Llama-2-7b-hf | `mxnf4_attn_head_int6` | 18 |
| `llama2_7b_decode_kivi` | `llama_decode_kivi` | Llama-2-7b-hf | `mxnf4_int6` | 19 |
| `amd135m_prefill` | `llama_prefill` | AMD-Llama-135m | `mxnf4_attn_head_int6` | 16 |
| `amd135m_decode_kivi` | `llama_decode_kivi` | AMD-Llama-135m | `mxnf4_int6` | 19 |
| `qwama_decode_kivi` | `llama_decode_kivi` | Qwama-0.5B-Instruct | `mxnf4_int6` | 23 |
| `llama31_8b_verify` | `llama_verify` (`--spec_length 14`) | Llama-3.1-8B | `mxnf4_attn_head_int6` | 22 |

Each kit has one ELF per layer (239 in all), `layers.txt` per network and the
board script at the zip root (`run_layers.py`, `run_power.py`). The graded kit
adds per layer `<layer>_scratchpad_dump.txt` (the regions to read back) and
`<layer>_scratchpad_expected.bin.<i>` (the expected bytes of region `i`).
Layers run with `MAX_TILES=2`, except the layers whose `layers.txt` end op is
`async_wait_*` (all tiles store from one scratchpad slot, so only the last
tile survives in the readback), which run with `MAX_TILES=1`.

## The commits

| Piece | Commit | Source |
| --- | --- | --- |
| SoC superrepo | `473ff85e` (parent of this branch) | this repo |
| `generators/voyager` (accelerator and SoC wrapper) | `0733cd8` (branch `voyager-compiler-migration`); the superrepo's gitlink `8be634b` has the same tree | `https://github.com/StanfordAccelerate/voyager.git` |
| `generators/voyager/voyager-compiler` | `34ceff2` (on `main`) | `https://github.com/jeffreyyu0602/voyager-compiler-2.git` |
| `generators/voyager/lib/{spdlog,xtensor,xtl}` | `3335c380`, `d9c3782e`, `d11fb6b5` (the gitlinks of `0733cd8`) | their GitHub repos |
| RISC-V toolchain | `riscv-tools 1.0.6` (ucb-bar conda, build `g56c29e0`), `riscv64-unknown-elf-gcc (gc891d8dc23e) 13.2.0`, the string in every kit ELF's `.comment` | conda env |
| Host compiler | Catapult 2024.2_2: its `g++ 9.2.0` and `shared/include` headers (HLS never runs) | site install |
| Python | 3.10.14, torch 2.12.1 (the PyPI wheel, `2.12.1+cu130`, run on CPU), transformers 5.13.0, conda `libprotobuf` 5.27.0 / `protoc` | `sphinx-repro/env/` |

The `.gitmodules` of `0733cd8` names `voyager-compiler.git`; `34ceff2` is on
`voyager-compiler-2.git`, so the compiler is cloned by hand below.

## Requirements

- Linux x86-64 with the Catapult 2024.2_2 install. `rebuild.sh` loads it with
  `modulecmd` (`catapult/2024.2_2`, plus the VCS GNU package for its
  runtime); at another site, edit those lines at the top of
  `generators/voyager/sphinx/firmware/rebuild.sh` so that `$CATAPULT_ROOT`
  points at the install, and replace the three hard-coded install paths
  (`CC`, the `shared/include` `-I`, the `shared/lib` `-L`) in
  `generators/voyager/test/soc/Makefile`, which builds `GenerateSoCBinaries`
  and `jtag_sim_checker`:
  `sed -i 's|/cad/mentor/2024.2_2/Mgc_home|<install>|' generators/voyager/test/soc/Makefile`.
  Nothing else from Catapult is used.
- A conda env with the packages of `sphinx-repro/env/`
  (`conda-explicit.txt`: `conda list --explicit --md5`; `pip-freeze.txt`:
  the pip packages, without the editable `voyager-compiler`).
- Hugging Face access to the gated `meta-llama` models (`hf auth login`,
  `huggingface_hub` 1.28.0).
  The verified build used these hub revisions:

  | repo | revision |
  | --- | --- |
  | `meta-llama/Llama-3.1-8B` | `d04e592bb4f6aa9cfee91e2e20afa771667e1d4b` |
  | `meta-llama/Llama-2-7b-hf` | `01c7f73d771dfac7d292323805ebc428287df4f9` |
  | `amd/AMD-Llama-135m` | `9a224a07b9b0210299d8c19b9e694ac56ed3ba1d` |
  | `turboderp/Qwama-0.5B-Instruct` | `fa73d1257f7681af595c86521b332fe26981f179` |
  | `wikitext` (dataset) | `b08601e04326c79dfdd32d625aee71d232d685c3` |

  The compiler loads `main`. If a repo's `main` has moved, download these
  revisions into the cache first and compile with `HF_HUB_OFFLINE=1`.
- Disk: about 24 GB for the conda env (`.conda-env`; 5 GB of it nvidia
  wheels), about 38 GB for the compiled params of all twelve networks
  (mostly `tensor_files`) and 1 GB for the firmware, so about 63 GB.
  `FREE_TENSORS=1` (below) keeps the params at one network's (5.6 GB at
  most): about 31 GB in all. pip's download cache (`~/.cache/pip`) needs a
  few GB more while the env is created.
- Time: per network, 2-25 min to compile and 1-15 min per kit to emit. The
  compile uses every core by default (torch threads); the result does not
  depend on the thread count (under `taskset -c 0-63` on a 128-core host,
  four networks reproduced their params exactly).

## 1. Checkout

```bash
git clone --branch sphinx-kit git@code.stanford.edu:tsmc7/sphinx/soc.git sphinx-soc
cd sphinx-soc
git submodule update --init generators/voyager
git -C generators/voyager checkout 0733cd8
git -C generators/voyager submodule update --init lib/spdlog lib/xtensor lib/xtl
git clone https://github.com/jeffreyyu0602/voyager-compiler-2.git \
    generators/voyager/voyager-compiler
git -C generators/voyager/voyager-compiler checkout 34ceff2
git -C generators/voyager apply ../../sphinx-repro/voyager-sphinx-kit.patch
```

Use whichever remote carries `sphinx-kit` (the GitHub mirror is
`git@github.com:jeffreyyu0602/sphinx.git`). No other submodule is needed (no
Chipyard setup, no toolchain build).
`voyager-sphinx-kit.patch` adds what the 2026-09-17 tree had but no commit
holds:

- `codegen.mk` targets for the seven networks beyond the five llama ones.
  They print exactly the compiler command lines logged on 2026-09-17: the
  `llama_prefill` flags with the graph, model and qconfig of the table above.
- `sphinx/`: the board scripts `run_layers.py`, `run_power.py`, `hello.gdb`,
  `command.txt`; `firmware/firmware.py` and `firmware/env.sh`, the script that
  built the kit on 2026-09-17 (kept as the record; it drives
  `run_voyager.py`, which needs a SphinxConfig simv); and
  `firmware/rebuild.sh`, the rebuild used here.

## 2. Environment

```bash
conda create -y -p $PWD/.conda-env --file sphinx-repro/env/conda-explicit.txt
conda activate $PWD/.conda-env
pip install -r sphinx-repro/env/pip-freeze.txt
printf 'source "$(conda info --base)/etc/profile.d/conda.sh"\nconda activate %s\n' \
    "$PWD/.conda-env" > env.sh
conda list --explicit --md5 | diff - sphinx-repro/env/conda-explicit.txt
pip list --format=freeze --exclude-editable | diff - sphinx-repro/env/pip-freeze.txt
```

Run `pip` only after `conda create`, in that order. `pip-freeze.txt` is the
full `pip list` of the verified env, so it also pins packages conda
installed that are not on PyPI (`awscli==2.17.23`, `conda==23.9.0`, ...);
pip finds those already installed and skips them. It also holds both the
cu12 and the cu13 nvidia stacks (about 5 GB together) because the verified
env did; torch `2.12.1+cu130` needs only the cu13 one, but the list is kept
as the env that was verified. The two `diff`s check the env (the first may
differ only in its `# created-by` header line).

`rebuild.sh` sources `env.sh` at the clone root (or `$ENV_SH`), then sets
everything else itself: `PYTHONPATH` to the checked-out compiler (so a
`voyager_compiler` installed elsewhere is not imported), the Sphinx run
environment (`MEMORY_SUFFIX=u_ram_core.memory BANK_WIDTH=32 MACRO_WIDTH=16
SCRATCHPAD_SIZE=2097152 NUM_BANKS=16 SCRATCHPAD_OFFSET=262144
CLOCK_PERIOD=0.9 VOYAGER_INT_ID=1 LINKER_SCRIPT=../htif.ld CONV2D_IM2COL=1`,
`MACRO_DEPTH` unset), the SphinxConfig accelerator parameters (`MXNF4`,
64x64, buffers 512/576/256, double-buffered accumulation, MVM and SpMM on)
and what `run_voyager.py --jtag-mode full` exports for the emitter
(`JTAG_SIM=1`, `SOC_MEM_OFFSET=262144`, `CACHE_SIZE=917504`). Host objects go
to `generators/voyager/build/sphinx-repro/` (`$BUILD_DIR`).

## 3. Rebuild and check

```bash
bash sphinx-repro/reproduce.sh                      # all twelve networks
FREE_TENSORS=1 bash sphinx-repro/reproduce.sh amd135m_prefill llama_decode
```

Per network, one after another, `reproduce.sh` runs:

1. `rebuild.sh compile <net>`: `make network-proto` with `CLOCK_PERIOD=1.0`
   (the params were priced at 1 GHz) into
   `generators/voyager/test/compiler/networks/<net>/MXNF4/` (`model.txt`,
   `layers.txt`, `tensor_files/`), and `compiler.txt` = `git describe` of the
   compiler (`34ceff2`). A network whose `model.txt` exists is not
   recompiled: move the directory aside to recompile it.
2. `check.sh params <net>`: `model.txt`, `layers.txt`, `compiler.txt` and
   every tensor file against `md5/params.md5` and `md5/tensors.md5`.
3. `rebuild.sh emit graded <net>`, then `rebuild.sh emit power <net>`
   (`POWER_LOOP=1`): `GenerateSoCBinaries` emits each kit layer's firmware
   and data image into
   `generators/voyager/test/soc/firmware/networks/<net>/MXNF4/64x64_512x576x256_true_true_true/`
   and `tests/voyager/Makefile` links `<layer>.riscv`. The layer set is what
   `run_voyager.py --uniquify_layers --skip_layers` selects
   (`regression_common.add_layers`, `ci_skip_rules.json`), split into the
   `MAX_TILES=2` and `MAX_TILES=1` groups. Both kinds write the same paths, so
   each is staged into `sphinx-repro/out/<graded|power>/` (the zip layout)
   before the next.
4. `check.sh kit graded|power <net>`: every staged file against
   `md5/kit-graded.md5` / `md5/kit-power.md5`, which list every member of the
   two zips; a staged file the kit lacks is an error too.

Each check prints `<n> of <n> identical` per network and exits non-zero on
any difference; logs are in `sphinx-repro/out/logs/`. Run the emissions one
at a time (they share the firmware objects); compiles of different networks
may run in parallel (`bash generators/voyager/sphinx/firmware/rebuild.sh
compile <net>`), after which `reproduce.sh` skips them. Do not use
`run_voyager.py --build-only` in a fresh tree: it still runs `make default`,
which elaborates the SoC and calls Catapult HLS.

**Layers the kit leaves out.** At these commits `GenerateSoCBinaries`
rejects eight selected layers, as it did on 2026-09-17, so the kit omits
them: `model_model_layers_0_post_attention_layernorm_fused` of
`llama2_7b_decode_kivi`, `amd135m_decode_kivi` and `qwama_decode_kivi`;
`matmul_3_fused`, `matmul_4` and `where_self_1` of `llama2_7b_verify`;
`matmul_3_fused` and `matmul_4_fused` of `llama31_8b_verify`. For those five
networks the emit log ends in `make: *** [GenerateSoCBinaries] Error 1` and
`NOT EMITTED: <net> <layer>` lines; that is expected, and `check.sh` still
finds every kit file.

**Not reproduced, by design.** Five expected readbacks in the graded zip
(`md5/not-reproduced.txt`) are leftovers of an earlier emission that
`firmware.py` globbed in: each is `.bin.2` of a layer whose dump list has two
regions, so nothing reads it, and a clean emission does not write it. The zip
archives themselves are not rebuilt (`firmware.py` zipped the staged kits);
their members are, and `md5/zips.md5` checks the committed archives.

## 4. Run on the chip

On the PC with the J-Link: unzip `sphinx-tests.zip` (or use the staged
`sphinx-repro/out/graded/`), start `openocd -f openocd.cfg` (J-Link,
`irlen 5`, IDCODE `0x00000001`, `arm semihosting enable`), then

```bash
python run_layers.py --dir networks --out results                        # every network
python run_layers.py --dir networks --networks llama_decode,llama_decode_kivi
python run_layers.py --dir networks --networks llama_prefill --layers neg,mul_5
```

`run_layers.py` resets, loads, runs to `_exit`, reads the accelerator cycle
counters (MMIO `0x20000008/108/208/308/408`, sampled before `load`: the debug
module's `reset halt` does not clear them, so it reports differences) and
dumps each region of the dump list, then compares each dump byte for byte
with the `_scratchpad_expected.bin.<i>` beside the ELF (a bf16 signed zero is
not an error) and writes per-network and overall `summary.csv`.
`generators/voyager/sphinx/hello.gdb` is a load-and-run smoke test. Power:
the `sphinx-power.zip` ELFs loop forever; `run_power.py start|sample|stop`
drives them.

To regrade dumps on a host instead, build the checker in the `rebuild.sh`
environment and run it on the dump files (`<prefix>.bin.0`, `.bin.1`, ...)
with the dump list of the same emission and `MAX_TILES=1` for the one-tile
layers. It reads the network's compiled params, tensors included (step 3
without `FREE_TENSORS`):

```bash
WT=$PWD bash -c 'source generators/voyager/sphinx/firmware/rebuild.sh none
    make -C generators/voyager/test/soc jtag_sim_checker
    D=generators/voyager/test/soc/firmware/networks/<net>/MXNF4/64x64_512x576x256_true_true_true
    MAX_TILES=2 NETWORK=<net> TESTS=<layer> DATATYPE=MXNF4 \
    SCRATCHPAD_DUMP_LIST=$PWD/$D/<layer>_scratchpad_dump.txt \
        $BUILD_DIR/cc/jtag_sim_checker <dump-prefix>'
```

`rebuild.sh` derives the clone root from its own path, which a sourced
script does not know, hence `WT=$PWD`. The checker writes its comparison
files (`gold_vs_accelerator` text) into `test_outputs/` in the current
directory, here the clone root, where git shows it as untracked; delete it
after reading.

## 5. RTL simulation (optional, not re-run)

Full JTAG mode in RTL needs a SphinxConfig simv: this superrepo with the
Chipyard submodules initialised, `sphinx-repro/soc-hyperram.patch` applied in
`generators/hyperram` and `sphinx-repro/soc-rocket-chip.patch` in
`generators/rocket-chip` (local edits the tapeout tree carried, in no
commit), and the cached tsmc7 HLS RTL under
`generators/voyager/build/MXNF4_64x64_512x576x256_true_true_true/Catapult/tsmc7/clock_0.9`,
which cannot be regenerated without the `internal-tech` submodule (keep it
newer than `src/*.h`, or a stale-looking cache re-runs HLS and fails). Then,
from `sims/vcs`: `MAX_TILES=2 python run_voyager.py --models <net> --config
SphinxConfig --num_processes 40 --uniquify_layers --skip_layers --jtag-mode
full`; pass is `Error count: 0` in each `_checker.log`.
`sphinx-repro/voyager-build-only.patch` adds the `run_voyager.py
--build-only` option `firmware.py` calls; it does not touch emission.

## Verification record

Rebuilt 2026-10-31 on the tsmc7 build server (Catapult 2024.2_2, the conda
env of `sphinx-repro/env/`, the Hugging Face revisions above):

- **All twelve networks reproduce byte for byte.** Compiled params:
  `model.txt`, `layers.txt`, `compiler.txt` and all 4,173 tensor files equal
  the 2026-09-17 ones. Kits: 239/239 ELFs in each kit (raw md5, no
  stripping), every dump list and every expected readback but the five stale
  ones, every `layers.txt` and both run scripts; 790 of the graded zip's 795
  members and all 252 of the power zip's.
- The five llama networks were rebuilt in two independent checkouts of these
  commits (git worktrees, with this patch before it gained the `codegen.mk`
  targets, which do not touch the five). The seven others were rebuilt in a
  fresh clone of this branch, submodules fetched from GitHub, by steps 1-3;
  the compiler commands `codegen.mk` printed for them equal the 2026-09-17
  logged ones character for character. `reproduce.sh` itself was run end to
  end on `amd135m_decode_kivi`.
- The regrade command of step 4, fed the kit's expected readbacks as dumps,
  reports `Error count: 0`.
- A second fresh clone of this branch, following this file as written
  (step 2 against the verified env, after both `diff`s passed), rebuilt
  `llama_prefill`, `llama_prefill_mp`, `amd135m_prefill` and
  `amd135m_decode_kivi` byte for byte (params, both kits) under
  `taskset -c 0-63`; the other eight were not re-run there for lack of
  disk.
- Not tested: recreating the conda env from `sphinx-repro/env/` (the build
  used the env that built the kit: its conda history ends 2026-08-24, and
  only `black` and `py-spy` were pip-installed since), a host other than
  tsmc7, and running the rebuilt ELFs (they are the kit's files, which ran on
  the chip).

## Provenance

- The 2026-09-17 params were compiled by `firmware.py` through `make
  network-proto` (`compiler.txt` `34ceff2`, clean) in a tree at voyager
  `0733cd8`, dirty only in the compiler pointer (`0733cd8` records
  `f9d4c49`) and in the uncommitted `codegen.mk` targets of the seven extra
  networks, which the patch restores from the logged command lines. The kits
  were emitted and zipped the same day with the `--build-only` hunk of
  `voyager-build-only.patch` uncommitted.
- The superrepo commit the tree had checked out then, `0b08d714`, survives
  only in a local reflog; `473ff85e` is its amend and differs from it only
  in the `generators/voyager` pointer.
- Earlier silicon and simulation results (2026-09-06 silicon run of
  `llama_prefill`, 18/18 bit-exact; the 2026-09-07 full-mode campaign,
  208/208) used older firmware that was not preserved and is superseded by
  this kit.

## Files

| path | what |
| --- | --- |
| `SPHINX_REPRO.md` | this file |
| `sphinx-repro/kits/sphinx-tests.zip`, `sphinx-power.zip` | the 2026-09-17 kits |
| `sphinx-repro/voyager-sphinx-kit.patch` | apply in `generators/voyager` (step 1) |
| `sphinx-repro/reproduce.sh`, `check.sh` | rebuild and check (step 3) |
| `sphinx-repro/md5/params.md5`, `tensors.md5` | the 2026-09-17 compiled params, twelve networks |
| `sphinx-repro/md5/kit-graded.md5`, `kit-power.md5` | every zip member |
| `sphinx-repro/md5/not-reproduced.txt` | the five stale zip members |
| `sphinx-repro/md5/zips.md5` | the two archives |
| `sphinx-repro/env/conda-explicit.txt`, `pip-freeze.txt` | the conda env of the verified build |
| `sphinx-repro/soc-hyperram.patch`, `soc-rocket-chip.patch` | RTL simulation only (step 5) |
| `sphinx-repro/voyager-build-only.patch` | optional, `run_voyager.py --build-only` |
