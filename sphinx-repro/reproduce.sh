#!/bin/bash
# Rebuild the 2026-09-17 Sphinx chip kits and check them against the
# committed md5 lists (SPHINX_REPRO.md).
#
#   bash sphinx-repro/reproduce.sh [net ...]     default: all twelve
#
# Per network, one at a time: compile the params (codegen.mk skips this when
# model.txt exists), check them, emit and link the graded kit and then the
# POWER_LOOP kit, staging each into sphinx-repro/out/<graded|power>/ in the
# zip layout, and check both. Logs go to sphinx-repro/out/logs/. With
# FREE_TENSORS=1 a network's tensor_files are deleted once its kits are
# staged (all twelve networks hold about 38 GB). ENV_SH: as in rebuild.sh.
X=$(cd "$(dirname "$0")" && pwd)
R=$(dirname "$X")
VOY=$R/generators/voyager
S=$VOY/sphinx/firmware/rebuild.sh
CFG=MXNF4/64x64_512x576x256_true_true_true
NETS="llama_prefill llama_prefill_mp llama_prefill_spmm llama_decode
      llama_decode_kivi llama2_7b_prefill llama2_7b_verify
      llama2_7b_decode_kivi amd135m_prefill amd135m_decode_kivi
      qwama_decode_kivi llama31_8b_verify"
[ $# -gt 0 ] && NETS="$*"
L=$X/out/logs
mkdir -p "$L" "$X/out/graded" "$X/out/power"
cp "$VOY/sphinx/run_layers.py" "$X/out/graded/"
cp "$VOY/sphinx/run_power.py" "$X/out/power/"

rc=0
for n in $NETS; do
    echo "compile $n ($(date +%T))"
    if ! bash "$S" compile "$n" > "$L/compile-$n.log" 2>&1; then
        echo "compile $n FAILED, see $L/compile-$n.log"
        rc=1
        continue
    fi
    bash "$X/check.sh" params "$n" || rc=1
    params=$VOY/test/compiler/networks/$n/MXNF4
    emitted=$VOY/test/soc/firmware/networks/$n/$CFG
    for kind in graded power; do
        echo "emit $kind $n ($(date +%T))"
        bash "$S" emit $kind "$n" > "$L/emit-$kind-$n.log" 2>&1
        dst=$X/out/$kind/networks/$n/$CFG
        rm -rf "$dst"
        mkdir -p "$dst"
        cp "$params/layers.txt" "$emitted"/*.riscv "$dst/"
        if [ $kind = graded ]; then
            cp "$emitted"/*_scratchpad_dump.txt \
                "$emitted"/*_scratchpad_expected.bin.* "$dst/"
        fi
        bash "$X/check.sh" kit $kind "$n" || rc=1
    done
    [ "${FREE_TENSORS:-0}" = 1 ] && rm -rf "$params/tensor_files"
done
echo "done ($(date +%T)), rc=$rc"
exit $rc
