#include "stdafx.h"
#include "java_id.h"
#include <oaidl.h>

JClassID* JClassID::init = NULL;
JMethodID_Base* JMethodID_Base::init = NULL;


JClassID javaLangNumber("java/lang/Number");
JMethodID javaLangNumber_intValue(javaLangNumber,"intValue","()I");
JMethodID javaLangNumber_floatValue(javaLangNumber,"floatValue","()F");
JMethodID javaLangNumber_doubleValue(javaLangNumber,"doubleValue","()D");

JClassID javaLangInteger("java/lang/Integer");
JMethodID javaLangInteger_new(javaLangInteger,"<init>","(I)V");

JClassID javaLangBoolean("java/lang/Boolean");
JMethodID javaLangBoolean_booleanValue(javaLangBoolean,"booleanValue","()Z");
JStaticMethodID javaLangBoolean_valueOf(javaLangBoolean,"valueOf","(Z)Ljava/lang/Boolean;");

JClassID comexception("com4j/ComException");
JMethodID comexception_new(comexception,"<init>","(Ljava/lang/String;I)V");
JClassID com4j_Holder("com4j/Holder");
jfieldID com4j_Holder_value;

