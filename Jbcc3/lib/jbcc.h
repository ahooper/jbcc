#ifndef JBCC_H
#define JBCC_H
// anything that is #defined must have a _ in its name so it doesn't conflict
// with any translated Java identifier
#include <stdint.h>
//excerpt of needed defines from <math.h>, minimizing pollution
#if defined(__GNUC__)
#   define    HUGE_VAL     __builtin_huge_val()
#   define    HUGE_VALF    __builtin_huge_valf()
#   define    HUGE_VALL    __builtin_huge_vall()
#   define    FLOAT_NAN    __builtin_nanf("0x7fc00000")
#else
#   define    HUGE_VAL     1e500
#   define    HUGE_VALF    1e50f
#   define    HUGE_VALL    1e5000L
#   define    FLOAT_NAN    __nan()
#endif
extern double fmod (double numerator, double denominator);
extern float fmodf (float numerator, float denominator);
#include <setjmp.h>

typedef uint8_t		jboolean;
typedef int8_t		jbyte;
#define jbyte_MAX	INT8_MAX
#define jbyte_MIN	INT8_MIN
typedef uint16_t	jchar;
#define jchar_MAX	UINT16_MAX
#define jchar_MIN	UINT16_MIN
typedef double		jdouble;
typedef float		jfloat;
typedef int32_t		jint;
#define jint_MAX		INT32_MAX
#define jint_MIN		INT32_MIN
typedef int64_t		jlong;
#define jlong_MAX	INT64_MAX
#define jlong_MIN	INT64_MIN
typedef int16_t		jshort;
#define jshort_MAX	INT16_MAX
#define jshort_MIN	INT16_MIN
typedef void*		jobject;
typedef jint		jsize;

#define NULL_REFERENCE	((jobject)0)

typedef struct o_java_lang_Class *java_lang_Class;
typedef struct o_java_lang_Object *java_lang_Object;
typedef struct o_java_lang_String *java_lang_String;
typedef struct o_java_lang_Throwable *java_lang_Throwable;
typedef struct o_Array *Array;
typedef struct a_java_lang_Object *Array_java_lang_Object;
typedef java_lang_Class jclass;
typedef java_lang_String jstring;
typedef java_lang_Throwable jthrowable;

#define ABSTRACT_METHOD	NULL_REFERENCE

typedef char* Class_Info_UTF8z;

struct Class_Field_Info {
	jint access_flags;
	Class_Info_UTF8z *name;
	Class_Info_UTF8z *desc;
};
struct Class_Interface_Info {
	Class_Info_UTF8z *name;
};
struct Class_Method_Info {
	jint access_flags;
	Class_Info_UTF8z *name;
	Class_Info_UTF8z *desc;
};
struct Attribute_Info {
	Class_Info_UTF8z *name;
	Class_Info_UTF8z *desc;
};
struct Class_Info {
	jint					access_flags;
	Class_Info_UTF8z		this_class;
	Class_Info_UTF8z		super_class;
	jint					interfaces_count;
	struct Class_Interface_Info *interfaces;
	jint					fields_count;
	struct Class_Field_Info *fields;
	jint					methods_count;
	struct Class_Method_Info *methods;
	jint					attributes_count;
	struct Attribute_Info 	*attributes;
};

struct Class {
	int				obj_Size;
	struct Interface_List_Entry *interfaces;
	char			*name;
	java_lang_Class	klass;
	uint8_t			initialized;
	void			(*_clinit_)();
	struct Class_Info *class_info;
};

struct Interface_List_Entry {
	void* klass;
	void* methods;
};

typedef union {
    jdouble	D;
    jfloat	F;
    jint	I;
    jlong	L;
    void*	A;
    struct {
    	jobject	O;
    	void*	IM;  // interface method list
    }		R;
} Any;

typedef void*		LabelPtr;
typedef struct SwitchPair {
	jint			v;
	LabelPtr	l;
} SwitchPair;

typedef struct {
	java_lang_String S;
	jint 	L;
	jchar	C[];
} StringConst;

typedef struct monitor {
	// TODO
} Monitor;

struct catch_block {
	struct catch_block	*next;		// next deeper catch block
	sigjmp_buf			context;	// how to catch the exception
};
struct env_head {
	struct catch_block *catch_stack;
};
typedef struct env_head *jbcc_env;
struct catch_entry {
	void* 		thrownClass;
	LabelPtr	handler;
};
#define CATCH_PUSH(env,list)
#define CATCH_POP(env,list)

#define OBJECT_HEAD			\
	Monitor *monitor;

// from Array.h
#define ARRAY_HEAD			\
	jint length;

struct m_Array { // from Array.h
    void (*_init_uph)(jbcc_env,java_lang_Object);
    jboolean (*equals_pw62vp)(jbcc_env,java_lang_Object,java_lang_Object);
    jint (*hashCode_up4)(jbcc_env,java_lang_Object);
    java_lang_String (*toString_w4s62z)(jbcc_env,java_lang_Object);
    void (*finalize_uph)(jbcc_env,java_lang_Object);
    java_lang_Object (*clone_Mz83ag3)(jbcc_env,java_lang_Object);
    java_lang_Class (*getClass_M234hr2)(jbcc_env,java_lang_Object);
    void (*notify_uph)(jbcc_env,java_lang_Object);
    void (*notifyAll_uph)(jbcc_env,java_lang_Object);
    void (*wait_uph)(jbcc_env,java_lang_Object);
    void (*wait_r3e7)(jbcc_env,java_lang_Object,jlong);
    void (*wait_ncjxw)(jbcc_env,java_lang_Object,jlong,jint);
    Array_java_lang_Object (*clone_Mw846ei)(jbcc_env,Array);
};
extern void Array__init_uph(jbcc_env,Array);
extern Array_java_lang_Object Array_clone_Mw846ei(jbcc_env,Array);
extern java_lang_Object Array_clone_Mz83ag3(jbcc_env,Array);

// from Array.c
#define ARRAY_METHODS																	\
        ._init_uph = /*override-incompatible pointer types*/&Array__init_uph,			\
        .equals_pw62vp = &java_lang_Object_equals_pw62vp,								\
        .hashCode_up4 = &java_lang_Object_hashCode_up4,									\
        .toString_w4s62z = &java_lang_Object_toString_w4s62z,							\
        .finalize_uph = &java_lang_Object_finalize_uph,									\
        .clone_Mz83ag3 = /*override-incompatible pointer types*/&Array_clone_Mz83ag3,	\
        .getClass_M234hr2 = &java_lang_Object_getClass_M234hr2,							\
        .notify_uph = &java_lang_Object_notify_uph,										\
        .notifyAll_uph = &java_lang_Object_notifyAll_uph,								\
        .wait_uph = &java_lang_Object_wait_uph,											\
        .wait_r3e7 = &java_lang_Object_wait_r3e7,										\
        .wait_ncjxw = &java_lang_Object_wait_ncjxw,										\
        .clone_Mw846ei = &Array_clone_Mw846ei,
extern struct Interface_List_Entry i_Array[];

struct c_Array {
    struct Class C;
    struct m_Array M;
    struct {
    } S;
};

typedef struct {
	OBJECT_HEAD
	struct c_Array *_C_;
	ARRAY_HEAD
} Array_Common;
typedef struct {
	OBJECT_HEAD
	struct c_Array *_C_;
	ARRAY_HEAD
	jboolean	E[];
} *Array_jboolean;
typedef Array_jboolean jbooleanArray;
typedef struct {
	OBJECT_HEAD
	struct c_Array *_C_;
	ARRAY_HEAD
	jbyte	E[];
} *Array_jbyte;
typedef Array_jbyte jbyteArray;
typedef struct jchar_array {
	OBJECT_HEAD
	struct c_Array *_C_;
	ARRAY_HEAD
	jchar	E[];
} *Array_jchar;
typedef Array_jchar jcharArray;
typedef struct {
	OBJECT_HEAD
	struct c_Array *_C_;
	ARRAY_HEAD
	jdouble	E[];
} *Array_jdouble;
typedef Array_jdouble jdoubleArray;
typedef struct {
	OBJECT_HEAD
	struct c_Array *_C_;
	ARRAY_HEAD
	jfloat	E[];
} *Array_jfloat;
typedef Array_jfloat jfloatArray;
typedef struct {
	OBJECT_HEAD
	struct c_Array *_C_;
	ARRAY_HEAD
	jint	E[];
} *Array_jint;
typedef Array_jint jintArray;
typedef struct {
	OBJECT_HEAD
	struct c_Array *_C_;
	ARRAY_HEAD
	jlong	E[];
} *Array_jlong;
typedef Array_jlong jlongArray;
typedef struct {
	OBJECT_HEAD
	struct c_Array *_C_;
	ARRAY_HEAD
	jshort	E[];
} *Array_jshort;
typedef Array_jshort jshortArray;
typedef struct {
	OBJECT_HEAD
	struct c_Array *_C_;
	ARRAY_HEAD
	jobject	E[];
} *Array_jobject;
typedef Array_jobject jobjectArray;

extern struct c_Array c_Array_jboolean;
extern struct c_Array c_Array_jbyte;
extern struct c_Array c_Array_jchar;
extern struct c_Array c_Array_jdouble;
extern struct c_Array c_Array_jfloat;
extern struct c_Array c_Array_jint;
extern struct c_Array c_Array_jlong;
extern struct c_Array c_Array_jshort;

extern struct c_Array c_Array_Array_jboolean;
extern struct c_Array c_Array_Array_jbyte;
extern struct c_Array c_Array_Array_jchar;
extern struct c_Array c_Array_Array_jdouble;
extern struct c_Array c_Array_Array_jfloat;
extern struct c_Array c_Array_Array_jint;
extern struct c_Array c_Array_Array_jlong;
extern struct c_Array c_Array_Array_jshort;

extern jint jbcc_d2i(jdouble op);
extern jlong jbcc_d2l(jdouble op);
extern jint jbcc_f2i(jfloat op);
extern jlong jbcc_f2l(jfloat op);
extern jint jbcc_idiv(jint op1, jint op2);
extern jint jbcc_irem(jint op1, jint op2);
extern jlong jbcc_ldiv(jlong op1, jlong op2);
extern jlong jbcc_lrem(jlong op1, jlong op2);
extern void jbcc_check_cast(jobject obj,struct Class* klass);
extern void* jbcc_find_interface(void* objectjobject,void* interfaceClass);
extern java_lang_String jbcc_init_string_const(StringConst* scon);
extern java_lang_Class jbcc_get_type(struct Class* klass);
extern void jbcc_init_class(struct Class* klass);
extern jboolean jbcc_instanceof(jobject obj,struct Class* klass);
extern LabelPtr jbcc_lookupswitch(jint value, int length, SwitchPair table[], LabelPtr defalt);
extern void jbcc_monitor_enter(jobject obj);
extern void jbcc_monitor_exit(jobject obj);
extern jobject jbcc_new(struct Class* klass);
extern jobject jbcc_new_array_jboolean(jint length);
extern jobject jbcc_new_array_jbyte(jint length);
extern jobject jbcc_new_array_jchar(jint length);
extern jobject jbcc_new_array_jdouble(jint length);
extern jobject jbcc_new_array_jfloat(jint length);
extern jobject jbcc_new_array_jint(jint length);
extern jobject jbcc_new_array_jlong(jint length);
extern jobject jbcc_new_array_jshort(jint length);
extern jobject jbcc_new_array_object(jint length, struct Class* klass);
extern jobject jbcc_new_array_multi_jboolean(jint numdims, jint length, ... );
extern jobject jbcc_new_array_multi_jbyte(jint numdims, jint length, ... );
extern jobject jbcc_new_array_multi_jchar(jint numdims, jint length, ... );
extern jobject jbcc_new_array_multi_jdouble(jint numdims, jint length, ... );
extern jobject jbcc_new_array_multi_jfloat(jint numdims, jint length, ... );
extern jobject jbcc_new_array_multi_jint(jint numdims, jint length, ... );
extern jobject jbcc_new_array_multi_jlong(jint numdims, jint length, ... );
extern jobject jbcc_new_array_multi_jshort(jint numdims, jint length, ... );
extern jobject jbcc_new_array_multi_object(struct Class* klass, jint numdims, jint length, ... );
extern void jbcc_throw(java_lang_Throwable throwable);
extern void jbcc_throw_ArrayIndexOutOfBoundsException(jint index);
extern void jbcc_throw_DivisionByZero();
extern void jbcc_throw_NullPointerException();

// need this for static string constants in interfaces
#include "java/lang/String.h"

#endif /*JBCC_H*/
