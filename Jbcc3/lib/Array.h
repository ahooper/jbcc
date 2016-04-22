#ifndef H_Array
#define H_Array
#include "java/lang/Object.h"
typedef struct o_Array *Array;
typedef struct a_Array *Array_Array;
typedef struct o_java_lang_Cloneable *java_lang_Cloneable;
typedef struct a_java_lang_Cloneable *Array_java_lang_Cloneable;
typedef struct o_java_io_Serializable *java_io_Serializable;
typedef struct a_java_io_Serializable *Array_java_io_Serializable;
extern Void Array__init_uph(Array);
typedef struct o_java_lang_Object *java_lang_Object;
typedef struct a_java_lang_Object *Array_java_lang_Object;
extern Array_java_lang_Object Array_clone_Mw846ei(Array);
extern java_lang_Object Array_clone_Mz83ag3(Array);
struct m_Array {
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
extern struct c_Array {
    struct Class C;
    struct m_Array M;
    struct {
    } S;
} c_Array;
struct o_Array {
    struct c_Array *_C_;
    Monitor *monitor;
    Int length;
};
struct a_Array{
    Array_Head H;
    Array E[];
};
#endif /*H_Array*/
