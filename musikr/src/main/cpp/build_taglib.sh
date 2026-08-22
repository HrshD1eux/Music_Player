set -e
WORKING_DIR=$1
echo "Working directory is at $WORKING_DIR"
cd "$WORKING_DIR"

TAGLIB_SRC_DIR=${WORKING_DIR}/taglib
TAGLIB_DST_DIR=${WORKING_DIR}/taglib/build
TAGLIB_PKG_DIR=${WORKING_DIR}/taglib/pkg
NDK_TOOLCHAIN=${WORKING_DIR}/android.toolchain.cmake
NDK_PATH=$2
SDK_CMAKE_DIR=${3:-}
if [[ -n "$SDK_CMAKE_DIR" && -d "$SDK_CMAKE_DIR" ]]; then
  export PATH="$SDK_CMAKE_DIR:$PATH"
fi
echo "Taglib source is at $TAGLIB_SRC_DIR"
echo "Taglib build is at $TAGLIB_DST_DIR"
echo "Taglib package is at $TAGLIB_PKG_DIR"
echo "NDK toolchain is at $NDK_TOOLCHAIN"
echo "NDK path is at $NDK_PATH"

X86_ARCH=x86
X86_64_ARCH=x86_64
ARMV7_ARCH=armeabi-v7a
ARMV8_ARCH=arm64-v8a

build_for_arch() {
  local ARCH=$1
  local DST_DIR="$TAGLIB_DST_DIR/$ARCH"
  local PKG_DIR="$TAGLIB_PKG_DIR/$ARCH"

  cd "$TAGLIB_SRC_DIR"
  cmake -G Ninja -B "$DST_DIR" \
    -DCMAKE_MAKE_PROGRAM="${SDK_CMAKE_DIR}/ninja.exe" \
    -DANDROID_NDK_PATH="${NDK_PATH}" -DCMAKE_TOOLCHAIN_FILE="${NDK_TOOLCHAIN}"  \
    -DANDROID_ABI=$ARCH -DANDROID_NDK="${NDK_PATH}" \
    -DBUILD_SHARED_LIBS=OFF -DVISIBILITY_HIDDEN=ON -DBUILD_TESTING=OFF \
    -DBUILD_EXAMPLES=OFF -DBUILD_BINDINGS=OFF -DWITH_ZLIB=OFF -DCMAKE_BUILD_TYPE=Release \
    -DWITH_APE=OFF -DWITH_ASF=OFF -DWITH_ASF=OFF -DWITH_MOD=OFF -DWITH_SHORTEN=OFF \
    -DWITH_TRUEAUDIO=OFF -DCMAKE_CXX_FLAGS="-fPIC"
  # Try to parallelize the build
  cmake --build "$DST_DIR" --config Release -j$(nproc 2> /dev/null || echo 4)
  cd "$WORKING_DIR"

  cmake --install "$DST_DIR" --config Release --prefix "$PKG_DIR" --strip
}

build_for_arch $X86_ARCH
build_for_arch $X86_64_ARCH
build_for_arch $ARMV7_ARCH
build_for_arch $ARMV8_ARCH
