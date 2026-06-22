#!/bin/bash
# Script envoltorio para ejecutar gmsh con la ruta de librerías correcta
LIB_DIR="$(pwd)/fake_root_o_g/data/data/com.diamon.civil/files/usr/lib"
GMSH_BIN="$(pwd)/fake_root_o_g/data/data/com.diamon.civil/files/usr/bin/gmsh"

export LD_LIBRARY_PATH="$LIB_DIR:$LD_LIBRARY_PATH"

"$GMSH_BIN" "$@"
