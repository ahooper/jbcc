#ifndef H_Array
#define H_Array
#include "java/lang/Object.h"
typedef struct o_Array *Array;
typedef struct a_Array *Array_Array;
extern void Array__init_uph(jbcc_env,Array);
typedef struct o_java_lang_Object *java_lang_Object;
typedef struct a_java_lang_Object *Array_java_lang_Object;
typedef struct o_java_lang_CloneNotSupportedException *java_lang_CloneNotSupportedException;
typedef struct a_java_lang_CloneNotSupportedException *Array_java_lang_CloneNotSupportedException;
extern Array_java_lang_Object Array_clone_Mw846ei(jbcc_env,Array);
extern java_lang_Object Array_clone_Mz83ag3(jbcc_env,Array);
typedef struct o_java_lang_Cloneable *java_lang_Cloneable;
typedef struct a_java_lang_Cloneable *Array_java_lang_Cloneable;
typedef struct o_java_io_Serializable *java_io_Serializable;
typedef struct a_java_io_Serializable *Array_java_io_Serializable;
struct m_Array {
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
extern struct c_Array {
    struct Class C;
    struct m_Array M;
    struct {
    } S;
} c_Array;
extern struct c_Array_Array {
    struct Class C;
    struct m_Array M;
} c_Array_Array;
struct o_Array {
    OBJECT_HEAD
    struct c_Array *_C_;
    jint length;
};
struct a_Array{
    OBJECT_HEAD
    struct c_Array_Array *_C_;
    ARRAY_HEAD
    Array E[];
};
#endif /*H_Array*/
