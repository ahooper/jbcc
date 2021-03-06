package ca.nevdull.jbcc3;

public interface CCode {
	
	// C code names

	static final String LIB_H		= "jbcc.h";

	static final String CLASS_STRUCT_PREFIX		= "c_";
	static final String CLASS_INTERFACES_PREFIX	= "i_";
	static final String METHOD_STRUCT_PREFIX	= "m_";
	static final String OBJECT_STRUCT_PREFIX	= "o_";
	
	static final String CLASS_CLASS 			= "C";
	static final String CLASS_OBJ_SIZE			= "obj_Size";
	static final String CLASS_INTERFACES		= "interfaces";
	static final String CLASS_NAME				= "name";
	static final String CLASS_KLASS				= "klass";
	static final String CLASS_INITIALIZED		= "initialized";
	static final String CLASS_CLASS_INIT		= "_clinit_";
	static final String CLASS_METHOD_TABLE		= "M";
	static final String CLASS_STATIC_FIELDS		= "S";
	
	static final String OBJECT_CLASS			= "_C_";
	static final String OBJECT_CLASS_TYPE		= "struct Class";
	static final String OBJECT_HEAD	 			= "OBJECT_HEAD";

	static final String ARRAY_STRUCT_PREFIX		= "a_";
	static final String ARRAY_INTERFACES		= "i_Array";
	static final String ARRAY_METHODS_TYPE		= "struct m_Array";
	static final String ARRAY_METHODS			= "ARRAY_METHODS";

	static final String T_ARRAY_		= "Array_";
	static final String T_BOOLEAN		= "jboolean";
	static final String T_BYTE			= "jbyte";
	static final String T_CHAR			= "jchar";
	static final String T_DOUBLE		= "jdouble";
	static final String T_FLOAT			= "jfloat";
	static final String T_INT			= "jint";
	static final String T_LONG			= "jlong";
	static final String T_SHORT			= "jshort";
	static final String T_VOID			= "void";
	static final String T_ANY			= "Any";
	static final String T_OBJECT		= "jobject";
	static final String T_ARRAY_COMMON	= T_ARRAY_+"Common";

	static final String LABEL_PTR_TYPE	= "LabelPtr";
	static final String SWITCH_PAIR_TYPE = "SwitchPair";
	static final String STRING_CONST_TYPE = "StringConst";

	static final String ARRAY_ELEMENTS 	= "E";
	static final String ARRAY_LENGTH 	= "length";
	static final String ARRAY_HEAD	 	= "ARRAY_HEAD";
	static final String CHAR_ARRAY_TYPE	= "struct "+T_CHAR+"_array";
	
	static final String STRING_CONSTANT_STRING = "S";
	
	static final String METHOD_ABSTRACT	= "ABSTRACT_METHOD";
	
	static final String NULL_REFERENCE	= "NULL_REFERENCE";

	static final String FRAME			= "_";
	static final String FRAME_SWAP		= "_swap";
	static final String FRAME_ANY		= "";
	static final String FRAME_ARRAY		= ".A";
	static final String FRAME_DOUBLE	= ".D";
	static final String FRAME_FLOAT		= ".F";
	static final String FRAME_INT		= ".I";
	static final String FRAME_LONG		= ".L";
	static final String FRAME_REFER_OBJECT	= ".R.O";
	static final String FRAME_REFER_METHODS	= ".R.IM";

	static final String INTERFACE_METHODS = "im_";

	static final String LIB_D2I			= "jbcc_d2i";
	static final String LIB_D2L			= "jbcc_d2l";
	static final String LIB_F2I			= "jbcc_f2i";
	static final String LIB_F2L			= "jbcc_f2l";
	static final String LIB_DREM		= "fmod";
	static final String LIB_FREM		= "fmodf";
	static final String LIB_IDIV		= "jbcc_idiv";
	static final String LIB_IREM		= "jbcc_irem";
	static final String LIB_LDIV		= "jbcc_ldiv";
	static final String LIB_LREM		= "jbcc_lrem";

	static final String LIB_CHECK_CAST	= "jbcc_check_cast";
	static final String LIB_GET_TYPE	= "jbcc_get_type";
	static final String LIB_INIT_STRING_CONST = "jbcc_init_string_const";
	static final String LIB_INIT_CLASS	= "jbcc_init_class";
	static final String LIB_INSTANCEOF	= "jbcc_instanceof";
	static final String LIB_LOOKUPSWITCH = "jbcc_lookupswitch";
	static final String LIB_MONITOR_ENTER = "jbcc_monitor_enter";
	static final String LIB_MONITOR_EXIT = "jbcc_monitor_exit";
	static final String LIB_NEW			= "jbcc_new";
	static final String LIB_NEW_ARRAY_ 	= "jbcc_new_array_";
	static final String LIB_NEW_ARRAY_OBJECT = "jbcc_new_array_object";
	static final String LIB_NEW_ARRAY_MULTI_ = "jbcc_new_array_multi_";
	static final String LIB_THROW		= "jbcc_throw";
	static final String LIB_THROW_ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION = "jbcc_throw_ArrayIndexOutOfBoundsException";
	static final String LIB_THROW_DIVISION_BY_ZERO = "jbcc_throw_DivisionByZero";
	static final String LIB_THROW_NULL_POINTER_EXCEPTION = "jbcc_throw_NullPointerException";

	static final String OPT_CLASS_INFO				= "CLASS_INFO";
	static final String CLASS_INFO_TYPE				= "struct Class_Info";
	static final String CLASS_INFO_ATTRIBUTE_TYPE	= "struct Attribute_Info";
	static final String CLASS_INFO_FIELD_TYPE		= "struct Class_Field_Info";
	static final String CLASS_INFO_INTERFACE_TYPE	= "struct Class_Interface_Info";	
	static final String CLASS_INFO_METHOD_TYPE		= "struct Class_Method_Info";
	static final String CLASS_INFO_UTF8Z_TYPE		= "Class_Info_UTF8z";

	static final String JAVA_LANG_STRING			= "java_lang_String";
	
	static final String ENV_TYPE					= "jbcc_env";
	static final String ENV_ARG						= "env";
	static final String CATCH_LIST_TYPE				= "struct catch_entry";
	static final String CATCH_LIST_PREFIX			= "catch";
	static final String CATCH_PUSH					= "CATCH_PUSH";
	static final String CATCH_POP					= "CATCH_POP";
	
	/** Get portion of string safe to include in a C // comment
	 * 
	 * @param s
	 * @return
	 */
	static String safeCommentSubstring(String s) {
		int trouble = s.indexOf('\n');
		if (trouble >= 0) s = s.substring(0,trouble);
		trouble = s.indexOf('\r');
		if (trouble >= 0) s = s.substring(0,trouble);
		trouble = s.indexOf('\\');
		if (trouble >= 0) s = s.substring(0,trouble);
		return s;
	}

}
