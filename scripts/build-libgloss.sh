#!/usr/bin/env bash

# exit script if any command fails
set -e
set -o pipefail

RDIR=$(git rev-parse --show-toplevel)

# get helpful utilities
source $RDIR/scripts/utils.sh

common_setup

# Allow user to override MAKE
[ -n "${MAKE:+x}" ] || MAKE=$(command -v gnumake || command -v gmake || command -v make)
readonly MAKE

usage() {
    echo "usage: ${0} [OPTIONS] [riscv-tools]"
    echo ""
    echo "Installation Types"
    echo "   riscv-tools: if set, builds the riscv toolchain (this is also the default)"
    echo ""
    echo "Options"
    echo "   --prefix -p PREFIX    : Install destination."
    echo "   --clean-after-install : Run make clean in calls to module_make and module_build"
    echo "   --help -h             : Display this message"
    exit "$1"
}

TOOLCHAIN="riscv-tools"
CLEANAFTERINSTALL=""
RISCV=""
FORCE=false

# getopts does not support long options, and is inflexible
while [ "$1" != "" ];
do
    case $1 in
        -h | -H | --help | help )
            usage 3 ;;
        -p | --prefix )
            shift
            RISCV=$(realpath $1) ;;
        --clean-after-install )
            CLEANAFTERINSTALL="true" ;;
        riscv-tools )
            TOOLCHAIN=$1 ;;
        * )
            error "invalid option $1"
            usage 1 ;;
    esac
    shift
done

if [ -z "$RISCV" ] ; then
    error "ERROR: Prefix not given. If conda is sourced, do you mean $CONDA_PREFIX/$TOOLCHAIN?"
fi

XLEN=64

export RISCV="$RISCV"

cd "${RDIR}"

SRCDIR="$(pwd)/toolchains/${TOOLCHAIN}"
[ -d "${SRCDIR}" ] || die "unsupported toolchain: ${TOOLCHAIN}"
. ./scripts/build-util.sh

# Common tools (not in any particular toolchain dir)

echo '==>  Installing libgloss'
CC= CXX= SRCDIR="$(pwd)/toolchains" module_all libgloss --prefix="${RISCV}/riscv${XLEN}-unknown-elf" --host=riscv${XLEN}-unknown-elf

echo "libgloss Build Complete!"
