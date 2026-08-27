cmake_minimum_required(VERSION 3.20)

cmake_policy(SET CMP0091 NEW)
set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")

project(com4j_native LANGUAGES C CXX ASM_MASM)

if(NOT MSVC)
  message(FATAL_ERROR "This project only builds with MSVC on Windows.")
endif()

if(CMAKE_SIZEOF_VOID_P EQUAL 8)
  set(COM4J_ARCH_DEFINE X86_WIN64)
  set(COM4J_PLATFORM_DEFINE WIN64)
  set(LIBFFI_ASM ${CMAKE_CURRENT_SOURCE_DIR}/../libffi/src/x86/win64.asm64)
  set(MIDL_ENV win64)
else()
  set(COM4J_ARCH_DEFINE X86_WIN32)
  set(COM4J_PLATFORM_DEFINE WIN32)
  set(LIBFFI_ASM ${CMAKE_CURRENT_SOURCE_DIR}/../libffi/src/x86/win32.asm)
  set(MIDL_ENV win32)
endif()

set_source_files_properties(${LIBFFI_ASM} PROPERTIES LANGUAGE ASM_MASM)

# ---------------------------------------------------------------------------
# JNI
# ---------------------------------------------------------------------------
if(DEFINED JAVA_HOME)
  set(ENV{JAVA_HOME} ${JAVA_HOME})
endif()
find_package(JNI REQUIRED)

# ---------------------------------------------------------------------------
# jnitl (git submodule, ../jnitl) - built as a small static lib.
# native/stdafx.h pulls in jnitl.h, which does:
#   #pragma comment(lib, "jnitl.lib")      (Release, static CRT)
#   #pragma comment(lib, "jnitld.lib")     (Debug, static CRT)
# so name the output to match and make sure it's on the link path.
# ---------------------------------------------------------------------------
set(JNITL_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../jnitl)

add_library(jnitl STATIC
  ${JNITL_DIR}/source/jnitl.cpp
  ${JNITL_DIR}/source/accessor.cpp
)
target_include_directories(jnitl PUBLIC ${JNITL_DIR}/include)
target_include_directories(jnitl PRIVATE ${JNI_INCLUDE_DIRS})
set_target_properties(jnitl PROPERTIES
  OUTPUT_NAME "jnitl"
  DEBUG_POSTFIX "d"
  ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/jnitl-lib
)
target_compile_definitions(jnitl PRIVATE WIN32 ${COM4J_PLATFORM_DEFINE} UNICODE _UNICODE)
# original vcproj built with TreatWChar_tAsBuiltInType="false" (/Zc:wchar_t-),
# which is why jni.h's `jchar` (unsigned short) and `wchar_t*`/BSTR interconvert
# implicitly all over this codebase.
target_compile_options(jnitl PRIVATE /Zc:wchar_t-)

# ---------------------------------------------------------------------------
# MIDL: com4j.idl -> com4j.tlb (+ generated .h/.c, unused but let midl emit
# its normal output). typelib.cpp does `#import "com4j.tlb" no_namespace`
# and com4j.rc embeds it as a TYPELIB resource, so both need the tlb's
# directory on their include search path.
# ---------------------------------------------------------------------------
if(CMAKE_SIZEOF_VOID_P EQUAL 8)
  set(MIDL_SDK_ARCH x64)
else()
  set(MIDL_SDK_ARCH x86)
endif()
file(GLOB MIDL_SDK_HINTS "C:/Program Files (x86)/Windows Kits/10/bin/10.0.*/${MIDL_SDK_ARCH}")
list(SORT MIDL_SDK_HINTS ORDER DESCENDING)
find_program(MIDL_EXECUTABLE midl.exe HINTS ${MIDL_SDK_HINTS} REQUIRED)

set(COM4J_TLB ${CMAKE_CURRENT_BINARY_DIR}/com4j.tlb)
set(COM4J_MIDL_H ${CMAKE_CURRENT_BINARY_DIR}/com4j_midl.h)
set(COM4J_MIDL_C ${CMAKE_CURRENT_BINARY_DIR}/com4j_midl_i.c)

add_custom_command(
  OUTPUT ${COM4J_TLB}
  COMMAND ${MIDL_EXECUTABLE}
          /nologo
          /env ${MIDL_ENV}
          /tlb ${COM4J_TLB}
          /h ${COM4J_MIDL_H}
          /iid ${COM4J_MIDL_C}
          /out ${CMAKE_CURRENT_BINARY_DIR}
          ${CMAKE_CURRENT_SOURCE_DIR}/com4j.idl
  DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/com4j.idl
  WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
  COMMENT "Running MIDL on com4j.idl"
  VERBATIM
)
add_custom_target(com4j_idl DEPENDS ${COM4J_TLB})

# ---------------------------------------------------------------------------
# com4j.dll
# ---------------------------------------------------------------------------
set(COM4J_SOURCES
  com4j.cpp
  com4j_Win32Lock.cpp
  error.cpp
  eventReceiver.cpp
  invoke.cpp
  java_id.cpp
  registry.cpp
  safearray.cpp
  stdafx.cpp
  TestObject.cpp
  toJava.cpp
  typelib.cpp
  variant.cpp
)

set(LIBFFI_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../libffi)
set(LIBFFI_SOURCES
  ${LIBFFI_DIR}/src/debug.c
  ${LIBFFI_DIR}/src/x86/ffi.c
  ${LIBFFI_DIR}/src/prep_cif.c
  ${LIBFFI_DIR}/src/types.c
  ${LIBFFI_ASM}
)

add_library(com4j SHARED
  ${COM4J_SOURCES}
  ${LIBFFI_SOURCES}
  com4j.rc
)

add_dependencies(com4j com4j_idl)

target_include_directories(com4j PRIVATE
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${CMAKE_CURRENT_BINARY_DIR}
  ${JNITL_DIR}/include
  ${LIBFFI_DIR}/include
  ${JNI_INCLUDE_DIRS}
)

target_compile_definitions(com4j PRIVATE
  ${COM4J_PLATFORM_DEFINE}
  _WINDOWS
  _USRDLL
  COM4J_EXPORTS
  ${COM4J_ARCH_DEFINE}
  UNICODE
  _UNICODE
  $<$<CONFIG:Debug>:_DEBUG>
  $<$<NOT:$<CONFIG:Debug>>:NDEBUG>
)
target_compile_options(com4j PRIVATE $<$<COMPILE_LANGUAGE:CXX,C>:/Zc:wchar_t->)

# jnitl.h does `#pragma comment(lib, "jnitl.lib"/"jnitld.lib")` - that pragma-driven
# search needs the jnitl target's actual (per-config) output dir on the /LIBPATH,
# in addition to linking it directly as a CMake target dependency.
target_link_directories(com4j PRIVATE $<TARGET_FILE_DIR:jnitl>)
target_link_libraries(com4j PRIVATE jnitl ole32 oleaut32 uuid)

set(COM4J_LINK_FLAGS "/DEF:${CMAKE_CURRENT_SOURCE_DIR}/com4j.def")
if(NOT CMAKE_SIZEOF_VOID_P EQUAL 8)
  # libffi's hand-written win32.asm predates SAFESEH exception-handler records;
  # the modern linker refuses to produce a SAFESEH image without this.
  set(COM4J_LINK_FLAGS "${COM4J_LINK_FLAGS} /SAFESEH:NO")
endif()
set_target_properties(com4j PROPERTIES
  LINK_FLAGS "${COM4J_LINK_FLAGS}"
)

# make sure the rc compile step can find com4j.tlb (embedded via `1 TYPELIB "com4j.tlb"`)
set_source_files_properties(com4j.rc PROPERTIES
  COMPILE_OPTIONS "/I${CMAKE_CURRENT_BINARY_DIR}"
)

# TestObject.cpp compiles to nothing outside _DEBUG - fine to always include.
