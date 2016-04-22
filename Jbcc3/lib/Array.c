#include "jbcc.h"
#include "Array.h"
#include "java/lang/Object.h"
#include "Array.h"
#include "java/lang/CloneNotSupportedException.h"
#include "java/lang/InternalError.h"
// <init> ()V
Void Array__init_uph(Array _0) {
Any _1, _2;
Lx4zaov: ;
	// line	3 Lx4zaov
	// ALOAD	0
_1.R.O = _0;
	// INVOKESPECIAL	java/lang/Object <init> ()V false
if (_1.R.O == null) jbcc_throw_NullPointerException();
((java_lang_Object)_1.R.O)->_C_->M._init_uph(_1.R.O);
Llwchh3: ;
	// line	4 Llwchh3
	// ALOAD	0
_1.R.O = _0;
	// ICONST_0
_2.I = 0;
	// PUTFIELD	Array length I
if (_1.R.O == null) jbcc_throw_NullPointerException();
((Array)_1.R.O)->length = _2.I;
Lgdn19l: ;
	// line	3 Lgdn19l
	// RETURN
return;
// DEAD CODE
}
// clone ()[Ljava/lang/Object;
Array_java_lang_Object Array_clone_Mw846ei(Array _0) {
Any _1, _2, _3, _4;
Li542me: ;
	// line	8 Li542me
	// ALOAD	0
_2.R.O = _0;
	// INVOKESPECIAL	java/lang/Object clone ()Ljava/lang/Object; false
if (_2.R.O == null) jbcc_throw_NullPointerException();
_2.R.O = ((java_lang_Object)_2.R.O)->_C_->M.clone_Mz83ag3(_2.R.O);
	// CHECKCAST	[Ljava/lang/Object;
jbcc_check_cast(_2.R.O, (struct Class*)&c__x5bLjava_lang_Object_x3b);
Lgjcqsz: ;
	// ARETURN
return _2.R.O;
Lwwnwqx: ;
	// line	9 Lwwnwqx
	// frame 4 local 0 stack 1
	// ASTORE	1
_1.R.O = _2.R.O;
Loh5r7k: ;
	// line	10 Loh5r7k
	// NEW	java/lang/InternalError
_2.R.O = jbcc_new((struct Class*)&c_java_lang_InternalError.C);
	// DUP
_3 = _2;
	// ALOAD	1
_4.R.O = _1.R.O;
	// INVOKEVIRTUAL	java/lang/CloneNotSupportedException getMessage ()Ljava/lang/String; false
if (_4.R.O == null) jbcc_throw_NullPointerException();
_4.R.O = ((java_lang_CloneNotSupportedException)_4.R.O)->_C_->M.getMessage_w4s62z(_4.R.O);
	// INVOKESPECIAL	java/lang/InternalError <init> (Ljava/lang/String;)V false
if (_3.R.O == null) jbcc_throw_NullPointerException();
((java_lang_InternalError)_3.R.O)->_C_->M._init_Mnmrpxd(_3.R.O,_4.R.O);
	// ATHROW
jbcc_throw(_2.R.O);
// DEAD CODE
}
// clone ()Ljava/lang/Object;
	// exceptions java/lang/CloneNotSupportedException
java_lang_Object Array_clone_Mz83ag3(Array _0) {
Any _1;
L1cll1f: ;
	// line	1 L1cll1f
	// ALOAD	0
_1.R.O = _0;
	// INVOKEVIRTUAL	Array clone ()[Ljava/lang/Object; false
if (_1.R.O == null) jbcc_throw_NullPointerException();
_1.A = ((Array)_1.R.O)->_C_->M.clone_Mw846ei(_1.R.O);
	// ARETURN
return _1.R.O;
}
#include "java/lang/Cloneable.h"
#include "java/io/Serializable.h"
static struct m_java_lang_Cloneable m_Array_java_lang_Cloneable = {
    /*override-incompatible pointer types*/&Array__init_uph,
    &java_lang_Object_equals_pw62vp,
    &java_lang_Object_hashCode_up4,
    &java_lang_Object_toString_w4s62z,
    &java_lang_Object_finalize_uph,
    /*override-incompatible pointer types*/&Array_clone_Mz83ag3,
    &java_lang_Object_getClass_M234hr2,
    &java_lang_Object_notify_uph,
    &java_lang_Object_notifyAll_uph,
    &java_lang_Object_wait_uph,
    &java_lang_Object_wait_r3e7,
    &java_lang_Object_wait_ncjxw,
};
static struct m_java_io_Serializable m_Array_java_io_Serializable = {
    /*override-incompatible pointer types*/&Array__init_uph,
    &java_lang_Object_equals_pw62vp,
    &java_lang_Object_hashCode_up4,
    &java_lang_Object_toString_w4s62z,
    &java_lang_Object_finalize_uph,
    /*override-incompatible pointer types*/&Array_clone_Mz83ag3,
    &java_lang_Object_getClass_M234hr2,
    &java_lang_Object_notify_uph,
    &java_lang_Object_notifyAll_uph,
    &java_lang_Object_wait_uph,
    &java_lang_Object_wait_r3e7,
    &java_lang_Object_wait_ncjxw,
};
static struct Interface_List_Entry i_Array[] = {
    {&c_java_lang_Cloneable, &m_Array_java_lang_Cloneable},
    {&c_java_io_Serializable, &m_Array_java_io_Serializable},
    {0, 0}
};
struct c_Array c_Array = {
    .C = { /*struct Class*/
        .obj_Size = sizeof(struct o_Array),
        .interfaces = i_Array,
        .name = "Array",
        .initialized = 0 },
    .M = { /*struct m_Array*/
        /*override-incompatible pointer types*/&Array__init_uph,
        &java_lang_Object_equals_pw62vp,
        &java_lang_Object_hashCode_up4,
        &java_lang_Object_toString_w4s62z,
        &java_lang_Object_finalize_uph,
        /*override-incompatible pointer types*/&Array_clone_Mz83ag3,
        &java_lang_Object_getClass_M234hr2,
        &java_lang_Object_notify_uph,
        &java_lang_Object_notifyAll_uph,
        &java_lang_Object_wait_uph,
        &java_lang_Object_wait_r3e7,
        &java_lang_Object_wait_ncjxw,
        &Array_clone_Mw846ei,
    },
    .S = {
    }
};
