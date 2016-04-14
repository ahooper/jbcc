package ca.nevdull.jbcc3;

public interface CCode {
	
	// C code names

	static final String LIB_H		= "jbcc.h";

	static final String CLASS_STRUCT_PREFIX		= "c_";
	static final String METHOD_STRUCT_PREFIX	= "m_";
	static final String OBJECT_STRUCT_PREFIX	= "o_";
	static final String CLASS_CLASS 			= "C";
	static final String CLASS_STATIC_FIELDS		= "S";
	static final String CLASS_METHOD_TABLE		= "M";
	static final String OBJECT_CLASS			= "_C_";
	static final String CLASS_CLASS_STRUCT		= "Class";

	static final String T_ARRAY_	= "Array_";
	static final String T_BOOLEAN	= "Boolean";
	static final String T_BYTE		= "Byte";
	static final String T_CHAR		= "Char";
	static final String T_DOUBLE	= "Double";
	static final String T_FLOAT		= "Float";
	static final String T_INT		= "Int";
	static final String T_LONG		= "Long";
	static final String T_SHORT		= "Short";
	static final String T_VOID		= "Void";
	static final String T_STACK		= "Stack";
	static final String T_ARRAY_HEAD	= T_ARRAY_+"Head";

	static final String MONITOR		= "Monitor *monitor;";
	static final String LABEL_PTR	= "LabelPtr";
	static final String SWITCH_PAIR	= "SwitchPair";
	static final String STRING_CONST = "StringConst";

	static final String ARRAY_ELEMENTS = "E";
	static final String ARRAY_LENGTH = "L";

	static final String STRING_CONSTANT_STRING = "S";

	static final String FRAME			= "_";
	static final String FRAME_SWAP		= "_swap";
	static final String FRAME_ANY		= "";
	static final String FRAME_ARRAY		= ".A";
	static final String FRAME_DOUBLE	= ".D";
	static final String FRAME_FLOAT		= ".F";
	static final String FRAME_INT		= ".I";
	static final String FRAME_LONG		= ".L";
	static final String FRAME_OBJECT	= ".O";

	static final String LIB_D2I		= "jbcc_d2i";
	static final String LIB_D2L		= "jbcc_d2l";
	static final String LIB_F2I		= "jbcc_f2i";
	static final String LIB_F2L		= "jbcc_f2l";
	static final String LIB_IDIV	= "jbcc_idiv";
	static final String LIB_IREM	= "jbcc_irem";
	static final String LIB_LDIV	= "jbcc_ldiv";
	static final String LIB_LREM	= "jbcc_lrem";

	static final String LIB_CHECK_CAST = "jbcc_check_cast";
	static final String LIB_INIT_STRING_CONST = "jbcc_initStringConst";
	static final String LIB_INSTANCEOF = "jbcc_instanceof";
	static final String LIB_MONITOR_EXIT = "jbcc_monitor_enter";
	static final String LIB_MONITOR_ENTER = "jbcc_monitor_exit";
	static final String LIB_NEW		= "jbcc_new";
	static final String LIB_NEW_ARRAY_ = "jbcc_new_array_";
	static final String LIB_NEW_ARRAY_OBJECT = "jbcc_new_array_object";
	static final String LIB_THROW	= "jbcc_throw";
	static final String LIB_THROW_ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION = "jbcc_ThrowArrayIndexOutOfBoundsException";
	static final String LIB_THROW_DIVISION_BY_ZERO = "jbcc_throwDivisionByZero";
	static final String LIB_THROW_NULL_POINTER_EXCEPTION = "jbcc_throwNullPointerException";

}