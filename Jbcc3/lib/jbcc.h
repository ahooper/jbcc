#include <stdint.h>
#include <math.h>
#ifndef JBCC_H
#define JBCC_H

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
typedef void*		Reference;
#define null		((Reference)0)
typedef void		Void;

typedef struct o_java_lang_Class *java_lang_Class;
typedef struct o_java_lang_Object *java_lang_Object;
typedef struct o_java_lang_String *java_lang_String;
typedef struct o_java_lang_Throwable *java_lang_Throwable;

typedef void*	Object;

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
    Object O;
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

// TODO Arrays have to be subclasses of java/lang/Object
typedef struct {
	Int C;
	Int L;
} Array_Head;
typedef struct {
	Array_Head H;
	Boolean	E[];
} *Array_Boolean;
typedef struct {
	Array_Head H;
	Byte	E[];
} *Array_Byte;
typedef struct Char_array {
	Array_Head H;
	Char	E[];
} *Array_Char;
typedef struct {
	Array_Head H;
	Double	E[];
} *Array_Double;
typedef struct {
	Array_Head H;
	Float	E[];
} *Array_Float;
typedef struct {
	Array_Head H;
	Int	E[];
} *Array_Int;
typedef struct {
	Array_Head H;
	Long	E[];
} *Array_Long;
typedef struct {
	Array_Head H;
	Short	E[];
} *Array_Short;
typedef struct {
	Array_Head H;
	Reference	E[];
} *Array_Reference;

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
