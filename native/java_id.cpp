#include "stdafx.h"
#include "java_id.h"
#include <oaidl.h>


JClassID javaLangObject("java/lang/Object");

JClassID com4j_COM4J("com4j/COM4J");
JStaticMethodID<jint> com4j_COM4J_getPtr(com4j_COM4J,"getPtr","(Lcom4j/Com4jObject;)I");

JClassID comexception("com4j/ComException");
JConstructorID comexception_new_hr(comexception,"(Ljava/lang/String;ILjava/lang/String;I)V");
JConstructorID comexception_new(comexception,"(Ljava/lang/String;Ljava/lang/String;I)V");
JClassID com4j_Holder("com4j/Holder");
jfieldID com4j_Holder_value;

JClassID com4j_Com4jObject("com4j/Com4jObject");

JClassID com4j_Variant("com4j/Variant");
JConstructorID com4j_Variant_new(com4j_Variant,"()V");
JFieldID<jobject> com4j_Variant_image(com4j_Variant,"image","Ljava/nio/ByteBuffer;");

JClassID com4jWrapper("com4j/Wrapper");
JConstructorID com4jWrapper_new(com4jWrapper,"(I)V");
JMethodID<jobject> com4jWrapper_queryInterface(com4jWrapper,"queryInterface","(Ljava/lang/Class;)Lcom4j/Com4jObject;");

JClassID com4j_ComEnum("com4j/ComEnum");
JMethodID<jint> com4j_ComEnum_comEnumValue(com4j_ComEnum,"comEnumValue","()I");

JClassID com4j_enumDictionary("com4j/EnumDictionary");
JStaticMethodID<jobject> com4j_enumDictionary_get(com4j_enumDictionary,"get","(Ljava/lang/Class;I)Ljava/lang/Enum;");

JClassID javaLangClass("java/lang/Class");
JMethodID<jstring> javaLangClass_getName(javaLangClass,"getName","()Ljava/lang/String;");
