#include "stdafx.h"
#include "java_id.h"
#include <oaidl.h>

JClassID* JClassID::init = NULL;
JMethodID* JMethodID::init = NULL;


JClassID javaLangNumber("java/lang/Number");
JMethodID javaLangNumber_intValue(javaLangNumber,"intValue","()I");

JClassID javaLangInteger("java/lang/Integer");
JMethodID javaLangInteger_new(javaLangInteger,"<init>","(I)V");

JClassID comexception("com4j/ComException");
JMethodID comexception_new(comexception,"<init>","(Ljava/lang/String;I)V");
JClassID com4j_Holder("com4j/Holder");
jfieldID com4j_Holder_value;

