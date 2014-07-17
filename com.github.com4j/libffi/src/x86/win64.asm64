FFI_TYPE_VOID				=0    
FFI_TYPE_INT				=1
FFI_TYPE_FLOAT				=2    
FFI_TYPE_DOUBLE				=3
FFI_TYPE_LONGDOUBLE			=4
FFI_TYPE_UINT8				=5   
FFI_TYPE_SINT8				=6
FFI_TYPE_UINT16				=7 
FFI_TYPE_SINT16				=8
FFI_TYPE_UINT32				=9
FFI_TYPE_SINT32				=10
FFI_TYPE_UINT64				=11
FFI_TYPE_SINT64				=12
FFI_TYPE_STRUCT				=13
FFI_TYPE_POINTER			=14
FFI_TYPE_SMALL_STRUCT_4B	=15
FFI_TYPE_SMALL_STRUCT_2B	=16
FFI_TYPE_SMALL_STRUCT_1B	=17

;; Constants for ffi_call_win64	
STACK						=0
PREP_ARGS_FN				=32
ECIF						=40
CIF_BYTES					=48
CIF_FLAGS					=56
RVALUE						=64
FN							=72

PUBLIC	ffi_call_win64

EXTRN	__chkstk:NEAR

_TEXT	SEGMENT

ffi_call_win64 PROC FRAME
        ;; copy registers onto stack
	mov	QWORD PTR [rsp+32], r9
	mov	QWORD PTR [rsp+24], r8
	mov	QWORD PTR [rsp+16], rdx
	mov	QWORD PTR [rsp+8], rcx
        .PUSHREG rbp
	push	rbp
        .ALLOCSTACK 48
	sub	rsp, 48					; 00000030H
        .SETFRAME rbp, 32
	lea	rbp, QWORD PTR [rsp+32]
        .ENDPROLOG

	mov	eax, DWORD PTR CIF_BYTES[rbp]
	add	rax, 15
	and	rax, -16
	call	__chkstk
	sub	rsp, rax
	lea	rax, QWORD PTR [rsp+32]
	mov	QWORD PTR STACK[rbp], rax

	mov	rdx, QWORD PTR ECIF[rbp]
	mov	rcx, QWORD PTR STACK[rbp]
	call	QWORD PTR PREP_ARGS_FN[rbp]

	mov	rsp, QWORD PTR STACK[rbp]

	movlpd	xmm3, QWORD PTR [rsp+24]
	movd	r9, xmm3

	movlpd	xmm2, QWORD PTR [rsp+16]
	movd	r8, xmm2

	movlpd	xmm1, QWORD PTR [rsp+8]
	movd	rdx, xmm1

	movlpd	xmm0, QWORD PTR [rsp]
	movd	rcx, xmm0

	call	QWORD PTR FN[rbp]
ret_struct4b$:
 	cmp	DWORD PTR CIF_FLAGS[rbp], FFI_TYPE_SMALL_STRUCT_4B
 	jne	ret_struct2b$

	mov	rcx, QWORD PTR RVALUE[rbp]
	mov	DWORD PTR [rcx], eax
	jmp	ret_void$

ret_struct2b$:
 	cmp	DWORD PTR CIF_FLAGS[rbp], FFI_TYPE_SMALL_STRUCT_2B
 	jne	ret_struct1b$

	mov	rcx, QWORD PTR RVALUE[rbp]
	mov	WORD PTR [rcx], ax
	jmp	ret_void$

ret_struct1b$:
 	cmp	DWORD PTR CIF_FLAGS[rbp], FFI_TYPE_SMALL_STRUCT_1B
 	jne	ret_uint8$

	mov	rcx, QWORD PTR RVALUE[rbp]
	mov	BYTE PTR [rcx], al
	jmp	ret_void$

ret_uint8$:
 	cmp	DWORD PTR CIF_FLAGS[rbp], FFI_TYPE_UINT8
 	jne	ret_sint8$

	mov	rcx, QWORD PTR RVALUE[rbp]
	movzx   rax, al
	mov	QWORD PTR [rcx], rax
	jmp	ret_void$

ret_sint8$:
 	cmp	DWORD PTR CIF_FLAGS[rbp], FFI_TYPE_SINT8
 	jne	ret_uint16$

	mov	rcx, QWORD PTR RVALUE[rbp]
	movsx   rax, al
	mov	QWORD PTR [rcx], rax
	jmp	ret_void$

ret_uint16$:
 	cmp	DWORD PTR CIF_FLAGS[rbp], FFI_TYPE_UINT16
 	jne	ret_sint16$

	mov	rcx, QWORD PTR RVALUE[rbp]
	movzx   rax, ax
	mov	QWORD PTR [rcx], rax
	jmp	SHORT ret_void$

ret_sint16$:
 	cmp	DWORD PTR CIF_FLAGS[rbp], FFI_TYPE_SINT16
 	jne	ret_uint32$

	mov	rcx, QWORD PTR RVALUE[rbp]
	movsx   rax, ax
	mov	QWORD PTR [rcx], rax
	jmp	SHORT ret_void$

ret_uint32$:
 	cmp	DWORD PTR CIF_FLAGS[rbp], FFI_TYPE_UINT32
 	jne	ret_sint32$

	mov	rcx, QWORD PTR RVALUE[rbp]
	mov     eax, eax
	mov	QWORD PTR [rcx], rax
	jmp	SHORT ret_void$

ret_sint32$:
 	cmp	DWORD PTR CIF_FLAGS[rbp], FFI_TYPE_SINT32
 	jne	ret_float$

	mov	rcx, QWORD PTR RVALUE[rbp]
	cdqe
	mov	QWORD PTR [rcx], rax
	jmp	SHORT ret_void$

ret_float$:
 	cmp	DWORD PTR CIF_FLAGS[rbp], FFI_TYPE_FLOAT
 	jne	SHORT ret_double$

 	mov	rax, QWORD PTR RVALUE[rbp]
 	movss	DWORD PTR [rax], xmm0
 	jmp	SHORT ret_void$

ret_double$:
 	cmp	DWORD PTR CIF_FLAGS[rbp], FFI_TYPE_DOUBLE
 	jne	SHORT ret_sint64$

 	mov	rax, QWORD PTR RVALUE[rbp]
 	movlpd	QWORD PTR [rax], xmm0
 	jmp	SHORT ret_void$

ret_sint64$:
  	cmp	DWORD PTR CIF_FLAGS[rbp], FFI_TYPE_SINT64
  	jne	ret_void$

 	mov	rcx, QWORD PTR RVALUE[rbp]
 	mov	QWORD PTR [rcx], rax
 	jmp	SHORT ret_void$
	
ret_void$:
	xor	rax, rax

	lea	rsp, QWORD PTR [rbp+16]
	pop	rbp
	ret	0
ffi_call_win64 ENDP
_TEXT	ENDS
END