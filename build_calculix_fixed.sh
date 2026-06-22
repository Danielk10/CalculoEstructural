#!/bin/bash

# Termux Build Script for CalculiX 2.23 (Android 15+ 16KB Alignment)

set -e

# Config
export TMX_PREFIX=$PREFIX
export FAKE_LIB=$HOME/fake_root/data/data/com.diamon.civil/files/usr/lib
export FAKE_INC=$HOME/fake_root/data/data/com.diamon.civil/files/usr/include

# Android 15+ 16KB alignment flags
export CFLAGS="-fPIC -fPIE -Oz -I$FAKE_INC -I$FAKE_INC/spooles -fopenmp -Wall -DARCH=\"Linux\" -DSPOOLES -DARPACK -DMATRIXSTORAGE -DNETWORKOUT"
export LDFLAGS="-pie -Wl,-z,max-page-size=16384"
export FCFLAGS="-fPIC -fPIE -Oz"

export CC=clang
export FC=gfortran

# Workspace
WORKDIR=$HOME/calculix_build
cd $WORKDIR/CalculiX/ccx_2.23/src

echo "Cleaning up..."
make clean || true

# Makefile Patching
echo "Updating Makefile for 16KB alignment..."

cat << EOF > Makefile
CFLAGS = $CFLAGS
FFLAGS = $FCFLAGS -cpp
LDFLAGS = $LDFLAGS

CC=$CC
FC=$FC

.c.o :
	\$(CC) \$(CFLAGS) -c \$<
.f.o :
	\$(FC) \$(FFLAGS) -c \$<

include Makefile.inc

SCCXMAIN = ccx_2.23.c

OCCXF = \$(SCCXF:.f=.o)
OCCXC = \$(SCCXC:.c=.o)
OCCXMAIN = \$(SCCXMAIN:.c=.o)

LIBS = $FAKE_LIB/libspooles.a $FAKE_LIB/libarpack.a -lopenblas -lpthread -lm -lc

ccx_2.23: \$(OCCXMAIN) ccx_2.23.a
	\$(CC) \$(CFLAGS) -c ccx_2.23.c; \$(FC) \$(FFLAGS) \$(LDFLAGS) -o \$@ \$(OCCXMAIN) ccx_2.23.a \$(LIBS) -fopenmp

ccx_2.23.a: \$(OCCXF) \$(OCCXC)
	ar vr \$@ \$?
EOF

echo "Makefile created. Compiling..."
make -j4 ccx_2.23

echo "Build complete."
