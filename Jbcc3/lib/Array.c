#include "jbcc.h"
#include "Array.h"
#include "java/lang/Object.h"
#include "java/lang/CloneNotSupportedException.h"
#include "java/lang/Cloneable.h"
#include "java/io/Serializable.h"
#include "Array.h"
#include "java/lang/InternalError.h"
// <init> ()V
void Array__init_uph(jbcc_env env, Array _0) {
Any _1, _2;
Lx4zaov: ;
	// line	3 Lx4zaov
	// ALOAD	0
_1.R.O = _0;
	// INVOKESPECIAL	java/lang/Object <init> ()V false
if (_1.R.O == NULL_REFERENCE) jbcc_throw_NullPointerException();
((java_lang_Object)_1.R.O)->_C_->M._init_uph(env,_1.R.O);
Llwchh3: ;
	// line	4 Llwchh3
	// ALOAD	0
_1.R.O = _0;
	// ICONST_0
_2.I = 0;
	// PUTFIELD	Array length I
if (_1.R.O == NULL_REFERENCE) jbcc_throw_NullPointerException();
((Array)_1.R.O)->length = _2.I;
Lgdn19l: ;
	// line	3 Lgdn19l
	// RETURN
return;
// DEAD CODE
}
// clone ()[Ljava/lang/Object;
Array_java_lang_Object Array_clone_Mw846ei(jbcc_env env, Array _0) {
Any _1, _2, _3, _4;
	// try Li542me Lhu8685 Luaooc4 java/lang/CloneNotSupportedException
Li542me: ;
	// try Li542me Lhu8685 Luaooc4 java/lang/CloneNotSupportedException
static struct catch_entry catchLi542me[] = {{&c_java_lang_CloneNotSupportedException,&&Luaooc4},{0,0}};
CATCH_PUSH(env,catchLi542me);
	// line	8 Li542me
	// ALOAD	0
_2.R.O = _0;
	// INVOKESPECIAL	java/lang/Object clone ()Ljava/lang/Object; false
if (_2.R.O == NULL_REFERENCE) jbcc_throw_NullPointerException();
_2.R.O = ((java_lang_Object)_2.R.O)->_C_->M.clone_Mz83ag3(env,_2.R.O);
	// CHECKCAST	[Ljava/lang/Object;
jbcc_check_cast(_2.R.O, (struct Class*)&c_Array_java_lang_Object);
Lhu8685: ;
	// try end Li542me
CATCH_POP(env,catchLi542me);
	// ARETURN
return _2.R.O;
Luaooc4: ;
	// line	9 Luaooc4
	// frame 4 local 0 stack 1
	// ASTORE	1
_1.R.O = _2.R.O;
Lswgtuo: ;
	// line	10 Lswgtuo
	// NEW	java/lang/InternalError
_2.R.O = jbcc_new((struct Class*)&c_java_lang_InternalError.C);
	// DUP
_3 = _2;
	// ALOAD	1
_4.R.O = _1.R.O;
	// INVOKEVIRTUAL	java/lang/CloneNotSupportedException getMessage ()Ljava/lang/String; false
if (_4.R.O == NULL_REFERENCE) jbcc_throw_NullPointerException();
_4.R.O = ((java_lang_CloneNotSupportedException)_4.R.O)->_C_->M.getMessage_w4s62z(env,_4.R.O);
	// INVOKESPECIAL	java/lang/InternalError <init> (Ljava/lang/String;)V false
if (_3.R.O == NULL_REFERENCE) jbcc_throw_NullPointerException();
((java_lang_InternalError)_3.R.O)->_C_->M._init_Mnmrpxd(env,_3.R.O,_4.R.O);
	// ATHROW
jbcc_throw(_2.R.O);
/*NOTREACHED*/__builtin_unreachable(); // control reaches end of non-void function
// DEAD CODE
}
// clone ()Ljava/lang/Object;
	// exceptions java/lang/CloneNotSupportedException
java_lang_Object Array_clone_Mz83ag3(jbcc_env env, Array _0) {
Any _1;
Lgxlku8: ;
	// line	1 Lgxlku8
	// ALOAD	0
_1.R.O = _0;
	// INVOKEVIRTUAL	Array clone ()[Ljava/lang/Object; false
if (_1.R.O == NULL_REFERENCE) jbcc_throw_NullPointerException();
_1.A = ((Array)_1.R.O)->_C_->M.clone_Mw846ei(env,_1.R.O);
	// ARETURN
return _1.R.O;
}
static struct m_java_lang_Cloneable m_Array_java_lang_Cloneable = {
    ._init_uph = /*override-incompatible pointer types*/&Array__init_uph,
    .equals_pw62vp = &java_lang_Object_equals_pw62vp,
    .hashCode_up4 = &java_lang_Object_hashCode_up4,
    .toString_w4s62z = &java_lang_Object_toString_w4s62z,
    .finalize_uph = &java_lang_Object_finalize_uph,
    .clone_Mz83ag3 = /*override-incompatible pointer types*/&Array_clone_Mz83ag3,
    .getClass_M234hr2 = &java_lang_Object_getClass_M234hr2,
    .notify_uph = &java_lang_Object_notify_uph,
    .notifyAll_uph = &java_lang_Object_notifyAll_uph,
    .wait_uph = &java_lang_Object_wait_uph,
    .wait_r3e7 = &java_lang_Object_wait_r3e7,
    .wait_ncjxw = &java_lang_Object_wait_ncjxw,
};
static struct m_java_io_Serializable m_Array_java_io_Serializable = {
    ._init_uph = /*override-incompatible pointer types*/&Array__init_uph,
    .equals_pw62vp = &java_lang_Object_equals_pw62vp,
    .hashCode_up4 = &java_lang_Object_hashCode_up4,
    .toString_w4s62z = &java_lang_Object_toString_w4s62z,
    .finalize_uph = &java_lang_Object_finalize_uph,
    .clone_Mz83ag3 = /*override-incompatible pointer types*/&Array_clone_Mz83ag3,
    .getClass_M234hr2 = &java_lang_Object_getClass_M234hr2,
    .notify_uph = &java_lang_Object_notify_uph,
    .notifyAll_uph = &java_lang_Object_notifyAll_uph,
    .wait_uph = &java_lang_Object_wait_uph,
    .wait_r3e7 = &java_lang_Object_wait_r3e7,
    .wait_ncjxw = &java_lang_Object_wait_ncjxw,
};
struct Interface_List_Entry i_Array[] = {
    {&c_java_lang_Cloneable, &m_Array_java_lang_Cloneable},
    {&c_java_io_Serializable, &m_Array_java_io_Serializable},
    {0, 0}
};
#ifdef CLASS_INFO
static Class_Info_UTF8z CS98lkda = "Array";
static Class_Info_UTF8z CSj09wq8 = "java/lang/Object";
static Class_Info_UTF8z CSymhsnf = "java/lang/Cloneable";
static Class_Info_UTF8z CSyb1r2n = "java/io/Serializable";
static struct Class_Interface_Info interfaces[] = {
    {&CSymhsnf}, // java/lang/Cloneable
    {&CSyb1r2n}, // java/io/Serializable
};
static Class_Info_UTF8z CSgjcqsz = "length";
static Class_Info_UTF8z CSwwnwqx = "I";
static struct Class_Field_Info fields[] = {
    {0x11,&CSgjcqsz,&CSwwnwqx}, // PubFin length I
};
static Class_Info_UTF8z CSoh5r7k = "<init>";
static Class_Info_UTF8z CS1cll1f = "()V";
static Class_Info_UTF8z CSu8x83a = "clone";
static Class_Info_UTF8z CSo0k4c2 = "()[Ljava/lang/Object;";
static Class_Info_UTF8z CSfeu9e1 = "()Ljava/lang/Object;";
static struct Class_Method_Info methods[] = {
    {0x0,&CSoh5r7k,&CS1cll1f}, //  <init> ()V
    {0x1,&CSu8x83a,&CSo0k4c2}, // Pub clone ()[Ljava/lang/Object;
    {0x1041,&CSu8x83a,&CSfeu9e1}, // PubVolBriSth clone ()Ljava/lang/Object;
};
static struct Class_Info info = {
    .access_flags = 0x20,
    .this_class = &CS98lkda,
    .super_class = &CSj09wq8,
    .interfaces_count = 2,
    .interfaces = &interfaces,
    .fields_count = 1,
    .fields = &fields,
    .methods_count = 3,
    .methods = &methods,
    .attributes_count = 0,
    .attributes = 0,
};
#endif /*CLASS_INFO*/
struct c_Array c_Array = {
    .C = { /*struct Class*/
        .obj_Size = sizeof(struct o_Array),
        .interfaces = i_Array,
        .name = "Array",
        .initialized = 0 },
    .M = { /*struct m_Array*/
        ._init_uph = /*override-incompatible pointer types*/&Array__init_uph,
        .equals_pw62vp = &java_lang_Object_equals_pw62vp,
        .hashCode_up4 = &java_lang_Object_hashCode_up4,
        .toString_w4s62z = &java_lang_Object_toString_w4s62z,
        .finalize_uph = &java_lang_Object_finalize_uph,
        .clone_Mz83ag3 = /*override-incompatible pointer types*/&Array_clone_Mz83ag3,
        .getClass_M234hr2 = &java_lang_Object_getClass_M234hr2,
        .notify_uph = &java_lang_Object_notify_uph,
        .notifyAll_uph = &java_lang_Object_notifyAll_uph,
        .wait_uph = &java_lang_Object_wait_uph,
        .wait_r3e7 = &java_lang_Object_wait_r3e7,
        .wait_ncjxw = &java_lang_Object_wait_ncjxw,
        .clone_Mw846ei = &Array_clone_Mw846ei,
    },
    .S = {
    }
};
struct c_Array_Array c_Array_Array = {
    .C = { /*struct Class*/
        .obj_Size = sizeof(struct a_Array),
        .interfaces = i_Array,
        .name = "Array[]",
        .initialized = 1 },
    .M = { /*struct m_Array*/
        ARRAY_METHODS
    },
};
