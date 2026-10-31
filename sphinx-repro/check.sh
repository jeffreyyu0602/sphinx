#!/bin/bash
# Compare rebuilt Sphinx artifacts with the 2026-09-17 md5 lists.
#
#   check.sh params <net>...               compiled params: model.txt,
#                                          layers.txt, compiler.txt, tensors
#   check.sh kit <graded|power> <net>...   kit files staged by reproduce.sh,
#                                          plus the run script at its root
#
# Paths in md5/not-reproduced.txt are skipped (SPHINX_REPRO.md says why).
# Prints one line per network and exits non-zero if a file differs, is
# missing, or (kit) is staged but not in the kit.
X=$(cd "$(dirname "$0")" && pwd)
R=$(dirname "$X")
CFG=MXNF4/64x64_512x576x256_true_true_true

what=$1
shift
case $what in
params)
    dir=$R
    lists="$X/md5/params.md5 $X/md5/tensors.md5"
    pre=generators/voyager/test/compiler/networks
    ;;
kit)
    kind=$1
    shift
    dir=$X/out/$kind
    lists=$X/md5/kit-$kind.md5
    pre=networks
    ;;
*)
    echo "usage: check.sh params <net>... | kit <graded|power> <net>..."
    exit 2
    ;;
esac

rc=0
all=$(mktemp)
sel=$(mktemp)

# check <label> <staged dir or ""> : md5 lines in $all, minus skipped paths
check() {
    grep -v -F -f "$X/md5/not-reproduced.txt" "$all" > "$sel"
    local total bad nbad=0 extra nextra=0
    total=$(wc -l < "$sel")
    bad=$(cd "$dir" && md5sum -c "$sel" 2> /dev/null | grep -v ': OK$')
    [ -n "$bad" ] && nbad=$(wc -l <<< "$bad") && echo "$bad"
    if [ -n "$2" ] && [ -d "$dir/$2" ]; then
        extra=$(cd "$dir" && find "$2" -type f | sort \
            | grep -v -x -F -f <(sed 's/^[0-9a-f]*  //' "$all"))
        [ -n "$extra" ] && nextra=$(wc -l <<< "$extra") \
            && sed 's/^/EXTRA /' <<< "$extra"
    fi
    if [ "$total" -eq 0 ] || [ $nbad -gt 0 ] || [ $nextra -gt 0 ]; then
        rc=1
        echo "$1: $nbad of $total differ or missing, $nextra extra FAIL"
    else
        echo "$1: $total of $total identical"
    fi
}

for n in "$@"; do
    cat $lists | grep -F "  $pre/$n/MXNF4/" > "$all"
    if [ $what = kit ]; then
        check "kit $kind $n" "$pre/$n/$CFG"
    else
        check "params $n" ""
    fi
done
if [ $what = kit ]; then
    grep -E '^[0-9a-f]{32}  [^/]+$' "$lists" > "$all"
    check "kit $kind root" ""
fi
rm -f "$all" "$sel"
exit $rc
