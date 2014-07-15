#include "stdafx.h"
#include "java_id.h"
#include <oaidl.h>


JClassID javaLangObject("java/lang/Object");

JClassID com4j_COM4J("com4j/COM4J");
JStaticMethodID<jlong> com4j_COM4J_getPtr(com4j_COM4J,"getPtr","(Lcom4j/Com4jObject;)J");

JClassID comexception("com4j/ComException");
JConstructorID comexception_new_hr(comexception,"(Ljava/lang/String;ILjava/lang/String;I)V");
JConstructorID comexception_new(comexception,"(Ljava/lang/String;Ljava/lang/String;I)V");
JClassID com4j_Holder("com4j/Holder");
JFieldID<jobject> com4j_Holder_value(com4j_Holder,"value","Ljava/lang/Object;");


JClassID com4j_Com4jObject("com4j/Com4jObject");

JClassID com4j_Variant("com4j/Variant");
JConstructorID com4j_Variant_new(com4j_Variant,"()V");
JFieldID<jobject> com4j_Variant_image(com4j_Variant,"image","Ljava/nio/ByteBuffer;");
JStaticMethodID<jobject> com4j_Variant_toDate(com4j_Variant,"toDate","(D)Ljava/util/Date;");
JStaticMethodID<jdouble> com4j_Variant_fromDate(com4j_Variant,"fromDate","(Ljava/util/Date;)D");

JClassID com4jWrapper("com4j/Wrapper");
JConstructorID com4jWrapper_new(com4jWrapper,"(J)V");
JMethodID<jobject> com4jWrapper_queryInterface(com4jWrapper,"queryInterface","(Ljava/lang/Class;)Lcom4j/Com4jObject;");
JMethodID<void> com4jWrapper_dispose0(com4jWrapper,"dispose0","()V");

JClassID com4j_ComEnum("com4j/ComEnum");
JMethodID<jint> com4j_ComEnum_comEnumValue(com4j_ComEnum,"comEnumValue","()I");

JClassID com4j_enumDictionary("com4j/EnumDictionary");
JStaticMethodID<jobject> com4j_enumDictionary_get(com4j_enumDictionary,"get","(Ljava/lang/Class;I)Ljava/lang/Enum;");

JClassID javaLangClass("java/lang/Class");
JMethodID<jstring> javaLangClass_getName(javaLangClass,"getName","()Ljava/lang/String;");

JClassID javaMathBigInteger("java/math/BigInteger");
JMethodID<jstring> javaMathBigInteger_toString(javaMathBigInteger,"toString","()Ljava/lang/String;");
JConstructorID javaMathBigInteger_new(javaMathBigInteger,"(Ljava/lang/String;)V");

JClassID javaMathBigDecimal("java/math/BigDecimal");
JMethodID<jstring> javaMathBigDecimal_toString(javaMathBigDecimal,"toString","()Ljava/lang/String;");
JConstructorID javaMathBigDecimal_new(javaMathBigDecimal,"(Ljava/math/BigInteger;I)V");
JConstructorID javaMathBigDecimal_new_Str(javaMathBigDecimal,"(Ljava/lang/String;)V");

JClassID javaUtilDate("java/util/Date");
