/* -----------------------------------------------------------------------
   ffi.c - Copyright (c) 1996, 1998, 1999, 2001, 2007, 2008  Red Hat, Inc.
           Copyright (c) 2002  Ranjit Mathew
           Copyright (c) 2002  Bo Thorsen
           Copyright (c) 2002  Roger Sayle
           Copyright (C) 2008  Free Software Foundation, Inc.

   x86 Foreign Function Interface

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

#ifdef _WIN64
#include <windows.h>
#endif

#include <ffi.h>
#include <ffi_common.h>

#include <stdlib.h>

/* ffi_prep_args is called by the assembly routine once stack space
   has been allocated for the function's arguments */

void ffi_prep_args(char *stack, extended_cif *ecif)
{
  register unsigned int i;
  register void **p_argv;
  register char *argp;
  register ffi_type **p_arg;

  argp = stack;

  if (ecif->cif->flags == FFI_TYPE_STRUCT
#ifdef X86_WIN64
      && (ecif->cif->rtype->size != 1 && ecif->cif->rtype->size != 2
          && ecif->cif->rtype->size != 4 && ecif->cif->rtype->size != 8)
#endif
      )
    {
      *(void **) argp = ecif->rvalue;
      argp += sizeof(void*);
    }

  p_argv = ecif->avalue;

  for (i = ecif->cif->nargs, p_arg = ecif->cif->arg_types;
       i != 0;
       i--, p_arg++)
    {
      size_t z;

      /* Align if necessary */
      if ((sizeof(void*) - 1) & (size_t) argp)
        argp = (char *) ALIGN(argp, sizeof(void*));

      z = (*p_arg)->size;
#ifdef X86_WIN64
      if (z > sizeof(ffi_arg)
          || ((*p_arg)->type == FFI_TYPE_STRUCT
              && (z != 1 && z != 2 && z != 4 && z != 8))
#if FFI_TYPE_DOUBLE != FFI_TYPE_LONGDOUBLE
          || ((*p_arg)->type == FFI_TYPE_LONGDOUBLE)
#endif
          )
        {
          z = sizeof(ffi_arg);
          *(void **)argp = *p_argv;
        }
      else if ((*p_arg)->type == FFI_TYPE_FLOAT)
        {
          memcpy(argp, *p_argv, z);
        }
      else
#endif
      if (z < sizeof(ffi_arg))
        {
          z = sizeof(ffi_arg);
          switch ((*p_arg)->type)
            {
            case FFI_TYPE_SINT8:
              *(ffi_sarg *) argp = (ffi_sarg)*(SINT8 *)(* p_argv);
              break;

            case FFI_TYPE_UINT8:
              *(ffi_arg *) argp = (ffi_arg)*(UINT8 *)(* p_argv);
              break;

            case FFI_TYPE_SINT16:
              *(ffi_sarg *) argp = (ffi_sarg)*(SINT16 *)(* p_argv);
              break;

            case FFI_TYPE_UINT16:
              *(ffi_arg *) argp = (ffi_arg)*(UINT16 *)(* p_argv);
              break;

            case FFI_TYPE_SINT32:
              *(ffi_sarg *) argp = (ffi_sarg)*(SINT32 *)(* p_argv);
              break;

            case FFI_TYPE_UINT32:
              *(ffi_arg *) argp = (ffi_arg)*(UINT32 *)(* p_argv);
              break;

            case FFI_TYPE_STRUCT:
              *(ffi_arg *) argp = *(ffi_arg *)(* p_argv);
              break;

            default:
              FFI_ASSERT(0);
            }
        }
      else
        {
          memcpy(argp, *p_argv, z);
        }
      p_argv++;
#ifdef X86_WIN64
      argp += (z + sizeof(void*) - 1) & ~(sizeof(void*) - 1);
#else
      argp += z;
#endif
    }
  
  return;
}

/* Perform machine dependent cif processing */
ffi_status ffi_prep_cif_machdep(ffi_cif *cif)
{
  /* Set the return type flag */
  switch (cif->rtype->type)
    {
    case FFI_TYPE_VOID:
#ifdef X86
    case FFI_TYPE_STRUCT:
#endif
#if defined(X86) || defined(X86_DARWIN) || defined(X86_WIN64)
    case FFI_TYPE_UINT8:
    case FFI_TYPE_UINT16:
    case FFI_TYPE_SINT8:
    case FFI_TYPE_SINT16:
#endif
#ifdef X86_WIN64
    case FFI_TYPE_UINT32:
    case FFI_TYPE_SINT32:
#endif

    case FFI_TYPE_SINT64:
    case FFI_TYPE_FLOAT:
    case FFI_TYPE_DOUBLE:
#ifndef X86_WIN64
#if FFI_TYPE_DOUBLE != FFI_TYPE_LONGDOUBLE
    case FFI_TYPE_LONGDOUBLE:
#endif
#endif
      cif->flags = (unsigned) cif->rtype->type;
      break;

    case FFI_TYPE_UINT64:
#ifdef X86_WIN64
    case FFI_TYPE_POINTER:
#endif
      cif->flags = FFI_TYPE_SINT64;
      break;

#ifndef X86
    case FFI_TYPE_STRUCT:
      if (cif->rtype->size == 1)
        {
          cif->flags = FFI_TYPE_SMALL_STRUCT_1B; /* same as char size */
        }
      else if (cif->rtype->size == 2)
        {
          cif->flags = FFI_TYPE_SMALL_STRUCT_2B; /* same as short size */
        }
      else if (cif->rtype->size == 4)
        {
#ifdef X86_WIN64
          cif->flags = FFI_TYPE_SMALL_STRUCT_4B;
#else
          cif->flags = FFI_TYPE_INT; /* same as int type */
#endif
        }
      else if (cif->rtype->size == 8)
        {
          cif->flags = FFI_TYPE_SINT64; /* same as int64 type */
        }
      else
        {
          cif->flags = FFI_TYPE_STRUCT;
#ifdef X86_WIN64
          // allocate space for return value pointer
          cif->bytes += ALIGN(sizeof(void*), FFI_SIZEOF_ARG);
#endif
        }
      break;
#endif

    default:
#ifdef X86_WIN64
      cif->flags = FFI_TYPE_SINT64;
      break;
    case FFI_TYPE_INT:
      cif->flags = FFI_TYPE_SINT32;
#else
      cif->flags = FFI_TYPE_INT;
#endif
      break;
    }

#ifdef X86_DARWIN
  cif->bytes = (cif->bytes + 15) & ~0xF;
#endif

#ifdef X86_WIN64
  {
    unsigned int i;
    ffi_type **ptr;

    for (ptr = cif->arg_types, i = cif->nargs; i > 0; i--, ptr++)
      {
        if (((*ptr)->alignment - 1) & cif->bytes)
          cif->bytes = ALIGN(cif->bytes, (*ptr)->alignment);
        cif->bytes += ALIGN((*ptr)->size, FFI_SIZEOF_ARG);
      }
  }
  // ensure space for storing four registers
  cif->bytes += 4 * sizeof(ffi_arg);
#endif

  return FFI_OK;
}

extern void ffi_call_SYSV(void (*)(char *, extended_cif *), extended_cif *,
                          unsigned, unsigned, unsigned *, void (*fn)(void));

#ifdef X86_WIN32
extern void ffi_call_STDCALL(void (*)(char *, extended_cif *), extended_cif *,
                          unsigned, unsigned, unsigned *, void (*fn)(void));

#endif /* X86_WIN32 */
#ifdef X86_WIN64
extern int
ffi_call_win64(void (*)(char *, extended_cif *), extended_cif *,
               unsigned, unsigned, unsigned *, void (*fn)(void));
#endif

void ffi_call(ffi_cif *cif, void (*fn)(void), void *rvalue, void **avalue)
{
  extended_cif ecif;

  ecif.cif = cif;
  ecif.avalue = avalue;
  
  /* If the return value is a struct and we don't have a return */
  /* value address then we need to make one                     */

#ifdef X86_WIN64
  if (rvalue == NULL
      && cif->flags == FFI_TYPE_STRUCT
      && cif->rtype->size != 1 && cif->rtype->size != 2
      && cif->rtype->size != 4 && cif->rtype->size != 8)
    {
      ecif.rvalue = alloca((cif->rtype->size + 0xF) & ~0xF);
    }
#else
  if (rvalue == NULL
      && cif->flags == FFI_TYPE_STRUCT)
    {
      ecif.rvalue = alloca(cif->rtype->size);
    }
#endif
  else
    ecif.rvalue = rvalue;
    
  
  switch (cif->abi) 
    {
#ifdef X86_WIN64
    case FFI_WIN64:
      {
        // Make copies of all struct arguments
        // NOTE: not sure if responsibility should be here or in caller
        unsigned int i;
        for (i=0; i < cif->nargs;i++) {
          size_t size = cif->arg_types[i]->size;
          if ((cif->arg_types[i]->type == FFI_TYPE_STRUCT
               && (size != 1 && size != 2 && size != 4 && size != 8))
#if FFI_TYPE_LONGDOUBLE != FFI_TYPE_DOUBLE
              || cif->arg_types[i]->type == FFI_TYPE_LONGDOUBLE
#endif
              )
            {
              void *local = alloca(size);
              memcpy(local, avalue[i], size);
              avalue[i] = local;
            }
        }
        ffi_call_win64(ffi_prep_args, &ecif, cif->bytes,
                       cif->flags, ecif.rvalue, fn);
      }
      break;
#else
    case FFI_SYSV:
      ffi_call_SYSV(ffi_prep_args, &ecif, cif->bytes, cif->flags, ecif.rvalue,
                    fn);
      break;
#ifdef X86_WIN32
    case FFI_STDCALL:
      ffi_call_STDCALL(ffi_prep_args, &ecif, cif->bytes, cif->flags,
                       ecif.rvalue, fn);
      break;
#endif /* X86_WIN32 */
#endif /* X86_WIN64 */
    default:
      FFI_ASSERT(0);
      break;
    }
}


