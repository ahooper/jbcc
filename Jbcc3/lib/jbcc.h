#ifndef JBCC_H
#define JBCC_H
#include <stdint.h>
//excerpt from <math.h> minimizing polution
#if defined(__GNUC__)
#   define    HUGE_VAL     __builtin_huge_val()
#   define    HUGE_VALF    __builtin_huge_valf()
#   define    HUGE_VALL    __builtin_huge_vall()
#   define    NAN          __builtin_nanf("0x7fc00000")
#else
#   define    HUGE_VAL     1e500
#   define    HUGE_VALF    1e50f
#   define    HUGE_VALL    1e5000L
#   define    NAN          __nan()
#endif

typedef uint8_t		Boolean;
typedef int8_t		Byte;
#define Byte_MAX	INT8_MAX
#define Byte_MIN	INT8_MIN
typedef uint16_t	Char;
#define Char_MAX	UINT16_MAX
#define Char_MIN	UINT16_MIN
typedef double		Double;
typedef float		Float;
typedef int32_t		Int;
#define Int_MAX		INT32_MAX
#define Int_MIN		INT32_MIN
typedef int64_t		Long;
#define Long_MAX	INT64_MAX
#define Long_MIN	INT64_MIN
typedef int16_t		Short;
#define Short_MAX	INT16_MAX
#define Short_MIN	INT16_MIN
typedef void		Void;
typedef void*		Reference;

#define NULL_REFERENCE	((Reference)0)
// anything that is #defined must have a _ in its name so it doesn't conflict
// with any translated Java identifier

typedef struct o_java_lang_Class *java_lang_Class;
typedef struct o_java_lang_Object *java_lang_Object;
typedef struct o_java_lang_String *java_lang_String;
typedef struct o_java_lang_Throwable *java_lang_Throwable;
typedef struct o_Array *Array;
typedef struct a_java_lang_Object *Array_java_lang_Object;

typedef void*	Object;
#define ABSTRACT_METHOD	NULL_REFERENCE

struct Interface_List_Entry {
	void* klass;
	void* methods;
};

struct Class {
	int obj_Size;
	struct Interface_List_Entry *interfaces;
	char *name;
	java_lang_Class klass;
	uint8_t initialized;
	Void (*_clinit_)();
};

typedef union {
    Double D;
    Float F;
    Int I;
    Long L;
    void* A;
    struct {
    	Object O;
    	void* M;  // interface method list
    } R;
} Any;

typedef void*		LabelPtr;
typedef struct SwitchPair {
	Int v;
	LabelPtr l;
} SwitchPair;

typedef struct {
	java_lang_String S;
	Int 	L;
	Char	C[];
} StringConst;

typedef struct monitor {
	// TODO
} Monitor;

#define OBJECT_HEAD			\
	Monitor *monitor;

// from Array.h
#define ARRAY_HEAD			\
	Int length;

struct m_Array { // from Array.h
    Void (*_init_uph)(java_lang_Object);
    Boolean (*equals_pw62vp)(java_lang_Object,java_lang_Object);
    Int (*hashCode_up4)(java_lang_Object);
    java_lang_String (*toString_w4s62z)(java_lang_Object);
    Void (*finalize_uph)(java_lang_Object);
    java_lang_Object (*clone_Mz83ag3)(java_lang_Object);
    java_lang_Class (*getClass_M234hr2)(java_lang_Object);
    Void (*notify_uph)(java_lang_Object);
    Void (*notifyAll_uph)(java_lang_Object);
    Void (*wait_uph)(java_lang_Object);
    Void (*wait_r3e7)(java_lang_Object,Long);
    Void (*wait_ncjxw)(java_lang_Object,Long,Int);
    Array_java_lang_Object (*clone_Mw846ei)(Array);
};
extern Void Array__init_uph(Array);
extern Array_java_lang_Object Array_clone_Mw846ei(Array);
extern java_lang_Object Array_clone_Mz83ag3(Array);

// from Array.c
#define ARRAY_METHODS													\
        /*override-incompatible pointer types*/&Array__init_uph,		\
        &java_lang_Object_equals_pw62vp,								\
        &java_lang_Object_hashCode_up4,									\
        &java_lang_Object_toString_w4s62z,								\
        &java_lang_Object_finalize_uph,									\
        /*override-incompatible pointer types*/&Array_clone_Mz83ag3,	\
        &java_lang_Object_getClass_M234hr2,								\
        &java_lang_Object_notify_uph,									\
        &java_lang_Object_notifyAll_uph,								\
        &java_lang_Object_wait_uph,										\
        &java_lang_Object_wait_r3e7,									\
        &java_lang_Object_wait_ncjxw,									\
        &Array_clone_Mw846ei,
extern struct Interface_List_Entry i_Array[];

struct c_Array {
    struct Class C;
    struct m_Array M;
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
	Boolean	E[];
} *Array_Boolean;
typedef struct {
	OBJECT_HEAD
	struct c_Array *_C_;
	ARRAY_HEAD
	Byte	E[];
} *Array_Byte;
typedef struct Char_array {
	OBJECT_HEAD
	struct c_Array *_C_;
	ARRAY_HEAD
	Char	E[];
} *Array_Char;
typedef struct {
	OBJECT_HEAD
	struct c_Array *_C_;
	ARRAY_HEAD
	Double	E[];
} *Array_Double;
typedef struct {
	OBJECT_HEAD
	struct c_Array *_C_;
	ARRAY_HEAD
	Float	E[];
} *Array_Float;
typedef struct {
	OBJECT_HEAD
	struct c_Array *_C_;
	ARRAY_HEAD
	Int	E[];
} *Array_Int;
typedef struct {
	OBJECT_HEAD
	struct c_Array *_C_;
	ARRAY_HEAD
	Long	E[];
} *Array_Long;
typedef struct {
	OBJECT_HEAD
	struct c_Array *_C_;
	ARRAY_HEAD
	Short	E[];
} *Array_Short;
typedef struct {
	OBJECT_HEAD
	struct c_Array *_C_;
	ARRAY_HEAD
	Reference	E[];
} *Array_Reference;

extern struct Class Class_Boolean;
extern struct Class Class_Byte;
extern struct Class Class_Char;
extern struct Class Class_Double;
extern struct Class Class_Float;
extern struct Class Class_Int;
extern struct Class Class_Long;
extern struct Class Class_Short;

extern struct Class Class_Array_Boolean;
extern struct Class Class_Array_Byte;
extern struct Class Class_Array_Char;
extern struct Class Class_Array_Double;
extern struct Class Class_Array_Float;
extern struct Class Class_Array_Int;
extern struct Class Class_Array_Long;
extern struct Class Class_Array_Short;

extern struct Class Class_Array_Array_Boolean;
extern struct Class Class_Array_Array_Byte;
extern struct Class Class_Array_Array_Char;
extern struct Class Class_Array_Array_Double;
extern struct Class Class_Array_Array_Float;
extern struct Class Class_Array_Array_Int;
extern struct Class Class_Array_Array_Long;
extern struct Class Class_Array_Array_Short;

extern struct c_Array c_Array_Char;

extern Int jbcc_d2i(Double op);
extern Long jbcc_d2l(Double op);
extern Int jbcc_f2i(Float op);
extern Long jbcc_f2l(Float op);
extern Int jbcc_idiv(Int op1, Int op2);
extern Int jbcc_irem(Int op1, Int op2);
extern Long jbcc_ldiv(Long op1, Long op2);
extern Long jbcc_lrem(Long op1, Long op2);
extern void jbcc_check_cast(Reference obj,struct Class* klass);
extern void* jbcc_find_interface(void* objectReference,void* interfaceClass);
extern java_lang_String jbcc_init_string_const(StringConst* scon);
extern void jbcc_init_class(struct Class* klass);
extern Boolean jbcc_instanceof(Reference obj,struct Class* klass);
extern LabelPtr jbcc_lookupswitch(Int value, int length, SwitchPair table[], LabelPtr defalt);
extern void jbcc_monitor_enter(Reference obj);
extern void jbcc_monitor_exit(Reference obj);
extern Reference jbcc_new(struct Class* klass);
extern Reference jbcc_new_array_Boolean(Int count);
extern Reference jbcc_new_array_Byte(Int count);
extern Reference jbcc_new_array_Char(Int count);
extern Reference jbcc_new_array_Double(Int count);
extern Reference jbcc_new_array_Float(Int count);
extern Reference jbcc_new_array_Int(Int count);
extern Reference jbcc_new_array_Long(Int count);
extern Reference jbcc_new_array_Short(Int count);
extern Reference jbcc_new_array_object(Int count, struct Class* klass);
extern void jbcc_throw(java_lang_Throwable throwable);
extern void jbcc_throw_ArrayIndexOutOfBoundsException(Int index);
extern void jbcc_throw_DivisionByZero();
extern void jbcc_throw_NullPointerException();

#endif /*JBCC_H*/
