#!/bin/bash
set -e

cd $HOME

export APP_PREFIX=/data/data/com.diamon.civil/files/usr
export DESTDIR=$HOME/fake_root
export FAKE_USR=$DESTDIR$APP_PREFIX
export TMX_PREFIX=/data/data/com.termux/files/usr

export CC=clang
export CXX=clang++

# Banderas estrictas para Android PIE y alineación 16KB
export COMMON_CFLAGS="-fPIC -fPIE -Oz -I$FAKE_USR/include -I$TMX_PREFIX/include"
export COMMON_CXXFLAGS="-fPIC -fPIE -Oz -I$FAKE_USR/include -I$TMX_PREFIX/include"
export LDFLAGS="-pie -Wl,-z,max-page-size=16384 -L$FAKE_USR/lib -L$TMX_PREFIX/lib"

echo "Descargando OpenCASCADE 8.0.0 (Código fuente desde GitHub)..."
rm -rf $HOME/occt

# Descargamos el tarball de la versión V8_0_0 directamente desde GitHub
wget -qO- "https://github.com/Open-Cascade-SAS/OCCT/archive/refs/tags/V8_0_0.tar.gz" | tar -xzf -

# Renombrar la carpeta extraída para facilidad
mv OCCT-* occt
cd occt

echo "Configurando OpenCASCADE con CMake para Termux..."
mkdir -p build
cd build
rm -rf ./*

# Configuramos CMake:
# - BUILD_LIBRARY_TYPE="Shared": Para generar librerías .so
# - USE_FREETYPE=OFF, USE_FREEIMAGE=OFF, USE_GLX=OFF, USE_OPENGL=OFF: Deshabilitar visualización.
cmake .. \
  -DCMAKE_INSTALL_PREFIX="$APP_PREFIX" \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_C_COMPILER="$CC" \
  -DCMAKE_CXX_COMPILER="$CXX" \
  -DCMAKE_C_FLAGS="$COMMON_CFLAGS" \
  -DCMAKE_CXX_FLAGS="$COMMON_CXXFLAGS" \
  -DCMAKE_SHARED_LINKER_FLAGS="$LDFLAGS" \
  -DCMAKE_EXE_LINKER_FLAGS="$LDFLAGS" \
  -DBUILD_LIBRARY_TYPE="Shared" \
  -DUSE_FREETYPE=OFF \
  -DUSE_FREEIMAGE=OFF \
  -DUSE_GLX=OFF \
  -DUSE_OPENGL=OFF \
  -DUSE_GLES2=OFF \
  -DBUILD_MODULE_Draw=OFF \
  -DBUILD_MODULE_Visualization=OFF \
  -DBUILD_DOC_Overview=OFF \
  -DBUILD_SAMPLES_QT=OFF \
  -DBUILD_SAMPLES_MFC=OFF

echo "Compilando OpenCASCADE (Esto tomará mucho tiempo)..."
cmake --build . --parallel $(($(nproc) - 1))

echo "Instalando en fake_root..."
DESTDIR="$DESTDIR" cmake --install .

echo "=== Compilación de OpenCASCADE Exitosa ==="
ls -lh "$FAKE_USR/lib/" | grep TK
