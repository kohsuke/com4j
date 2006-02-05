#include "stdafx.h"
#include "java_id.h"
#include <oaidl.h>


/*
JClassID javaLangNumber("java/lang/Number");
JMethodID<jbyte> javaLangNumber_byteValue(javaLangNumber,"byteValue","()B");
JMethodID<jint> javaLangNumber_intValue(javaLangNumber,"intValue","()I");
JMethodID<jfloat> javaLangNumber_floatValue(javaLangNumber,"floatValue","()F");
JMethodID<jdouble> javaLangNumber_doubleValue(javaLangNumber,"doubleValue","()D");
JMethodID<jshort> javaLangNumber_shortValue(javaLangNumber,"shortValue","()S");
JMethodID<jlong> javaLangNumber_longValue(javaLangNumber,"longValue","()J");

JClassID javaLangInteger("java/lang/Integer");
JStaticMethodID<jobject> javaLangInteger_valueOf(javaLangInteger,"valueOf","(I)Ljava/lang/Integer;");

JClassID javaLangShort("java/lang/Short");
JStaticMethodID<jobject> javaLangShort_valueOf(javaLangShort,"valueOf","(S)Ljava/lang/Short;");

JClassID javaLangLong("java/lang/Long");
JStaticMethodID<jobject> javaLangLong_valueOf(javaLangLong,"valueOf","(J)Ljava/lang/Long;");

JClassID javaLangFloat("java/lang/Float");
JStaticMethodID<jobject> javaLangFloat_valueOf(javaLangFloat,"valueOf","(F)Ljava/lang/Float;");

JClassID javaLangDouble("java/lang/Double");
JStaticMethodID<jobject> javaLangDouble_valueOf(javaLangDouble,"valueOf","(D)Ljava/lang/Double;");

JClassID javaLangByte("java/lang/Byte");
JStaticMethodID<jobject> javaLangByte_valueOf(javaLangByte,"valueOf","(B)Ljava/lang/Byte;");

JClassID javaLangBoolean("java/lang/Boolean");
JMethodID<jboolean> javaLangBoolean_booleanValue(javaLangBoolean,"booleanValue","()Z");
JStaticMethodID<jobject> javaLangBoolean_valueOf(javaLangBoolean,"valueOf","(Z)Ljava/lang/Boolean;");

JClassID javaLangString("java/lang/String");
*/

JClassID com4j_COM4J("com4j/COM4J");
JStaticMethodID<jint> com4j_COM4J_getPtr(com4j_COM4J,"getPtr","(Lcom4j/Com4jObject;)I");

JClassID comexception("com4j/ComException");
JConstructorID comexception_new_hr(comexception,"(Ljava/lang/String;ILjava/lang/String;I)V");
JConstructorID comexception_new(comexception,"(Ljava/lang/String;Ljava/lang/String;I)V");
JClassID com4j_Holder("com4j/Holder");
jfieldID com4j_Holder_value;

JClassID com4j_Com4jObject("com4j/Com4jObject");

JClassID com4j_Variant("com4j/Variant");
jfieldID com4j_Variant_image;

JClassID com4jWrapper("com4j/Wrapper");
JConstructorID com4jWrapper_new(com4jWrapper,"(I)V");

/*
JClassID booleanArray("[Z");
JClassID byteArray("[B");
JClassID charArray("[C");
JClassID doubleArray("[D");
JClassID floatArray("[F");
JClassID intArray("[I");
JClassID longArray("[J");
JClassID shortArray("[S");
JClassID stringArray("[Ljava/lang/String;");
JClassID objectArray("[Ljava/lang/Object;");
*/
