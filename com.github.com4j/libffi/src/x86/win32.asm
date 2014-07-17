;;;-----------------------------------------------------------------------
;;;   win32.S - Copyright (c) 1996, 1998, 2001, 2002, 2009  Red Hat, Inc.
;;;	     Copyright (c) 2001  John Beniton
;;;	     Copyright (c) 2002  Ranjit Mathew
;;;   Ported to MASM syntax for Com4J, and removed closure functionality by
;;;   Mike Poindexter (staticsnow <at> gmail.com)
;;;			
;;;
;;;   X86 Foreign Function Interface
;;; 
;;;   Permission is hereby granted, free of charge, to any person obtaining
;;;   a copy of this software and associated documentation files (the
;;;   ``Software''), to deal in the Software without restriction, including
;;;   without limitation the rights to use, copy, modify, merge, publish,
;;;   distribute, sublicense, and/or sell copies of the Software, and to
;;;   permit persons to whom the Software is furnished to do so, subject to
;;;   the following conditions:
;;; 
;;;   The above copyright notice and this permission notice shall be included
;;;   in all copies or substantial portions of the Software.
;;; 
;;;   THE SOFTWARE IS PROVIDED ``AS IS'', WITHOUT WARRANTY OF ANY KIND,
;;;   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
;;;   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
;;;   NONINFRINGEMENT.  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
;;;   HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
;;;   WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
;;;   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
;;;   DEALINGS IN THE SOFTWARE.
;;;   -----------------------------------------------------------------------

FFI_TYPE_VOID       =0    
FFI_TYPE_INT        =1
FFI_TYPE_FLOAT      =2    
FFI_TYPE_DOUBLE     =3
FFI_TYPE_LONGDOUBLE =4
FFI_TYPE_UINT8      =5   
FFI_TYPE_SINT8      =6
FFI_TYPE_UINT16     =7 
FFI_TYPE_SINT16     =8
FFI_TYPE_UINT32     =9
FFI_TYPE_SINT32     =10
FFI_TYPE_UINT64     =11
FFI_TYPE_SINT64     =12
FFI_TYPE_STRUCT     =13
FFI_TYPE_POINTER    =14

.386
PUBLIC	_ffi_call_STDCALL
PUBLIC	_ffi_call_SYSV

_TEXT	SEGMENT

_ffi_call_SYSV PROC
	push ebp
	mov ebp, esp
	
	;;Make room for all of the new args.
	mov ecx, dword ptr[16+ebp]
	sub esp, ecx
	mov eax, esp
	
	;;Place all of the ffi_prep_args in position
	push dword ptr[12+ebp]
	push eax
	call dword ptr[8+ebp]
	
	;;Return stack to previous state and call the function
	add esp, 8
	
	;;FIXME: Align the stack to a 128-bit boundary to avoid
    ;;potential performance hits.
	call dword ptr[28+ebp]
	
	;;Clean stack
	mov ecx, dword ptr[16+ebp]
	add esp, ecx
	mov ecx, dword ptr[20+ebp]
	
	;;Check for NULL return
	cmp dword ptr[24+ebp], 0
	jne retint
	
	;;Clean fp stack if needed
	cmp ecx, FFI_TYPE_FLOAT 
	jne noretval
	fstp st(0)
	jmp epilogue
retint:
	cmp ecx, FFI_TYPE_INT
	jne retfloat
	
	mov ecx, dword ptr[24+ebp]
	mov dword ptr[0+ecx],eax
	jmp epilogue
retfloat:
	cmp ecx, FFI_TYPE_FLOAT
	jne retdouble
 
	mov ecx, dword ptr[24+ebp]
	fstp dword ptr[ecx]
	jmp epilogue
retdouble:
	cmp ecx, FFI_TYPE_DOUBLE
	jne retlongdouble
	
	mov ecx, dword ptr[24+ebp]
	fstp dword ptr[ecx]
	jmp epilogue
retlongdouble:
	cmp ecx, FFI_TYPE_LONGDOUBLE
	jne retint64
	
	mov ecx, dword ptr[24+ebp]
	fstp dword ptr[ecx]
	jmp epilogue
retint64:
	cmp ecx, FFI_TYPE_SINT64
	jne retstruct1b
	
	mov ecx, dword ptr[24+ebp]
	mov dword ptr[0+ecx],eax
	mov dword ptr[4+ecx],edx
retstruct1b:
	cmp ecx,offset FFI_TYPE_SINT8
	jne retstruct2b
	
	mov ecx, dword ptr[24+ebp]
	mov byte ptr[0+ecx], al
	jmp epilogue
retstruct2b:
	cmp ecx, FFI_TYPE_SINT16
	jne retstruct
	
	mov ecx,dword ptr[24+ebp]
	mov word ptr[0+ecx],ax
	jmp epilogue
retstruct:
noretval:
epilogue:
	mov esp, ebp
	pop ebp
	ret
_ffi_call_SYSV ENDP

_ffi_call_STDCALL PROC
	push ebp
	mov ebp, esp
	
	;;Make room for all of the new args.
	mov ecx, dword ptr[16+ebp]
	sub esp, ecx
	mov eax, esp
	
	;;Place all of the ffi_prep_args in position
	push dword ptr[12+ebp]
	push eax
	call dword ptr[8+ebp]
	
	;;Return stack to previous state and call the function
	add esp, 8
	
	;;FIXME: Align the stack to a 128-bit boundary to avoid
    ;;potential performance hits.
	call dword ptr[28+ebp]
	
	;;stdcall functions pop arguments off the stack themselves

    ;;Load %ecx with the return type code
	mov ecx, dword ptr[20+ebp]
	
	;;If the return value pointer is NULL, assume no return value.
	cmp dword ptr[24+ebp], 0
	jne sc_retint
	
	;;Even if there is no space for the return value, we are
    ;;obliged to handle floating-point values.
	cmp ecx, FFI_TYPE_FLOAT
	jne sc_noretval
	fstp st(0)
	jmp sc_epilogue

sc_retint:
	cmp ecx, FFI_TYPE_INT
	jne sc_retfloat
 
	mov ecx,dword ptr[24+ebp]
	mov dword ptr[ecx],eax
	jmp sc_epilogue
	
sc_retfloat:
	cmp ecx, FFI_TYPE_FLOAT
	jne sc_retdouble
	
	mov ecx, dword ptr[24+ebp]
	fstp dword ptr[ecx]
	jmp sc_epilogue
	
sc_retdouble:
	cmp ecx, FFI_TYPE_DOUBLE
	jne sc_retlongdouble
 
	mov ecx, dword ptr[24+ebp]
	fstp dword ptr[ecx]
	jmp sc_epilogue
	
sc_retlongdouble:
	cmp ecx,offset FFI_TYPE_LONGDOUBLE
	jne sc_retint64
	
	mov ecx,dword ptr[24+ebp]
	fstp dword ptr[ecx]
	jmp sc_epilogue
	
sc_retint64:
	cmp ecx, FFI_TYPE_SINT64
	jne sc_retstruct1b
 
	mov ecx, dword ptr[24+ebp]
	mov dword ptr[0+ecx],eax
	mov dword ptr[4+ecx],edx

sc_retstruct1b:
	cmp ecx, FFI_TYPE_SINT8
	jne sc_retstruct2b
	
	mov ecx, dword ptr[24+ebp]
	mov byte ptr[0+ecx],al
	jmp sc_epilogue
	
sc_retstruct2b:
	cmp ecx, FFI_TYPE_SINT16
	jne sc_retstruct
 
	mov ecx, dword ptr[24+ebp]
	mov word ptr[0+ecx],ax
	jmp sc_epilogue

sc_retstruct:
sc_noretval:
sc_epilogue:
	mov esp,ebp
	pop ebp
	ret
	
_ffi_call_STDCALL ENDP


_TEXT	ENDS
END