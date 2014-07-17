/* -----------------------------------------------------------------*-C-*-
   libffi 3.0.8 - Copyright (c) 1996-2003, 2007, 2008  Red Hat, Inc.

   Permission is hereby granted, free of charge, to any person obtaining
   a copy of this software and associated documentation files (the
   ``Software''), to deal in the Software without restriction, including
   without limitation the rights to use, copy, modify, merge, publish,
   distribute, sublicense, and/or sell copies of the Software, and to
   permit persons to whom the Software is furnished to do so, subject to
   the following conditions:

   The above copyright notice and this permission notice shall be included
   in all copies or substantial portions of the Software.

   THE SOFTWARE IS PROVIDED ``AS IS'', WITHOUT WARRANTY OF ANY KIND,
   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
   NONINFRINGEMENT.  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
   HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
   WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
   DEALINGS IN THE SOFTWARE.

   ----------------------------------------------------------------------- */

/* -------------------------------------------------------------------
   The basic API is described in the README file.
   -------------------------------------------------------------------- */

#ifndef LIBFFI_H
#define LIBFFI_H

#ifdef __cplusplus
extern "C" {
#endif

/* ---- System configuration information --------------------------------- */

#include <ffitarget.h>

#ifndef LIBFFI_ASM

#ifdef _MSC_VER
#define __attribute__(X)
#endif

#include <stddef.h>
#include <limits.h>

/* LONG_LONG_MAX is not always defined (not if STRICT_ANSI, for example).
   But we can find it either under the correct ANSI name, or under GNU
   C's internal name.  */
#ifdef LONG_LONG_MAX
# define FFI_LONG_LONG_MAX LONG_LONG_MAX
#else
# ifdef LLONG_MAX
#  define FFI_LONG_LONG_MAX LLONG_MAX
# else
#  ifdef __GNUC__
#   define FFI_LONG_LONG_MAX __LONG_LONG_MAX__
#  endif
# endif
#endif

/* The closure code assumes that this works on pointers, i.e. a size_t	*/
/* can hold a pointer.							*/

typedef struct _ffi_type
{
  size_t size;
  unsigned short alignment;
  unsigned short type;
  struct _ffi_type **elements;
} ffi_type;

#ifndef LIBFFI_HIDE_BASIC_TYPES
#if SCHAR_MAX == 127
# define ffi_type_uchar                ffi_type_uint8
# define ffi_type_schar                ffi_type_sint8
#else
 #error "char size not supported"
#endif

#if SHRT_MAX == 32767
# define ffi_type_ushort       ffi_type_uint16
# define ffi_type_sshort       ffi_type_sint16
#elif SHRT_MAX == 2147483647
# define ffi_type_ushort       ffi_type_uint32
# define ffi_type_sshort       ffi_type_sint32
#else
 #error "short size not supported"
#endif

#if INT_MAX == 32767
# define ffi_type_uint         ffi_type_uint16
# define ffi_type_sint         ffi_type_sint16
#elif INT_MAX == 2147483647
# define ffi_type_uint         ffi_type_uint32
# define ffi_type_sint         ffi_type_sint32
#elif INT_MAX == 9223372036854775807
# define ffi_type_uint         ffi_type_uint64
# define ffi_type_sint         ffi_type_sint64
#else
 #error "int size not supported"
#endif

#if LONG_MAX == 2147483647
# if FFI_LONG_LONG_MAX != 9223372036854775807
 #error "no 64-bit data type supported"
# endif
#elif LONG_MAX != 9223372036854775807
 #error "long size not supported"
#endif

#if LONG_MAX == 2147483647
# define ffi_type_ulong        ffi_type_uint32
# define ffi_type_slong        ffi_type_sint32
#elif LONG_MAX == 9223372036854775807
# define ffi_type_ulong        ffi_type_uint64
# define ffi_type_slong        ffi_type_sint64
#else
 #error "long size not supported"
#endif

/* These are defined in types.c */
extern ffi_type ffi_type_void;
extern ffi_type ffi_type_uint8;
extern ffi_type ffi_type_sint8;
extern ffi_type ffi_type_uint16;
extern ffi_type ffi_type_sint16;
extern ffi_type ffi_type_uint32;
extern ffi_type ffi_type_sint32;
extern ffi_type ffi_type_uint64;
extern ffi_type ffi_type_sint64;
extern ffi_type ffi_type_float;
extern ffi_type ffi_type_double;
extern ffi_type ffi_type_pointer;
extern ffi_type ffi_type_longdouble;
#endif /* LIBFFI_HIDE_BASIC_TYPES */

typedef enum {
  FFI_OK = 0,
  FFI_BAD_TYPEDEF,
  FFI_BAD_ABI
} ffi_status;

typedef unsigned FFI_TYPE;

typedef struct {
  ffi_abi abi;
  unsigned nargs;
  ffi_type **arg_types;
  ffi_type *rtype;
  unsigned bytes;
  unsigned flags;
#ifdef FFI_EXTRA_CIF_FIELDS
  FFI_EXTRA_CIF_FIELDS;
#endif
} ffi_cif;


/* ---- Public interface definition -------------------------------------- */

ffi_status ffi_prep_cif(ffi_cif *cif,
			ffi_abi abi,
			unsigned int nargs,
			ffi_type *rtype,
			ffi_type **atypes);

void ffi_call(ffi_cif *cif,
	      void (*fn)(void),
	      void *rvalue,
	      void **avalue);

/* Useful for eliminating compiler warnings */
#define FFI_FN(f) ((void (*)(void))f)

/* ---- Definitions shared with assembly code ---------------------------- */

#endif

/* If these change, update src/mips/ffitarget.h. */
#define FFI_TYPE_VOID       0    
#define FFI_TYPE_INT        1
#define FFI_TYPE_FLOAT      2    
#define FFI_TYPE_DOUBLE     3
#define FFI_TYPE_LONGDOUBLE 4
#define FFI_TYPE_UINT8      5   
#define FFI_TYPE_SINT8      6
#define FFI_TYPE_UINT16     7 
#define FFI_TYPE_SINT16     8
#define FFI_TYPE_UINT32     9
#define FFI_TYPE_SINT32     10
#define FFI_TYPE_UINT64     11
#define FFI_TYPE_SINT64     12
#define FFI_TYPE_STRUCT     13
#define FFI_TYPE_POINTER    14

/* This should always refer to the last type code (for sanity checks) */
#define FFI_TYPE_LAST       FFI_TYPE_POINTER

#ifdef __cplusplus
}
#endif

#endif
