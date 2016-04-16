package ca.nevdull.jbcc2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Formatter;
import java.util.HashMap;
import org.apache.commons.bcel6.Const;
import org.apache.commons.bcel6.classfile.Attribute;
import org.apache.commons.bcel6.classfile.Code;
import org.apache.commons.bcel6.classfile.Constant;
import org.apache.commons.bcel6.classfile.ConstantClass;
import org.apache.commons.bcel6.classfile.ConstantString;
import org.apache.commons.bcel6.classfile.ConstantUtf8;
import org.apache.commons.bcel6.classfile.ExceptionTable;
import org.apache.commons.bcel6.classfile.Field;
import org.apache.commons.bcel6.classfile.JavaClass;
import org.apache.commons.bcel6.classfile.Method;
import org.apache.commons.bcel6.classfile.Utility;
import org.apache.commons.bcel6.generic.*;

public class CompiledClass extends org.apache.commons.bcel6.generic.EmptyVisitor {
	private JavaClass klass;
    private PrintWriter out;
    private String className;
    ConstantPoolGen constPool;
	String convClassName;
	private Main main;
	
	// C code names

	private static final String LIB_H		= "jbcc.h";

	private static final String CLASS_STRUCT_PREFIX		= "c_";
	private static final String METHOD_STRUCT_PREFIX	= "m_";
	private static final String OBJECT_STRUCT_PREFIX	= "o_";
	private static final String CLASS_CLASS 			= "C";
	private static final String CLASS_STATIC_FIELDS		= "S";
	private static final String CLASS_METHOD_TABLE		= "M";
	private static final String OBJECT_CLASS			= "_C_";
	private static final String CLASS_CLASS_STRUCT		= "Class";

	private static final String T_ARRAY_	= "Array_";
	private static final String T_BOOLEAN	= "Boolean";
	private static final String T_BYTE		= "Byte";
	private static final String T_CHAR		= "Char";
	private static final String T_DOUBLE	= "Double";
	private static final String T_FLOAT		= "Float";
	private static final String T_INT		= "Int";
	private static final String T_LONG		= "Long";
	private static final String T_SHORT		= "Short";
	private static final String T_VOID		= "Void";
	private static final String T_STACK		= "Stack";
	private static final String T_ARRAY_HEAD	= T_ARRAY_+"Head";

	private static final String MONITOR		= "Monitor *monitor;";
	private static final String LABEL_PTR	= "LabelPtr";
	private static final String SWITCH_PAIR	= "SwitchPair";
	private static final String STRING_CONST = "StringConst";

	private static final String ARRAY_ELEMENTS = "E";
	private static final String ARRAY_LENGTH = "L";

	private static final String STRING_CONSTANT_STRING = "S";

	private static final String STACK		= "_";
	private static final String STACK_SWAP	= "_swap";
	private static final char STACK_DOUBLE	= 'd';
	private static final char STACK_FLOAT	= 'f';
	private static final char STACK_INT		= 'i';
	private static final char STACK_LONG	= 'l';
	private static final char STACK_REFER	= 'a';

	private static final String LIB_D2I		= "jbcc_d2i";
	private static final String LIB_D2L		= "jbcc_d2l";
	private static final String LIB_F2I		= "jbcc_f2i";
	private static final String LIB_F2L		= "jbcc_f2l";
	private static final String LIB_IDIV	= "jbcc_idiv";
	private static final String LIB_IREM	= "jbcc_irem";
	private static final String LIB_LDIV	= "jbcc_ldiv";
	private static final String LIB_LREM	= "jbcc_lrem";

	private static final String LIB_CHECK_CAST = "jbcc_check_cast";
	private static final String LIB_INIT_STRING_CONST = "jbcc_initStringConst";
	private static final String LIB_INSTANCEOF = "jbcc_instanceof";
	private static final String LIB_MONITOR_EXIT = "jbcc_monitor_enter";
	private static final String LIB_MONITOR_ENTER = "jbcc_monitor_exit";
	private static final String LIB_NEW		= "jbcc_new";
	private static final String LIB_NEW_ARRAY_ = "jbcc_new_array_";
	private static final String LIB_NEW_ARRAY_OBJECT = "jbcc_new_array_object";
	private static final String LIB_THROW	= "jbcc_throw";
	private static final String LIB_THROW_ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION = "jbcc_ThrowArrayIndexOutOfBoundsException";
	private static final String LIB_THROW_DIVISION_BY_ZERO = "jbcc_throwDivisionByZero";
	private static final String LIB_THROW_NULL_POINTER_EXCEPTION = "jbcc_throwNullPointerException";

    public CompiledClass(JavaClass klass, Main main) {
        this.klass = klass;
        this.main = main;
        this.out = main.opt_out;
        this.className = klass.getClassName();
        this.constPool = new ConstantPoolGen(klass.getConstantPool());
		this.convClassName = convertClassName(className);
		this.analyzer = new StackAnalyzer(klass, out);
     }
    
	public void header(File file) throws IOException {
		setOut(file);
		
		out.println("#ifndef H_"+convClassName);
		out.println("#define H_"+convClassName);
		String superclassName = klass.getSuperclassName();
		if (!superclassName.equals(className)) {
			out.println("#include \""+classHeaderFileName(superclassName)+"\"");
		}
        
		// referenced class names
		int cpsize = constPool.getSize();
        for (int i = 0;  i < cpsize;  i++) {
        	Constant con = constPool.getConstant(i);
		    if (con != null && con.getTag() == Const.CONSTANT_Class) {
		        ConstantUtf8 cnb = (ConstantUtf8) constPool.getConstant(((ConstantClass) con).getNameIndex());
		        String referencedClassName = cnb.getBytes();
		        if (referencedClassName.startsWith("[")) continue;  // skip array references  //TODO
		        String crcn = convertClassName(referencedClassName.replace('/', '.'));
		        out.print("typedef struct "+OBJECT_STRUCT_PREFIX);
		        out.print(crcn);
		        out.print(" *");
		        out.print(crcn);
		        out.println(";");
				out.println("#include \""+classHeaderFileName(referencedClassName.replace('/', '.'))+"\"");
		    }
		}
				
		// method declarations
        for (Method method : klass.getMethods()) {
     		declareMethod(method);
        }
        
        // virtual method table
        out.print("struct "+METHOD_STRUCT_PREFIX);
        out.print(convClassName);
        out.println(" {");
        for (Method method : klass.getMethods()) {
			 //TODO superclass methods that are not overridden
        	if (!method.isStatic()) {
        		out.print("    ");
        		declareMethodPointer(method);
        	}
        }
        out.println("};");
        
        // class structure
        out.print("extern struct "+CLASS_STRUCT_PREFIX);
        out.print(convClassName);
        out.println(" {");
        out.println("    struct "+CLASS_CLASS_STRUCT+" "+CLASS_CLASS+";");
        out.print("    struct "+METHOD_STRUCT_PREFIX);
        out.print(convClassName);
        out.println(" "+CLASS_METHOD_TABLE+";");
        out.println("    struct {");
        for (Field field : klass.getFields()) {
        	if (field.isStatic()) {
        		out.print("    ");
        		declareField(field);
        	}
        }
        out.println("    } "+CLASS_STATIC_FIELDS+";");
        out.print("} "+CLASS_STRUCT_PREFIX);
        out.print(convClassName);
        out.println(";");
        
        // instance field declarations
        out.print("struct "+OBJECT_STRUCT_PREFIX);
        out.print(convClassName);
        out.println(" {");
        out.print("    struct "+CLASS_STRUCT_PREFIX);
        out.print(convClassName);
        out.println(" *"+OBJECT_CLASS+";");
        out.println("    "+MONITOR);
        for (Field field : klass.getFields()) {
        	if (!field.isStatic()) {
        		out.print("    ");
        		declareField(field);
        	}
        }
        out.println("};");
        //TODO inherited fields
        
		out.println("#endif /*H_"+convClassName+"*/");
		
		out.flush();
		if (file != null) out.close();
		
	}

	private void setOut(File file) throws IOException {
		if (file != null) {
			File parent = file.getParentFile();
			if (parent != null) parent.mkdirs();
			this.out = new PrintWriter(file);
		}
	}

	private String classHeaderFileName(String className) {
		StringBuilder fileName = new StringBuilder();
		int w = 0;
		for (int x; (x = className.indexOf('.',w)) >= 0;  w = x+1) {
			fileName.append(escapeName(className.substring(w,x))).append('/');
		}
		fileName.append(escapeName(className.substring(w))).append(main.FILE_SUFFIX_HEADER);
		return fileName.toString();
	}

	static String escapeName(String name) {
		StringBuilder sb = new StringBuilder();
		Formatter formatter = null;  // only created if needed
		int w = 0, l = name.length();
		for (int x;  ;  w = x + 1) {
			char c = 0;
			for (x = w; x < l; x++) {
				c = name.charAt(x);
				if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'z'|| c >= 'A' && c <= 'Z')) break;
			}
			if (x > w) sb.append(name.substring(w, x));
			if (x >= l) break;
			if (c == '.') {
				sb.append("_");  // . changed to _
			} else if (c == '_') {
				sb.append("__");  // double up underscore
			} else {
				if (formatter == null) formatter = new Formatter(sb); // formatter is now needed
				formatter.format("_u%04x",(int)c);  // escape Unicode
			}
		}
		return sb.toString();
	}

	static String convertClassName(String name) {
		return escapeName(name);
	}

	private String convertClassName(ObjectType objType) {
		return convertClassName(objType.getClassName());
	}
	
	static String convertType(Type type) {
		if (type == Type.BOOLEAN) return T_BOOLEAN;
		if (type == Type.BYTE) return T_BYTE;
		if (type == Type.CHAR) return T_CHAR;
		if (type == Type.DOUBLE) return T_DOUBLE;
		if (type == Type.FLOAT) return T_FLOAT;
		if (type == Type.INT) return T_INT;
		if (type == Type.LONG) return T_LONG;
		if (type == Type.SHORT) return T_SHORT;
		if (type == Type.VOID) return T_VOID;
		if (type instanceof ArrayType) return T_ARRAY_+convertType(((ArrayType)type).getElementType());
		if (type instanceof ObjectType) return convertClassName(((ObjectType)type).getClassName());
		throw new RuntimeException("Unexpected convertType class "+type.getClass());
	}
    
	private void declareMethod(Method method) {
		putMethodPrototype(method, convertMethodName(convClassName+"_", method));
	}

	private void putMethodPrototype(Method method, String name) {
		out.print(convertType(method.getReturnType()));
		out.print(" ");
		out.print(name);
		out.print("(");
		String sep = "";
		if (!method.isStatic()) {
			out.print(convClassName);
			sep = ",";
		}
		for (Type argType : method.getArgumentTypes()) {
			out.print(sep);  sep = ",";
			out.print(convertType(argType));
		}
		out.println(");");
	}
    
	private void declareMethodPointer(Method method) {
		putMethodPrototype(method, "(*"+convertMethodName("", method)+")");
	}

	private String convertMethodName(String prefix, Method method) {
		return convertMethodName(prefix, method.getName(), method.getSignature());
	}

	private String convertMethodName(String prefix, String methodName, String signature) {
		int sigHash = signature.hashCode();
		String sig = (sigHash >= 0) ? Integer.toString(sigHash, 36)
				 					: "M"+Integer.toString(-sigHash, 36);
		if (Character.isJavaIdentifierStart(methodName.charAt(0))) {
			// normal methods
			return prefix+escapeName(methodName)+"_"+sig;			
		} else if (methodName.equals(Const.CONSTRUCTOR_NAME)) {
			return prefix+"_init_"+sig;
		} else if (methodName.equals(Const.STATIC_INITIALIZER_NAME)) {
			return prefix+"_clinit_"+sig;
		} else {
			return prefix+escapeName(methodName);			
		}
	}

	private void declareField(Field field) {
		String fName = field.getName();
		Type fType = field.getType();
		out.print(convertType(fType));
		out.print(" ");
		out.print(escapeName(fName));			
		out.println(";");
	}

	private HashMap<Constant, String> stringConstants = new HashMap<Constant, String>();

	public void code(File file) throws IOException {
		setOut(file);
		
		out.println("#include \""+LIB_H+"\"");
		out.println("#include \""+classHeaderFileName(className)+"\"");
        
		// String constants
		int cpsize = constPool.getSize();
        for (int i = 0;  i < cpsize;  i++) {
        	Constant con = constPool.getConstant(i);
		    if (con != null && con.getTag() == Const.CONSTANT_String) {
                ConstantUtf8 cons = (ConstantUtf8) constPool.getConstant(((ConstantString) con).getStringIndex());
		        String scon = cons.getBytes();
		        String scn = STRING_CONSTANT_STRING+Integer.toString(i,36);
		        out.print("static ");
		        out.print(STRING_CONST);
		        out.print(" ");
		        out.print(scn);
		        out.print(" = {0,");
		        out.print(scon.length());
		        out.print(",{");
		        String sep = "";
		        for (char ch : scon.toCharArray()) {out.print(sep); sep = ","; out.print((int)ch); }
		        out.print("}}; // ");
		        int newline = scon.indexOf('\n');
		        out.println((newline < 0) ? scon : scon.substring(0,newline));
		        stringConstants.put(con,scn);
		    }
		}
        
		// method definitions
        for (Method method : klass.getMethods()) {
            defineMethod(method);
        }
        
		 // class structure
		 out.print("struct "+CLASS_STRUCT_PREFIX);
		 out.print(convClassName);
		 out.print(" "+CLASS_STRUCT_PREFIX);
		 out.print(convClassName);
		 out.println(" = {");
		 out.println("    /*struct "+CLASS_CLASS_STRUCT+"*/ {");
		 out.print("        sizeof(struct "+OBJECT_STRUCT_PREFIX);
		 out.print(convClassName);
		 out.println("),");
		 out.print("        \"");
		 out.print(className);
		 out.println("\" },");
		 out.print("    /*struct "+METHOD_STRUCT_PREFIX);
		 out.print(convClassName);
		 out.println("*/ {");
		 for (Method method : klass.getMethods()) {
			 //TODO superclass methods that are not overridden
			 if (!method.isStatic()) {
				 out.print("        ");
				 out.print(convertMethodName(convClassName+"_", method));
				 out.println(",");
			 }
		 }
		 out.println("    }");
		 out.println("};");
			
		out.flush();
		if (file != null) out.close();
		
	}

	private void defineMethod(Method method) {
        out.println("\t// method " + Utility.accessToString(method.getAccessFlags()) +
                " " + method.getName() + " " + method.getSignature());
        ExceptionTable et = method.getExceptionTable();
        if (et != null && et.getNumberOfExceptions() > 0) {
        	out.println("\t// "+et.toString());
        }
        
        if (method.isNative()) return;
        if (method.isAbstract()) return;  // does this occur?

		out.print(convertType(method.getReturnType()));
		out.print(" ");
		out.print(convertMethodName(convClassName+"_", method));
		out.print("(");

        for (Attribute attr : method.getAttributes()) {
        	if (attr instanceof Code) {
        		try {
					visitCode(method, (Code)attr);
				} catch (AnalyzerException ex) {
					System.err.println("Analysis failed on "+method.getName()+": "+ex.getMessage());
				}
        	} else if (attr instanceof ExceptionTable) {
        		//TODO visitExceptionTable((ExceptionTable)attr);
        	} else {
        		out.println("\t//     attribute "+attr);
        	}
        }
		
        out.println("}");
	}
	
	HashMap<InstructionHandle, String> label = new HashMap<InstructionHandle, String>();
	private String[] localVars;
	int stackSize;
/* OBSOLETE
	private IdentityHashMap<InstructionHandle,Integer> branchStackSize = new IdentityHashMap<InstructionHandle,Integer>();
*/
	StackAnalyzer analyzer;

	public void visitCode(Method method, Code code) throws AnalyzerException {
        label.clear();
        int maxLocals = code.getMaxLocals();
		localVars = new String[maxLocals];
        int maxStack = code.getMaxStack();
        stackSize = 0;
         
        MethodGen mg = new MethodGen(method, className, constPool);
        InstructionList il = mg.getInstructionList();
        InstructionHandle[] ihs = il.getInstructionHandles();

        String[] argNames = mg.getArgumentNames();
        int localVarStart = argNames.length;
        if (!method.isStatic()) localVarStart += 1;
        for (int i = 0;  i < argNames.length; i++) {
        	//out.println("\t\t// argument " + argNames[i] + ": " + mg.getArgumentType(i).getSignature());
        	Type argType = mg.getArgumentType(i);
			if (argType == Type.LONG || argType == Type.DOUBLE) localVarStart += 1;
        }
       
        // Make labels for all referenced instructions
        prepareBranchTargets(ihs);

        LocalVariableGen[] lvs = mg.getLocalVariables();
        String argSep = "", argEnd = ") {";  
        for (LocalVariableGen lv : lvs) {
            InstructionHandle ih = lv.getStart();
        	label.put(ih, makeLabel(ih));
            ih = lv.getEnd();
        	label.put(ih, makeLabel(ih));
            //out.println("\t\t// var " + lv.getIndex() + " " + lv + ": " +
        	//        lv.getType().getSignature() +
        	//        " from " + label.get(lv.getStart()) +
        	//        " to " + label.get(lv.getEnd()));
        	Type lvType = lv.getType();
			String lVar = lv.getName();
			int lvindex = lv.getIndex();
			localVars[lvindex] = lVar;
			if (lvindex >= localVarStart) {
				if (lvindex == localVarStart) {
					out.println(argEnd);  argEnd = null;
				}
				emit(convertType(lvType)+" "+lVar+";");
			} else {
				out.print(argSep);  argSep = ", ";
				out.print(convertType(lvType)+" "+lVar);
			}
			//TODO does a long local count index up two words?
        }
        if (argEnd != null) out.println(argEnd);  // for static no arguments
        // create any anonymous locals
        for (int i = 0;  i < localVars.length;  i++) {
        	if (localVars[i] == null) {
        		// anonymous location
        		//TODO
            }
        }
        out.print("\t// locals " + code.getMaxLocals());
        out.print(" arguments " + localVarStart);
        out.println(" stack " + code.getMaxStack());
    	out.print(T_STACK+" "+STACK_SWAP);
    	for (int s = 0;  s < maxStack;  ++s) {
    		out.print(", "+STACK+s);
    	}
    	out.println(";");
    	
        analyzer.analyze(mg);

        CodeExceptionGen[] ehs = mg.getExceptionHandlers();
        prepareExceptionTargets(ehs);

        HashMap<InstructionHandle, String> sourceLine = new HashMap<InstructionHandle, String>();

        LineNumberGen[] lns = mg.getLineNumbers();
        for (LineNumberGen ln : lns) {
            InstructionHandle ih = ln.getInstruction();
            sourceLine.put(ih, "line " + ln.getSourceLine());
        }

        for (LocalVariableGen lv : lvs) {
            out.println("\t// var " + lv.getIndex() + " " + lv.getName() + ": " +
                    lv.getType().getSignature() +
                    " from " + label.get(lv.getStart()) +
                    " to " + label.get(lv.getEnd()));
        }
        //TODO how to determine which local variable to reference for instruction range

/* OBSOLETE
        int prevStackSize = 0;
*/
        for (InstructionHandle ih : ihs) {
            Instruction inst = ih.getInstruction();
            
            String lab = label.get(ih);
            if (lab != null) {
        		out.print(lab);
        		out.print(":");
        		stackSize = analyzer.stackSize[ih.getPosition()];
        		assert stackSize >= 0;  // ensure analyzer has done this position
        		out.println("\t// stack size "+stackSize);
            }

/* OBSOLETE       
            if (branchStackSize.containsKey(ih)) {
            	Integer bss = branchStackSize.get(ih);
            	if (bss == null) {
            		// either fall in or backward branch target
            		if (prevStackSize < 0) {
            			out.println("**** don't know stack size for backward branch");
            		} else {
            			branchStackSize.put(ih, new Integer(prevStackSize));
                		out.println("\t// stack size "+stackSize);
            		}
            	} else {
            		if (prevStackSize < 0) {
            			stackSize = bss.intValue();  // size from branch
                		out.println("\t// stack size "+stackSize);
            		} else if (bss.intValue() == prevStackSize) {
            			// expected case
                		out.println("\t// stack size "+stackSize);
            		} else {
            			out.println("**** stack size fall in "+prevStackSize+" branch "+bss.intValue());
            		}
            	}
            }
*/
            
            String sl = sourceLine.get(ih);
            if (sl != null) {
        		out.print("\t// ");
        		out.println(sl);
            }

        	out.print("\t// ");
        	out.println(inst.toString(/*constPool.getConstantPool()*/));
        	
        	inst.accept(this);
        	
/* OBSOLETE       	
        	if (inst instanceof GotoInstruction
        		|| inst instanceof ReturnInstruction		// TODO maybe also Select
        		|| inst instanceof ATHROW
        		|| inst instanceof RET) {
        		prevStackSize = -1;  // unknown size after unconditional transfer
        	} else {
        		prevStackSize = stackSize;
        	}
        	if (inst instanceof BranchInstruction) {
        		out.println("\t// stack size "+stackSize);
        		BranchInstruction branch = (BranchInstruction)inst;
        		InstructionHandle target = branch.getTarget();
            	Integer tss = branchStackSize.get(target);
            	if (tss == null) {
            		branchStackSize.put(target, new Integer(stackSize));
            	} else if (tss != stackSize) {
        			out.println("**** target "+makeLabel(target)+" stack size previously "+tss.intValue()+" here "+stackSize);
            	}
        	} else if (inst instanceof Select) {
        		Select select = (Select)inst;
        		// TODO check select targets
            }
*/

        }
/* OBSOLETE
        if (stackSize > 0) {
        	out.println("**** stack size "+stackSize+" at end of method");
        }
*/

        for (CodeExceptionGen c : ehs) {
            ObjectType caught = c.getCatchType();
            String class_name = (caught == null) ?  // catch any exception, used when compiling finally
                    "all" : caught.getClassName().replace('.', '/');

            out.println("\t// catch " + class_name + " from " +
                    label.get(c.getStartPC()) + " to " + label.get(c.getEndPC()) +
                    " using " + label.get(c.getHandlerPC()));
        }

	}

	private void prepareBranchTargets(InstructionHandle[] ihs) {
		for (InstructionHandle ih : ihs) {
            if (ih instanceof BranchHandle) {
                BranchInstruction bi = (BranchInstruction) ih.getInstruction();

                if (bi instanceof Select) { // Special cases LOOKUPSWITCH and TABLESWITCH
                    for (InstructionHandle target : ((Select) bi).getTargets()) {
                    	label.put(target, makeLabel(target));
/* OBSOLETE
                    	branchStackSize.put(target,null);
*/
                    }
                }

                InstructionHandle targetih = bi.getTarget();
            	label.put(targetih, makeLabel(targetih));
/* OBSOLETE
            	branchStackSize.put(targetih,null);
*/
            }
        }
	}

	private void prepareExceptionTargets(CodeExceptionGen[] ehs) {
/* OBSOLETE
		Integer excpStackSize = new Integer(1);
*/
		for (CodeExceptionGen c : ehs) {
            InstructionHandle ih = c.getStartPC();
        	label.put(ih, makeLabel(ih));
            ih = c.getEndPC();
        	label.put(ih, makeLabel(ih));
            ih = c.getHandlerPC();
        	label.put(ih, makeLabel(ih));
/* OBSOLETE
        	branchStackSize.put(ih,excpStackSize);
*/
        }
	}

	private String makeLabel(InstructionHandle ih) {
		return "L"+ih.getPosition();
	}

	private String getLabel(InstructionHandle instructionHandle) {
        String lab = label.get(instructionHandle);
        assert lab != null;
        return lab;
	}

	private void emit(String s) {
		out.println(s);
	}
	
	private void push(char type, String val) {
		int s = stackSize++;
		assert !(type == STACK_LONG || type == STACK_DOUBLE);
		emit(STACK+s+"."+type+" = "+val+";");
	}
	
	private void pushWide(char type, String val) {
		int s = stackSize++;
		assert (type == STACK_LONG || type == STACK_DOUBLE);
		stackSize++;
		emit(STACK+s+"."+type+" = "+val+";");
	}
	
	private void pushAny(String val, boolean wide) {
		int s = stackSize++;
		if (wide) stackSize++;
		emit(STACK+s+" = "+val+";");
	}

	private String pop(char type) {
		int s = --stackSize;
		assert !(type == STACK_LONG || type == STACK_DOUBLE);
		return STACK+s+"."+type;
	}

	private String popWide(char type) {
		int s = --stackSize;
		assert (type == STACK_LONG || type == STACK_DOUBLE);
		s = --stackSize;
		return STACK+s+"."+type;
	}

	private String popAny(boolean wide) {
		int s = --stackSize;
		if (wide) s = --stackSize;
		return STACK+s;
	}

	private String top(int d) {
		return STACK+(stackSize-d);
	}

	@Override
	public void visitBIPUSH(BIPUSH ins) {
		push(STACK_INT,Integer.toString(ins.getValue().intValue()));
	}

	@Override
	public void visitSIPUSH(SIPUSH ins) {
		push(STACK_INT,Integer.toString(ins.getValue().intValue()));
	}

	@Override
    public void visitACONST_NULL(ACONST_NULL ins) {
		push(STACK_REFER,"null");
    }
    @Override
    public void visitDCONST(DCONST ins) {
		pushWide(STACK_DOUBLE,Double.toString(ins.getValue().doubleValue()));
    }
    @Override
    public void visitFCONST(FCONST ins) {
		push(STACK_FLOAT,Float.toString(ins.getValue().floatValue())+"F");
    }
    @Override
    public void visitICONST(ICONST ins) {
		push(STACK_INT,Integer.toString(ins.getValue().intValue()));
    }
    @Override
    public void visitLCONST(LCONST ins) {
    	pushWide(STACK_LONG,Long.toString(ins.getValue().longValue()));
    }

	@Override
	public void visitLDC(LDC ins) {
		Type type = ins.getType(constPool);
		if (type == Type.INT) {
			push(STACK_INT,Integer.toString(((Integer)ins.getValue(constPool)).intValue()));
		} else if (type == Type.FLOAT) {
			push(STACK_FLOAT,Float.toString(((Float)ins.getValue(constPool)).floatValue())+"F");
		} else if (type == Type.STRING) {
	        Constant con = constPool.getConstantPool().getConstant(ins.getIndex());
	        String scn = stringConstants.get(con);
			push(STACK_REFER,scn+"."+STRING_CONSTANT_STRING+" ? "+scn+"."+STRING_CONSTANT_STRING+" : "+LIB_INIT_STRING_CONST+"(&"+scn+")");
		} else if (type == Type.CLASS) {
			//TODO 
			push(STACK_REFER,"*TODO*Class:"+ins.getValue(constPool).toString());
		} else {
			push(STACK_REFER,"bad LDC type "+type.toString());
		}
	}

	@Override
	public void visitLDC2_W(LDC2_W ins) {
		Type type = ins.getType(constPool);
		if (type == Type.DOUBLE) {
			pushWide(STACK_DOUBLE,Double.toString(ins.getValue(constPool).doubleValue()));
		} else if (type == Type.LONG) {
			pushWide(STACK_LONG,Long.toString(ins.getValue(constPool).longValue()));
		} else {
			push(STACK_REFER,"bad LDC2_W type "+type.toString());
		}
	}

    @Override
    public void visitDADD(DADD ins) {
		dyadicWideOperation(ins,"+",STACK_DOUBLE);
    }
    @Override
    public void visitFADD(FADD ins) {
		dyadicOperation(ins,"+",STACK_FLOAT);
    }
    @Override
    public void visitIADD(IADD ins) {
		dyadicOperation(ins,"+",STACK_INT);
    }
    @Override
    public void visitLADD(LADD ins) {
		dyadicWideOperation(ins,"+",STACK_LONG);
    }

    private void dyadicOperation(ArithmeticInstruction ins, String op, char type) {
		String two = pop(type);
		String one = pop(type);
		push(type,one+" "+op+" "+two);
	}

    private void dyadicWideOperation(ArithmeticInstruction ins, String op, char type) {
		String two = popWide(type);
		String one = popWide(type);
		pushWide(type,one+" "+op+" "+two);
	}

    private void division(ArithmeticInstruction ins, String op, char type, String zero) {
		String two = pop(type);
		checkDivision(two,zero);
		String one = pop(type);
		push(type,one+" "+op+" "+two);
	}

    private void wideDivision(ArithmeticInstruction ins, String op, char type, String zero) {
		String two = popWide(type);
		checkDivision(two,zero);
		String one = popWide(type);
		pushWide(type,one+" "+op+" "+two);
	}

	private void checkDivision(String two, String zero) {
		emit("if ("+two+" == "+zero+") "+LIB_THROW_DIVISION_BY_ZERO+"();");
	}

    @Override
    public void visitDSUB(DSUB ins) {
		dyadicWideOperation(ins,"-",STACK_DOUBLE);
    }
    @Override
    public void visitFSUB(FSUB ins) {
		dyadicOperation(ins,"-",STACK_FLOAT);
    }
    @Override
    public void visitISUB(ISUB ins) {
		dyadicOperation(ins,"-",STACK_INT);
    }
    @Override
    public void visitLSUB(LSUB ins) {
		dyadicWideOperation(ins,"-",STACK_LONG);
    }

    @Override
    public void visitDMUL(DMUL ins) {
		dyadicWideOperation(ins,"*",STACK_DOUBLE);
    }
    @Override
    public void visitFMUL(FMUL ins) {
		dyadicOperation(ins,"*",STACK_FLOAT);
    }
    @Override
    public void visitIMUL(IMUL ins) {
		dyadicOperation(ins,"*",STACK_INT);
    }
    @Override
    public void visitLMUL(LMUL ins) {
		dyadicWideOperation(ins,"*",STACK_LONG);
    }

    @Override
	public void visitDDIV(DDIV ins) {
		wideDivision(ins,"/",STACK_DOUBLE, "0.0");
	}

	@Override
	public void visitFDIV(FDIV ins) {
		wideDivision(ins,"/",STACK_FLOAT, "0.0F");
	}

	@Override
	public void visitIDIV(IDIV ins) {
		String two = pop(STACK_INT);
		// REDUNDANT checkDivision(two);
		String one = pop(STACK_INT);
		push(STACK_INT,LIB_IDIV+"("+one+","+two+")");
	}

	@Override
	public void visitLDIV(LDIV ins) {
		String two = pop(STACK_LONG);
		// REDUNDANT checkDivision(two);
		String one = pop(STACK_LONG);
		push(STACK_LONG,LIB_LDIV+"("+one+","+two+")");
	}

	@Override
	public void visitDREM(DREM ins) {
		wideDivision(ins,"/",STACK_DOUBLE, "0.0");
	}

	@Override
	public void visitFREM(FREM ins) {
		wideDivision(ins,"%",STACK_DOUBLE, "0.0F");
	}

	@Override
	public void visitIREM(IREM ins) {
		String two = pop(STACK_INT);
		// REDUNDANT checkDivision(two);
		String one = pop(STACK_INT);
		push(STACK_INT,LIB_IREM+"("+one+","+two+")");
	}

	@Override
	public void visitLREM(LREM ins) {
		String two = pop(STACK_LONG);
		// REDUNDANT checkDivision(two);
		String one = pop(STACK_LONG);
		push(STACK_LONG,LIB_LREM+"("+one+","+two+")");
	}

	@Override
	public void visitDNEG(DNEG ins) {
		String one = popWide(STACK_DOUBLE);
		pushWide(STACK_DOUBLE,"0.0 - "+one);
	}

	@Override
	public void visitFNEG(FNEG ins) {
		String one = pop(STACK_FLOAT);
		push(STACK_FLOAT,"0.0 - "+one);
	}

	@Override
	public void visitINEG(INEG ins) {
		String one = pop(STACK_INT);
		push(STACK_INT,"0 - "+one);
	}

	@Override
	public void visitLNEG(LNEG ins) {
		String one = popWide(STACK_LONG);
		pushWide(STACK_LONG,"0 - "+one);
	}
    
    @Override
	public void visitIAND(IAND ins) {
		dyadicOperation(ins,"&",STACK_INT);
	}

	@Override
	public void visitIOR(IOR ins) {
		dyadicOperation(ins,"|",STACK_INT);
	}

	@Override
	public void visitISHL(ISHL ins) {
		shiftOperation("<<", STACK_INT, "", 31);
	}
	
	private void shiftOperation(String op, char type, String cast, int max) {
		String two = pop(STACK_INT);
		String one = pop(type);
		push(type,cast+one+" "+op+" ("+two+" & "+max+")");
	}
	
	private void shiftWideOperation(String op, char type, String cast, int max) {
		String two = pop(STACK_INT);
		String one = popWide(type);
		pushWide(type,cast+one+" "+op+" ("+two+" & "+max+")");
	}

	@Override
	public void visitISHR(ISHR ins) {
		shiftOperation(">>", STACK_INT, "", 31);
	}

	@Override
	public void visitIUSHR(IUSHR ins) {
		shiftOperation(">>", STACK_INT, "(unsigned "+convertType(Type.INT)+")", 31);
	}

	@Override
	public void visitIXOR(IXOR ins) {
		dyadicOperation(ins,"^",STACK_INT);
	}

	@Override
	public void visitLAND(LAND ins) {
		dyadicWideOperation(ins,"&",STACK_LONG);
	}

	@Override
	public void visitLCMP(LCMP ins) {
		String two = popWide(STACK_LONG);
		String one = popWide(STACK_LONG);
		comparisonOperation(one, two, false);
	}

	private void comparisonOperation(String one, String two, boolean lt) {
	    // (From toba-1.1c) these are carefully crafted to make NaN cases come out right
	    if (lt)			// if want -1 for NaN
	    	push(STACK_INT, "(" +
							one + " > " + two + ") ? 1 : ((" +
							one + " == " + two + ") ? 0 : -1)");
	    else
	    	push(STACK_INT, "(" +
							one + " < " + two + ") ? -1 : ((" +
							one + " == " + two + ") ? 0 : 1)");
	}

	@Override
	public void visitDCMPG(DCMPG ins) {
		String two = popWide(STACK_DOUBLE);
		String one = popWide(STACK_DOUBLE);
		comparisonOperation(one, two, false);
	}

	@Override
	public void visitDCMPL(DCMPL ins) {
		String two = popWide(STACK_DOUBLE);
		String one = popWide(STACK_DOUBLE);
		comparisonOperation(one, two, true);
	}

	@Override
	public void visitFCMPG(FCMPG ins) {
		String two = pop(STACK_FLOAT);
		String one = pop(STACK_FLOAT);
		comparisonOperation(one, two, false);
	}

	@Override
	public void visitFCMPL(FCMPL ins) {
		String two = pop(STACK_FLOAT);
		String one = pop(STACK_FLOAT);
		comparisonOperation(one, two, true);
	}

	@Override
	public void visitLOR(LOR ins) {
		dyadicWideOperation(ins,"|",STACK_LONG);
	}

	@Override
	public void visitLSHL(LSHL ins) {
		shiftWideOperation("<<", STACK_LONG, "", 63);
	}

	@Override
	public void visitLSHR(LSHR ins) {
		shiftWideOperation(">>", STACK_LONG, "", 63);
	}

	@Override
	public void visitLUSHR(LUSHR ins) {
		shiftWideOperation(">>", STACK_LONG, "(unsigned "+convertType(Type.LONG)+")", 63);
	}

	@Override
	public void visitLXOR(LXOR ins) {
		dyadicWideOperation(ins,"^",STACK_LONG);
	}

	@Override
	public void visitALOAD(ALOAD ins) {
		push(STACK_REFER,localVars[ins.getIndex()]);
	}

	@Override
	public void visitDLOAD(DLOAD ins) {
		pushWide(STACK_DOUBLE,localVars[ins.getIndex()]);
	}

	@Override
	public void visitFLOAD(FLOAD ins) {
		push(STACK_FLOAT,localVars[ins.getIndex()]);
	}

	@Override
	public void visitILOAD(ILOAD ins) {
		push(STACK_INT,localVars[ins.getIndex()]);
	}

	@Override
	public void visitLLOAD(LLOAD ins) {
		pushWide(STACK_LONG,localVars[ins.getIndex()]);
	}

	@Override
	public void visitASTORE(ASTORE ins) {
		storeLocal(STACK_REFER,localVars[ins.getIndex()]);
	}

	private void storeLocal(char type, String lvar) {
		String val = pop(type);
		emit(lvar+" = "+val+";");
	}

	private void storeWideLocal(char type, String lvar) {
		String val = popWide(type);
		emit(lvar+" = "+val+";");
	}

	@Override
	public void visitDSTORE(DSTORE ins) {
		storeWideLocal(STACK_DOUBLE,localVars[ins.getIndex()]);
	}

	@Override
	public void visitFSTORE(FSTORE ins) {
		storeLocal(STACK_FLOAT,localVars[ins.getIndex()]);
	}

	@Override
	public void visitISTORE(ISTORE ins) {
		storeLocal(STACK_INT,localVars[ins.getIndex()]);
	}

	@Override
	public void visitLSTORE(LSTORE ins) {
		storeWideLocal(STACK_LONG,localVars[ins.getIndex()]);
	}

	@Override
	public void visitIINC(IINC ins) {
		String lvar = localVars[ins.getIndex()];
		int val = ins.getIncrement();
		emit(lvar+" += "+Integer.toString(val)+";");
	}

	@Override
	public void visitAALOAD(AALOAD ins) {
		arrayLoad(STACK_REFER,Type.OBJECT);
	}

	private void arrayLoad(char type, Type elType) {
		String ind = pop(STACK_INT);
		String ref = pop(STACK_REFER);
		checkIndex(ref,ind);
		push(type,"(("+T_ARRAY_+convertType(elType)+")"+ref+").E["+ind+"]");
	}

	private void arrayWideLoad(char type, Type elType) {
		String ind = pop(STACK_INT);
		String ref = pop(STACK_REFER);
		checkIndex(ref,ind);
		pushWide(type,"(("+T_ARRAY_+convertType(elType)+")"+ref+").E["+ind+"]");
	}

	private void checkIndex(String ref, String ind) {
		checkRefer(ref);
		emit("if ("+ind+" < 0 || "+ind+" >= (("+T_ARRAY_HEAD+")"+ref+")."+ARRAY_LENGTH+") "+LIB_THROW_ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION+"("+ind+");");
	}
	
	@Override
	public void visitBALOAD(BALOAD ins) {
		arrayLoad(STACK_INT,Type.BYTE);
	}

	@Override
	public void visitCALOAD(CALOAD ins) {
		arrayLoad(STACK_INT,Type.CHAR);
	}

	@Override
	public void visitDALOAD(DALOAD ins) {
		arrayWideLoad(STACK_DOUBLE,Type.DOUBLE);
	}

	@Override
	public void visitFALOAD(FALOAD ins) {
		arrayLoad(STACK_FLOAT,Type.FLOAT);
	}

	@Override
	public void visitIALOAD(IALOAD ins) {
		arrayLoad(STACK_INT,Type.INT);
	}

	@Override
	public void visitLALOAD(LALOAD ins) {
		arrayWideLoad(STACK_LONG,Type.LONG);
	}

	@Override
	public void visitSALOAD(SALOAD ins) {
		arrayLoad(STACK_INT,Type.SHORT);
	}

	private void arrayStore(char type, Type elType) {
		String val = pop(type);
		String ind = pop(STACK_INT);
		String ref = pop(STACK_REFER);
		checkIndex(ref,ind);
		emit("(("+T_ARRAY_+convertType(elType)+")"+ref+")."+ARRAY_ELEMENTS+"["+ind+"] = "+val+";");
	}

	private void arrayWideStore(char type, Type elType) {
		String val = popWide(type);
		String ind = pop(STACK_INT);
		String ref = pop(STACK_REFER);
		checkIndex(ref,ind);
		emit("(("+T_ARRAY_+convertType(elType)+")"+ref+")."+ARRAY_ELEMENTS+"["+ind+"] = "+val+";");
	}

	@Override
	public void visitAASTORE(AASTORE ins) {
		arrayStore(STACK_REFER,Type.OBJECT);
	}

	@Override
	public void visitBASTORE(BASTORE ins) {
		arrayStore(STACK_INT,Type.BYTE);
	}

	@Override
	public void visitCASTORE(CASTORE ins) {
		arrayStore(STACK_INT,Type.CHAR);
	}

	@Override
	public void visitDASTORE(DASTORE ins) {
		arrayWideStore(STACK_DOUBLE,Type.DOUBLE);
	}

	@Override
	public void visitFASTORE(FASTORE ins) {
		arrayStore(STACK_FLOAT,Type.FLOAT);
	}

	@Override
	public void visitIASTORE(IASTORE ins) {
		arrayStore(STACK_INT,Type.INT);
	}

	@Override
	public void visitLASTORE(LASTORE ins) {
		arrayWideStore(STACK_LONG,Type.LONG);
	}

	@Override
	public void visitSASTORE(SASTORE ins) {
		arrayStore(STACK_INT,Type.SHORT);
	}

	@Override
    public void visitGETFIELD(GETFIELD ins) {
    	ObjectType refType = (ObjectType)ins.getReferenceType(constPool);
		String fieldName = ins.getFieldName(constPool);
		Type fieldType = ins.getFieldType(constPool);
    	String ref = pop(STACK_REFER);
		checkRefer(ref);
		String field = convertField(refType, fieldName, ref);
		pushType(fieldType, field);
    }

	private String convertField(ObjectType refType, String fieldName, String ref) {
		return "(("+convertClassName(refType)+")"+ref+")->"+escapeName(fieldName);
	}

	private void pushType(Type fieldType, String field) {
		if (fieldType == Type.BOOLEAN) {
			push(STACK_INT,field);
		} else if (fieldType == Type.BYTE) {
			push(STACK_INT,field);
		} else if (fieldType == Type.CHAR) {
			push(STACK_INT,field);
		} else if (fieldType == Type.DOUBLE) {
			pushWide(STACK_DOUBLE,field);
		} else if (fieldType == Type.FLOAT) {
			push(STACK_FLOAT,field);
		} else if (fieldType == Type.INT) {
			push(STACK_INT,field);
		} else if (fieldType == Type.LONG) {
			pushWide(STACK_LONG,field);
		} else if (fieldType == Type.SHORT) {
			push(STACK_INT,field);
		} else if (fieldType instanceof ReferenceType) { // Array, Object, or undefined
			push(STACK_REFER,field);
		} else {
			throw new RuntimeException("Unexpected pushType class "+fieldType.getClass());
		}
	}
	
	private void checkRefer(String ref) {
		emit("if ("+ref+" == null) "+LIB_THROW_NULL_POINTER_EXCEPTION+"();");
	}

	@Override
	public void visitPUTFIELD(PUTFIELD ins) {
		ObjectType refType = (ObjectType)ins.getReferenceType(constPool);
		String fieldName = ins.getFieldName(constPool);
		Type fieldType = ins.getFieldType(constPool);
    	String val = popType(fieldType);
		String ref = pop(STACK_REFER);
		checkRefer(ref);
		String field = convertField(refType, fieldName, ref);
		emit(field+" = "+val+";");
	}

	private String popType(Type fieldType) {
		if (fieldType == Type.BOOLEAN) {
			return pop(STACK_INT);
		} else if (fieldType == Type.BYTE) {
			return pop(STACK_INT);
		} else if (fieldType == Type.CHAR) {
			return pop(STACK_INT);
		} else if (fieldType == Type.DOUBLE) {
			return popWide(STACK_DOUBLE);
		} else if (fieldType == Type.FLOAT) {
			return pop(STACK_FLOAT);
		} else if (fieldType == Type.INT) {
			return pop(STACK_INT);
		} else if (fieldType == Type.LONG) {
			return popWide(STACK_LONG);
		} else if (fieldType == Type.SHORT) {
			return pop(STACK_INT);
		} else if (fieldType instanceof ReferenceType) { // Array, Object, or undefined
			return pop(STACK_REFER);
		} else {
			throw new RuntimeException("Unexpected popType class "+fieldType.getClass());
		}
	}

	@Override
	public void visitGETSTATIC(GETSTATIC ins) {
		// TODO call class initialization
		ObjectType refType = (ObjectType)ins.getReferenceType(constPool);
		String fieldName = ins.getFieldName(constPool);
		Type fieldType = ins.getFieldType(constPool);
		String field = convertStatic(refType, fieldName);
		pushType(fieldType, field);
	}

	private String convertStatic(ObjectType refType, String fieldName) {
		return CLASS_STRUCT_PREFIX+convertClassName(refType)+"."+CLASS_STATIC_FIELDS+"."+fieldName;
	}

	@Override
	public void visitPUTSTATIC(PUTSTATIC ins) {
		// TODO call class initialization
		// TODO assignment conversion?
		ObjectType refType = (ObjectType)ins.getReferenceType(constPool);
		String fieldName = ins.getFieldName(constPool);
		Type fieldType = ins.getFieldType(constPool);
    	String val = popType(fieldType);
		String field = convertStatic(refType, fieldName);
		emit(field+" = "+val+";");
	}

	@Override
	public void visitDUP(DUP ins) {
		pushAny(top(1), false);
	}

	@Override
	public void visitDUP2(DUP2 ins) {
		pushAny(top(2), true);
	}

	@Override
	public void visitDUP_X1(DUP_X1 ins) {
		String one = top(1);
		String two = top(2);
		pushAny(one,false);
		emit(one+" = "+two+";");
		emit(two+" = "+top(1)+";");
	}

	@Override
	public void visitDUP_X2(DUP_X2 ins) {
		String one = top(1);
		String two = top(2);
		String three = top(3);
		pushAny(one,false);
		emit(one+" = "+two+";");
		emit(two+" = "+three+";");
		emit(three+" = "+top(1)+";");
		// not sure if this is right for category 2 (DOUBLE or LONG) two would be an empty slot
	}

	@Override
	public void visitDUP2_X1(DUP2_X1 ins) {
		String one = top(1);
		String two = top(2);
		String three = top(3);
		pushAny(two,false);
		pushAny(one,false);
		emit(one+" = "+three+";");
		emit(two+" = "+top(1)+";");
		emit(three+" = "+top(2)+";");
		// not sure if this is right for category 2 (DOUBLE or LONG) three would be an empty slot
	}

	@Override
	public void visitDUP2_X2(DUP2_X2 ins) {
		String one = top(1);
		String two = top(2);
		String three = top(3);
		String four = top(4);
		pushAny(two,false);
		pushAny(one,false);
		emit(one+" = "+three+";");
		emit(two+" = "+four+";");
		emit(three+" = "+top(1)+";");
		emit(four+" = "+top(2)+";");
		// not sure if this is right for category 2 (DOUBLE or LONG) one and three would be empty slots
	}

	@Override
	public void visitSWAP(SWAP ins) {
		String one = top(1);
		String two = top(2);
		emit(STACK_SWAP+" = "+two+";");
		emit(two+" = "+one+";");
		emit(one+" = "+STACK_SWAP+";");
	}

	@Override
	public void visitPOP(POP ins) {
		@SuppressWarnings("unused")
		String discard = popAny(false);
	}

	@Override
	public void visitPOP2(POP2 ins) {
		@SuppressWarnings("unused")
		String discard = popAny(true);
	}

	@Override
	public void visitGOTO(GOTO ins) {
		emitGoto(ins.getTarget());
	}

	private void emitGoto(InstructionHandle target) {
		emit("goto "+getLabel(target)+";");
	}

	@Override
	public void visitGOTO_W(GOTO_W ins) {
		emitGoto(ins.getTarget());
	}

	@Override
	public void visitIFEQ(IFEQ ins) {
		String one = pop(STACK_INT);
		branch(one,"==","0",ins.getTarget());
	}

	private void branch(String one, String op, String two, InstructionHandle target) {
		emit("if ("+one+" "+op+" "+two+") goto "+getLabel(target)+";");
	}

	@Override
	public void visitIFGE(IFGE ins) {
		String one = pop(STACK_INT);
		branch(one,">=","0",ins.getTarget());
	}

	@Override
	public void visitIFGT(IFGT ins) {
		String one = pop(STACK_INT);
		branch(one,">","0",ins.getTarget());
	}

	@Override
	public void visitIFLE(IFLE ins) {
		String one = pop(STACK_INT);
		branch(one,"<=","0",ins.getTarget());
	}

	@Override
	public void visitIFLT(IFLT ins) {
		String one = pop(STACK_INT);
		branch(one,"<","0",ins.getTarget());
	}

	@Override
	public void visitIFNE(IFNE ins) {
		String one = pop(STACK_INT);
		branch(one,"!=","0",ins.getTarget());
	}

	@Override
	public void visitIFNONNULL(IFNONNULL ins) {
		String one = pop(STACK_REFER);
		branch(one,"!=","null",ins.getTarget());
	}

	@Override
	public void visitIFNULL(IFNULL ins) {
		String one = pop(STACK_REFER);
		branch(one,"==","0",ins.getTarget());
	}

	@Override
	public void visitIF_ACMPEQ(IF_ACMPEQ ins) {
		String two = pop(STACK_REFER);
		String one = pop(STACK_REFER);
		branch(one,"==",two,ins.getTarget());
	}

	@Override
	public void visitIF_ACMPNE(IF_ACMPNE ins) {
		String two = pop(STACK_REFER);
		String one = pop(STACK_REFER);
		branch(one,"!=",two,ins.getTarget());
	}

	@Override
	public void visitIF_ICMPEQ(IF_ICMPEQ ins) {
		String two = pop(STACK_INT);
		String one = pop(STACK_INT);
		branch(one,"==",two,ins.getTarget());
	}

	@Override
	public void visitIF_ICMPGE(IF_ICMPGE ins) {
		String two = pop(STACK_INT);
		String one = pop(STACK_INT);
		branch(one,">=",two,ins.getTarget());
	}

	@Override
	public void visitIF_ICMPGT(IF_ICMPGT ins) {
		String two = pop(STACK_INT);
		String one = pop(STACK_INT);
		branch(one,">",two,ins.getTarget());
	}

	@Override
	public void visitIF_ICMPLE(IF_ICMPLE ins) {
		String two = pop(STACK_INT);
		String one = pop(STACK_INT);
		branch(one,"<=",two,ins.getTarget());
	}

	@Override
	public void visitIF_ICMPLT(IF_ICMPLT ins) {
		String two = pop(STACK_INT);
		String one = pop(STACK_INT);
		branch(one,"<",two,ins.getTarget());
	}

	@Override
	public void visitIF_ICMPNE(IF_ICMPNE ins) {
		String two = pop(STACK_INT);
		String one = pop(STACK_INT);
		branch(one,"!=",two,ins.getTarget());
	}

	@Override
	public void visitD2F(D2F ins) {
		String one = popWide(STACK_DOUBLE);
		push(STACK_FLOAT,"("+T_FLOAT+")"+one);
	}

	@Override
	public void visitD2I(D2I ins) {
		String one = popWide(STACK_DOUBLE);
		push(STACK_INT,LIB_D2I+"("+one+")");
	}

	@Override
	public void visitD2L(D2L ins) {
		String one = popWide(STACK_DOUBLE);
		pushWide(STACK_LONG,LIB_D2L+"("+one+")");
	}

	@Override
	public void visitF2D(F2D ins) {
		String one = pop(STACK_FLOAT);
		pushWide(STACK_DOUBLE,"("+T_DOUBLE+")"+one);
	}

	@Override
	public void visitF2I(F2I ins) {
		String one = pop(STACK_FLOAT);
		push(STACK_INT,LIB_F2I+"("+one+")");
	}

	@Override
	public void visitF2L(F2L ins) {
		String one = pop(STACK_FLOAT);
		pushWide(STACK_LONG,LIB_F2L+"("+one+")");
	}

	@Override
	public void visitI2B(I2B ins) {
		intTruncation(T_BYTE);
	}

	private void intTruncation(String type) {
		String one = pop(STACK_INT);
		push(STACK_INT,"("+type+")"+one);
	}

	@Override
	public void visitI2C(I2C ins) {
		intTruncation(T_CHAR);
	}

	@Override
	public void visitI2D(I2D ins) {
		String one = pop(STACK_INT);
		pushWide(STACK_DOUBLE,"("+T_DOUBLE+")"+one);
	}

	@Override
	public void visitI2F(I2F ins) {
		String one = pop(STACK_INT);
		push(STACK_FLOAT,"("+T_FLOAT+")"+one);
	}

	@Override
	public void visitI2L(I2L ins) {
		String one = pop(STACK_INT);
		pushWide(STACK_LONG,one);
	}

	@Override
	public void visitI2S(I2S ins) {
		intTruncation(T_SHORT);
	}

	@Override
	public void visitL2D(L2D ins) {
		String one = popWide(STACK_LONG);
		pushWide(STACK_DOUBLE,"("+T_DOUBLE+")"+one);
	}

	@Override
	public void visitL2F(L2F ins) {
		String one = popWide(STACK_LONG);
		push(STACK_FLOAT,"("+T_FLOAT+")"+one);
	}

	@Override
	public void visitL2I(L2I ins) {
		String one = popWide(STACK_LONG);
		push(STACK_INT,"("+T_INT+")"+one);
	}

	@Override
	public void visitNEW(NEW ins) {
		ObjectType type = (ObjectType)ins.getType(constPool);
		push(STACK_REFER,LIB_NEW+"("+classPointer(type)+")");
	}

	@Override
	public void visitNEWARRAY(NEWARRAY ins) {
		ArrayType type = (ArrayType)ins.getType();
		BasicType eType = (BasicType)type.getElementType();
		String count = pop(STACK_INT);
		String typ;
		if (eType == Type.BOOLEAN) typ = "Boolean";
		else if (eType == Type.BYTE) typ = "Byte";
		else if (eType == Type.CHAR) typ = "Char";
		else if (eType == Type.DOUBLE) typ = "Double";
		else if (eType == Type.FLOAT) typ = "Float";
		else if (eType == Type.INT) typ = "Int";
		else if (eType == Type.LONG) typ = "Long";
		else if (eType == Type.SHORT) typ = "Short";
		else throw new RuntimeException("Unexpected NEWARRAY element type "+eType.getClass());
		push(STACK_REFER,LIB_NEW_ARRAY_+typ+"("+count+")");
	}

	@Override
	public void visitANEWARRAY(ANEWARRAY ins) {
		ArrayType type = (ArrayType)ins.getType(constPool);
		ObjectType eType = (ObjectType)type.getElementType();
		String count = pop(STACK_INT);
		push(STACK_REFER,LIB_NEW_ARRAY_OBJECT+"("+count+", "+classPointer(eType)+")");
	}
	
	private String classPointer(ObjectType eType) {
		return "&"+CLASS_STRUCT_PREFIX+convertClassName(eType.getClassName());
	}

	@Override
	public void visitMULTIANEWARRAY(MULTIANEWARRAY ins) {
		//TODO
	}

	@Override
	public void visitARRAYLENGTH(ARRAYLENGTH ins) {
		String ref = pop(STACK_REFER);
		checkRefer(ref);
		push(STACK_INT,"(("+T_ARRAY_HEAD+")"+ref+")."+ARRAY_LENGTH);
	}

	@Override
	public void visitCHECKCAST(CHECKCAST ins) {
		String ref = top(1)+"."+STACK_REFER;
		// REDUNDANT checkRefer(ref);
		emit(LIB_CHECK_CAST+"("+ref+", "+classPointer(ins.getLoadClassType(constPool))+");");
	}

	@Override
	public void visitINSTANCEOF(INSTANCEOF ins) {
		String ref = pop(STACK_REFER);
		// REDUNDANT checkRefer(ref);
		push(STACK_INT,LIB_INSTANCEOF+"("+ref+", "+classPointer(ins.getLoadClassType(constPool))+")");
	}

	@Override
	public void visitINVOKEDYNAMIC(INVOKEDYNAMIC ins) {
		//TODO
		throw new RuntimeException("unimplemented inststruction "+ins);
	}

	@Override
	public void visitINVOKEINTERFACE(INVOKEINTERFACE ins) {
		String className = ins.getClassName(constPool);
		String methName = ins.getMethodName(constPool);
		String sign = ins.getSignature(constPool);
		String[] args = popArguments(1,sign);
		String ref = pop(STACK_REFER);
		checkRefer(ref);
		args[0] = ref;
		invoke(virtualMethodReference(className,ref,methName,sign), args, sign);
		//TODO not yet looking for interfaces
	}

	@Override
	public void visitINVOKESPECIAL(INVOKESPECIAL ins) {
		String className = ins.getClassName(constPool);
		String methName = ins.getMethodName(constPool);
		String sign = ins.getSignature(constPool);
		String[] args = popArguments(1,sign);
		String ref = pop(STACK_REFER);
		checkRefer(ref);
		args[0] = ref;
		invoke(virtualMethodReference(className,ref,methName,sign), args, sign);
		//TODO this has not yet captured the distinction from INVOKEVIRTUAL https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-6.html#jvms-6.5.invokespecial
	}

	private String virtualMethodReference(String className, String objectRef, String methodName, String signature) {
		String prefix = "(("+convertClassName(className)+")"+objectRef+")->"+OBJECT_CLASS+"->"+CLASS_METHOD_TABLE+".";
		return convertMethodName(prefix,methodName,signature);
	}

	@Override
	public void visitINVOKESTATIC(INVOKESTATIC ins) {
		String className = ins.getClassName(constPool);
		String methName = ins.getMethodName(constPool);
		String sign = ins.getSignature(constPool);
		String[] args = popArguments(0,sign);
		invoke(convertMethodName(convertClassName(className)+"_",methName,sign), args, sign);
	}

	private void invoke(String name, String[] arguments, String signature) {
		Type retType = Type.getReturnType(signature);
		StringBuilder call = new StringBuilder();
		call.append(name).append("(");
		String sep = "";
		for (int i = 0;  i < arguments.length;  ++i) {
			call.append(sep);  sep = ",";
			String argVal = arguments[i];
			call.append(argVal);
		}
		call.append(")");
		if (retType == Type.VOID) {
			emit(call.toString()+";");
		} else {
			pushType(retType,call.toString());
		}
	}

	private String[] popArguments(int reserve, String signature) {
		Type[] argTypes = Type.getArgumentTypes(signature);
		int numArgs = argTypes.length;
		String[] argList = new String[reserve+numArgs];
		for (int i = numArgs-1;  i >= 0;  --i) {
			Type argType = argTypes[i];
			argList[reserve+i] = popType(argType);
		}
		return argList;
	}

	@Override
	public void visitINVOKEVIRTUAL(INVOKEVIRTUAL ins) {
		String className = ins.getClassName(constPool);
		String methName = ins.getMethodName(constPool);
		String sign = ins.getSignature(constPool);
		String[] args = popArguments(1,sign);
		String ref = pop(STACK_REFER);
		checkRefer(ref);
		args[0] = ref;
		invoke(virtualMethodReference(className,ref,methName,sign), args, sign);
	}
	
	@Override
	public void visitARETURN(ARETURN obj) {
		String val = pop(STACK_REFER);
		emit("return "+val+";");
	}

	@Override
	public void visitDRETURN(DRETURN obj) {
		String val = popWide(STACK_DOUBLE);
		emit("return "+val+";");
	}

	@Override
	public void visitFRETURN(FRETURN obj) {
		String val = pop(STACK_FLOAT);
		emit("return "+val+";");
	}

	@Override
	public void visitIRETURN(IRETURN obj) {
		String val = pop(STACK_INT);
		emit("return "+val+";");
	}

	@Override
	public void visitLRETURN(LRETURN obj) {
		String val = popWide(STACK_LONG);
		emit("return "+val+";");
	}

	@Override
	public void visitRETURN(RETURN obj) {
		emit("return;");
	}

	@Override
	public void visitATHROW(ATHROW obj) {
		String val = pop(STACK_REFER);
		emit(LIB_THROW+"("+val+");");
	}

	static int switchCount = 0;

	@Override
	public void visitLOOKUPSWITCH(LOOKUPSWITCH ins) {
		String val = pop(STACK_INT);
		String tableName = "switchTable"+(switchCount++);
		StringBuilder table = new StringBuilder();
		int[] matches = ins.getMatchs();
		InstructionHandle[] targets = ins.getTargets();
		String sep = "";
        for (int i = 0; i < matches.length; i++) {
        	table.append(sep).append("{").append(matches[i]).append(",&&").append(makeLabel(targets[i])).append("}");
        	sep = ",";
        }
		emit("static "+SWITCH_PAIR+" "+tableName+"[] = {"+table+"};");
		String defalt = makeLabel(ins.getTarget());
		emit("goto *_lookupswitch("+val+", "+matches.length+", "+tableName+", &&"+defalt+");");
	}

	@Override
	public void visitTABLESWITCH(TABLESWITCH ins) {
		String val = pop(STACK_INT);
		String tableName = "switchTable"+(switchCount++);
		StringBuilder table = new StringBuilder();
		int[] matches = ins.getMatchs();
		InstructionHandle[] targets = ins.getTargets();
		String sep = "";
        for (int i = 0; i < matches.length; i++) {
        	table.append(sep).append("&&").append(makeLabel(targets[i]));
        	sep = ",";
        }
		emit("static "+LABEL_PTR+" "+tableName+"[] = {"+table+"};");
		String defalt = makeLabel(ins.getTarget());
		int low = matches[0], high = matches[matches.length-1];
		emit("if ("+val+" < "+low+") goto "+defalt+";");
		emit("if ("+val+" > "+high+") goto "+defalt+";");
		emit("goto *"+tableName+"["+val+"-"+low+"];");
	}

	@Override
	public void visitMONITORENTER(MONITORENTER ins) {
		String val = pop(STACK_REFER);
		emit(LIB_MONITOR_ENTER+"("+val+");");
	}

	@Override
	public void visitMONITOREXIT(MONITOREXIT ins) {
		String val = pop(STACK_REFER);
		emit(LIB_MONITOR_EXIT+"("+val+");");
	}

	@Override
	public void visitJSR(JSR ins) {
		throw new RuntimeException("unimplemented inststruction "+ins);
	}

	@Override
	public void visitJSR_W(JSR_W ins) {
		throw new RuntimeException("unimplemented inststruction "+ins);
	}

	@Override
	public void visitRET(RET ins) {
		throw new RuntimeException("unimplemented inststruction "+ins);
	}

	@Override
	public void visitBREAKPOINT(BREAKPOINT ins) {
		throw new RuntimeException("unexpected inststruction "+ins);
	}

	@Override
	public void visitIMPDEP1(IMPDEP1 ins) {
		throw new RuntimeException("unexpected inststruction "+ins);
	}

	@Override
	public void visitIMPDEP2(IMPDEP2 ins) {
		throw new RuntimeException("unexpected inststruction "+ins);
	}

	@Override
	public void visitNOP(NOP ins) {
		// empty
	}
	
}
