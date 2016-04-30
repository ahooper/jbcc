package ca.nevdull.jbcc3;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

public class MethodCompiler extends InstructionAdapter implements CCode {

	private PrintWriter out;

	private InstructionCommenter insnCommenter;
	protected StringConstants stringCons;
	private ClassCache classCache;

	private String[] argIsInterface;
	private String methodIsSynchronized;
	
	MethodCompiler(ClassCache classCache) {
		super(Opcodes.ASM5, null);
		this.classCache = classCache;
		stringCons = new StringConstants();
		insnCommenter = new InstructionCommenter();
	}

	void setOut(PrintWriter out) {
		this.out = out;
		insnCommenter.setOut(out);
		stringCons.setOut(out);
		stringCons.clear();
	}
	
	void compileStrings(MethodNode method) {
		method.accept(stringCons);
	}
	
	void compileClassReferences(MethodNode method, ClassReferences references) {
		method.accept(references);
	}

	void compileCode(ClassNode classNode, MethodNode method) throws AnalyzerException {
		out.println("// "+method.name+" "+method.desc);
        List<String> exceptions = method.exceptions;
        if (exceptions != null && exceptions.size() > 0) {
        	out.print("\t// exceptions");
        	for (String exc : exceptions) {
        		out.print(" ");
        		out.print(exc);
        	}
        	out.println();
        }
        
        if ((method.access & Opcodes.ACC_ABSTRACT) != 0) {
        	out.println("/*ABSTRACT*/");
        	return;
        }
        
		Type[] argTypes = Type.getArgumentTypes(method.desc);
		Type returnType = Type.getReturnType(method.desc);

        if ((method.access & Opcodes.ACC_NATIVE) != 0) {
			out.println("/*NATIVE*/");
        	return;  // all the externs are now generated in the .h so is no longer needed here
			//out.print("/*NATIVE*/ extern ");
        //} else if (   /*(method.access & Opcodes.ACC_STATIC) == 0
        //		   || */(method.access & Opcodes.ACC_PRIVATE) != 0) {
		//	out.print("static ");
        	// inheriting classes need all the method declarations to complete their
			// tables, even PRIVATE methods
		}
        out.print(convertType(returnType));
		out.print(" ");
		out.print(externalMethodName(classNode.name, method));
		out.print("(");
		out.print(ENV_TYPE+" "+ENV_ARG);

		argsEnd = 0;
		int virtual = ((method.access & Opcodes.ACC_STATIC) == 0) ? 1 : 0;
		argIsInterface = new String[method.maxLocals];
		if ((method.access & Opcodes.ACC_STATIC) == 0) {
			checkArgIsInterface(argsEnd,classNode);
        	out.print(", ");
        	out.print(convertClassName(classNode.name)/*this*/);
        	out.print(" ");
        	out.print(FRAME);
        	out.print(argsEnd++);
        }
        for (int i = 0;  i < argTypes.length;  i++) {
			checkArgIsInterface(argsEnd,argTypes[i]);
        	out.print(", ");
        	out.print(convertType(argTypes[i]));
        	out.print(" ");
        	out.print(FRAME);
        	out.print(argsEnd++);
            if (argTypes[i].getSize() == 2) argsEnd++;
        }
        if ((method.access & Opcodes.ACC_NATIVE) != 0) {
    		out.println(");");
    		return;
        }
		out.println(") {");
        for (int i = 0;  i < argIsInterface.length;  i++) {
	    	if (argIsInterface[i] != null) {
	    		out.println(argIsInterface[i]);
	    	}
	    }
		
		int m = method.maxLocals + method.maxStack;
		if (m > argsEnd) {
			out.print(T_ANY);  String sep = " ";
			for (int n = argsEnd;  n < m;  n++) {
	        	out.print(sep);  sep = ", ";
				out.print(FRAME+n);
			}
			out.println(";");
		}
		
		tryStart.clear();  tryEnd.clear();
		for (TryCatchBlockNode tryBlock : method.tryCatchBlocks) {
        	out.println("\t// try "+makeLabel(tryBlock.start)+" "+makeLabel(tryBlock.end)+" "+makeLabel(tryBlock.handler)+" "+tryBlock.type);
        	Label label = tryBlock.start.getLabel();
        	ArrayList<TryCatchBlockNode> list = tryStart.get(label);
        	if (list == null) {
        		list = new ArrayList<TryCatchBlockNode>();
        		tryStart.put(label, list);
        		tryEnd.put(tryBlock.end.getLabel(), list);
        	}
        	list.add(tryBlock);
        }
		
		methodIsSynchronized = null;
		if ((method.access & Opcodes.ACC_SYNCHRONIZED) != 0) {
			if ((method.access & Opcodes.ACC_STATIC) != 0) {
				methodIsSynchronized = CLASS_STRUCT_PREFIX+convertClassName(classNode.name)+"."+CLASS_CLASS+"."+CLASS_KLASS;
			} else {
				methodIsSynchronized = FRAME+"0";
			}
			emit(LIB_MONITOR_ENTER+"("+methodIsSynchronized+");");
		}
		
		InsnList ins = method.instructions;
		if (ins.size() > 0) {
		    Analyzer<BasicValue> a = new Analyzer<BasicValue>(new BasicInterpreter());
		    Frame<BasicValue>[] frames = a.analyze(classNode.name, method);
		    //OBSOLETE: I'm now handling interface conversion at method entry, not invoker
		    //Analyzer<TypedValue> a = new Analyzer<TypedValue>(new TypedInterpreter());
		    //Frame<TypedValue>[] frames = a.analyze(classNode.name, method);
		    for (int fx = 0; fx < frames.length; ++fx) {
		    	frame = frames[fx];
		    	AbstractInsnNode insn = ins.get(fx);
		    	insn.accept(insnCommenter);
		    	if (frame == null) {
		    		out.println("// DEAD CODE");
		    		continue;
		    	}
				if (Main.opt_debug) out.println("\t// "+frame);
				insn.accept(this);
		    }
		}
		
        out.println("}");
	}

	private void checkArgIsInterface(int frameLoc, Type argType) {
		if (argType.getSort() == Type.OBJECT) {
			String argClassName = argType.getInternalName();
			try {
				ClassNode argCN = classCache.get(argClassName);
				checkArgIsInterface(frameLoc, argCN);
			} catch (ClassNotFoundException | IOException e) {
				System.out.println("Unable to load "+argClassName+": "+e.getMessage());
			}
		}
	}

	private void checkArgIsInterface(int frameLoc, ClassNode argClassNode) {
		if ((argClassNode.access & Opcodes.ACC_INTERFACE) != 0) {
			String convIntfcName = convertClassName(argClassNode.name);
			argIsInterface[frameLoc] = "struct "+METHOD_STRUCT_PREFIX+convIntfcName+" *"+INTERFACE_METHODS+(frameLoc)
								+" = jbcc_find_interface("+FRAME+(frameLoc)+",&"+CLASS_STRUCT_PREFIX+convIntfcName+");";
		}
	}

	public static String externalMethodName(String owner, MethodNode method) {
		return convertClassName(owner)+"_"+convertMethodName(method.name,method.desc);
	}

	public static String convertMethodName(String name, String desc) {
		int descHash = (desc).hashCode();
		String sig = (descHash >= 0) ? Integer.toString(descHash, 36)
				 					: "M"+Integer.toString(-descHash, 36);
		if (Character.isJavaIdentifierStart(name.charAt(0))) {
			// normal methods
			return escapeName(name)+"_"+sig;			
		} else if (name.equals("<init>")) {
			return "_init_"+sig;
		} else if (name.equals("<clinit>")) {
			return "_clinit_"+sig;
		} else {
			return escapeName(name);			
		}
	}
	
	static String convertType(Type type) {
        switch (type.getSort()) {
        case Type.BOOLEAN:
        	return T_BOOLEAN;
        case Type.BYTE:
        	return T_BYTE;
        case Type.CHAR:
        	return T_CHAR;
        case Type.DOUBLE:
        	return T_DOUBLE;
        case Type.FLOAT:
        	return T_FLOAT;
        case Type.INT:
        	return T_INT;
        case Type.LONG:
        	return T_LONG;
        case Type.SHORT:
        	return T_SHORT;
        case Type.VOID:
        	return T_VOID;
        case Type.ARRAY:
        	return T_ARRAY_+convertType(type.getElementType());
        case Type.OBJECT:
        	return convertClassName(type.getInternalName());
        default:
    		throw new RuntimeException("Unexpected convertType "+type);
        }
	}

	static String convertTypeDesc(String desc) {
		return convertType(Type.getType(desc));
	}
	
    //OBSOLETE: I'm now handling interface conversion at method entry, not invoker
	//Frame<TypedValue> frame;
	Frame<BasicValue> frame;
	int argsEnd;
	
	String stack(int d, Type type) {
		String variant;  int width = 1;
        switch (type.getSort()) {
        case Type.BOOLEAN:
        case Type.BYTE:
        case Type.CHAR:
        case Type.SHORT:
        	// all above treated as INT
        case Type.INT:
            variant = FRAME_INT;
            break;
        case Type.FLOAT:
            variant = FRAME_FLOAT;
            break;
        case Type.LONG:
            variant = FRAME_LONG;  width = 2;
            break;
        case Type.DOUBLE:
            variant = FRAME_DOUBLE;  width = 2;
            break;
        case Type.ARRAY:
            variant = FRAME_ARRAY;
            break;
        // case Type.OBJECT:
        default:
            variant = FRAME_REFER_OBJECT;
        }
		return stack(d, variant, width);		
	}

	private String stack(int d, String variant, int width) {
		int loc = frame.getLocals() + frame.getStackSize() - d;
		// *width is not needed, because asm.tree.analysis.Frame does not use two stack slots
		// for double-word values. it does reserve a dummy slot in locals
		return FRAME+loc+variant;
	}
	
	private Type stackType(int d) {
		return frame.getStack(frame.getStackSize() - d).getType();
	}
	
	private boolean stackCategory2(int d) {
		return frame.getStack(frame.getStackSize() - d).getSize() == 2;
	}
	
	String local(int n, Type type) {
		if (n < argsEnd) {
			return FRAME+n;  // arguments don't have varying types
		}
		String variant;
        switch (type.getSort()) {
        case Type.BOOLEAN:
        case Type.CHAR:
        case Type.BYTE:
        case Type.SHORT:
        	// all above treated as INT
        case Type.INT:
            variant = FRAME_INT;
            break;
        case Type.FLOAT:
            variant = FRAME_FLOAT;
            break;
        case Type.LONG:
            variant = FRAME_LONG;
            break;
        case Type.DOUBLE:
            variant = FRAME_DOUBLE;
            break;
        case Type.ARRAY:
            variant = FRAME_ARRAY;
            break;
        // case Type.OBJECT:
        default:
            variant = FRAME_REFER_OBJECT;
        }
		return FRAME+n+variant;		
	}


	private String zero(Type type) {
        switch (type.getSort()) {
        case Type.INT:
            return "0";
		case Type.FLOAT:
            return "0.0F";
		case Type.LONG:
            return "0L";
		case Type.DOUBLE:
            return "0.0";
		default:
            throw new IllegalArgumentException("Unexpected type for zero constant "+type);
        }
	}
	
	private final static Type OBJECT_PSEUDO_TYPE = Type.VOID_TYPE;  //TODO this is a hack

	@Override
	public void aconst(Object cst) {
		if (cst == null) emit(stack(0,OBJECT_PSEUDO_TYPE)+" = "+NULL_REFERENCE+";");
		else {
	        String scn = stringCons.get((String)cst);
			emit(stack(0,OBJECT_PSEUDO_TYPE)+" = "+scn+"."+STRING_CONSTANT_STRING+" ? "+scn+"."+STRING_CONSTANT_STRING+" : "+LIB_INIT_STRING_CONST+"(&"+scn+");");
		}
	}

	private void emit(String string) {
		out.println(string);
	}

	@Override
	public void add(Type type) {
		dyadic(type," + ");
	}

	private void dyadic(Type type, String op) {
		emit(stack(2,type)+" = "+stack(2,type)+op+stack(1,type)+";");
	}

	@Override
	public void aload(Type type) {
		String ref = stack(2,FRAME_ARRAY,1);
		String ind = stack(1,Type.INT_TYPE);
		emitIndexCheck(ref, ind);
		emit(stack(2,type)+" = "+arrayIndex(ref,ind,type)+";");
	}

	private void emitIndexCheck(String ref, String ind) {
		emit("if ("+ref+" == "+NULL_REFERENCE+") "+LIB_THROW_NULL_POINTER_EXCEPTION+"();");
		emit("if ("+ind+" < 0 || "+ind+" >= (("+T_ARRAY_COMMON+"*)"+ref+")->"+ARRAY_LENGTH+") "+LIB_THROW_ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION+"("+ind+");");
	}
	
	private String arrayIndex(String ref, String ind, Type elemType) {
		return "(("+T_ARRAY_+convertType(elemType)+")"+ref+")->"+ARRAY_ELEMENTS+"["+ind+"]";
	}

	@Override
	public void and(Type type) {
		dyadic(type," & ");
	}

	@Override
	public void anew(Type type) {
		// assume LIB_NEW does this: emitClassInitialize(type.getInternalName());
		emit(stack(0,type)+" = "+LIB_NEW+"("+classPointer(type)+"."+CLASS_CLASS+");");
	}

	static String classPointer(Type type) {
		return "("+OBJECT_CLASS_TYPE+"*)&"+CLASS_STRUCT_PREFIX+convertType(type);
	}

	@Override
	public void areturn(Type type) {
		if (methodIsSynchronized != null) {
			emit("// "+LIB_MONITOR_EXIT+"("+methodIsSynchronized+");");
		}
		if (type == Type.VOID_TYPE) emit("return;");
		else emit("return "+stack(1,type)+";");
	}

	@Override
	public void arraylength() {
		String ref = stack(1,FRAME_ARRAY,1);
		emitReferenceCheck(ref);
		emit(stack(1,Type.INT_TYPE)+" = "+"(("+T_ARRAY_COMMON+"*)"+ref+")->"+ARRAY_LENGTH+";");
	}

	@Override
	public void astore(Type type) {
		String ref = stack(3,FRAME_ARRAY,1);
		String ind = stack(2,Type.INT_TYPE);
		emitIndexCheck(ref, ind);
		emit(arrayIndex(ref,ind,type)+" = "+stack(1,type)+";");
	}

	@Override
	public void athrow() {
		if (methodIsSynchronized != null) {
			emit("// "+LIB_MONITOR_EXIT+"("+methodIsSynchronized+");");
		}
		emit(LIB_THROW+"("+stack(1,OBJECT_PSEUDO_TYPE)+");");
		emit("/*NOTREACHED*/__builtin_unreachable(); // control reaches end of non-void function"); // clang and gcc
	}

	@Override
	public void cast(Type from, Type to) {
		String v = stack(1,from), r;
        if (from == Type.DOUBLE_TYPE) {
            if (to == Type.FLOAT_TYPE) {
            	r = "("+T_FLOAT+")"+v;
            } else if (to == Type.LONG_TYPE) {
                r = LIB_D2L+"("+v+")";
            } else {
                r = LIB_D2I+"("+v+")";
            }
        } else if (from == Type.FLOAT_TYPE) {
            if (to == Type.DOUBLE_TYPE) {
            	r = "("+T_DOUBLE+")"+v;
            } else if (to == Type.LONG_TYPE) {
                r = LIB_F2L+"("+v+")";
            } else {
                r = LIB_F2I+"("+v+")";
            }
        } else if (from == Type.LONG_TYPE) {
        	if (to == Type.DOUBLE_TYPE) {
            	r = "("+T_DOUBLE+")"+v;
            } else if (to == Type.FLOAT_TYPE) {
            	r = "("+T_FLOAT+")"+v;
            } else {
            	r = "("+T_INT+")"+v;
            }
        } else {
            if (to == Type.BYTE_TYPE) {
            	r = "("+T_BYTE+")"+v;
            } else if (to == Type.CHAR_TYPE) {
            	r = "("+T_CHAR+")"+v;
            } else if (to == Type.DOUBLE_TYPE) {
            	r = "("+T_DOUBLE+")"+v;
            } else if (to == Type.FLOAT_TYPE) {
            	r = "("+T_FLOAT+")"+v;
            } else if (to == Type.LONG_TYPE) {
            	r = "("+T_LONG+")"+v;
            } else if (to == Type.SHORT_TYPE) {
            	r = "("+T_SHORT+")"+v;
            } else return;  // INT to INT
        }
		emit(stack(1,to)+" = "+r+";");
	}

	@Override
	public void checkcast(Type type) {
		String ref = stack(1,OBJECT_PSEUDO_TYPE);
		emit(LIB_CHECK_CAST+"("+ref+", "+classPointer(type)+");");
	}

	@Override
	public void cmpg(Type type) {
	    // (From toba-1.1c) this is carefully crafted to make NaN cases come out right
		emit(stack(2,Type.INT_TYPE)+" = (" +
	    			stack(2,type) + " < " + stack(1,type) + ") ? -1 : ((" +
	    			stack(2,type) + " == " + stack(1,type) + ") ? 0 : 1);");
	}

	@Override
	public void cmpl(Type type) {
	    // (From toba-1.1c) this is carefully crafted to make NaN cases come out right
		emit(stack(2,Type.INT_TYPE)+" = (" +
	    			stack(2,type) + " > " + stack(1,type) + ") ? 1 : ((" +
	    			stack(2,type) + " == " + stack(1,type) + ") ? 0 : -1);");
	}

	@Override
	public void dconst(double cst) {
		emit(stack(0,Type.DOUBLE_TYPE)+" = "+doubleLiteral(cst)+";");
	}
	
	static String doubleLiteral(double v) {
		if (Double.isNaN(v)) return "FLOAT_NAN";
		else if (Double.isInfinite(v)) return (v > 0.0) ? "HUGE_VAL" : "-HUGE_VAL";
		else return Double.toString(v);
	}

	@Override
	public void div(Type type) {
		if (type == Type.INT_TYPE) {
			emit(stack(2,type)+" = "+LIB_IDIV+"("+stack(2,type)+", "+stack(1,type)+");");
		} else if (type == Type.LONG_TYPE) {
			emit(stack(2,type)+" = "+LIB_LDIV+"("+stack(2,type)+", "+stack(1,type)+");");
		} else {
			emit("if ("+stack(1,type)+" == "+zero(type)+") "+LIB_THROW_DIVISION_BY_ZERO+"();");
			emit(stack(2,type)+" = "+stack(2,type)+" / "+stack(1,type)+";");
		}
	}

	@Override
	public void dup() {
		// ..., value →
		// ..., value, value
		// don't receive any type information
		emit(stack(0,FRAME_ANY,1)+" = "+stack(1,FRAME_ANY,1)+";");
	}

	@Override
	public void dup2() {
		//	Form 1:
		//	..., value2, value1 →
		//	..., value2, value1, value2, value1
		//	where both value1 and value2 are values of a category 1 computational type (§2.11.1).
		//	Form 2:
		//	..., value →
		//	..., value, value
		//	where value is a value of a category 2 computational type (§2.11.1).
		if (stackCategory2(1)) {  // Form 2
	        String s1 = stack(1,FRAME_ANY,1);
			String s0 = stack(0,FRAME_ANY,1);
			emit(s0+" = "+s1+";");
		} else {
	        String s1 = stack(1,FRAME_ANY,1);
	        String s2 = stack(2,FRAME_ANY,1);
			String s0 = stack(0,FRAME_ANY,1);
			String sm = stack(-1,FRAME_ANY,1);
			emit(sm+" = "+s1+";");
			emit(s0+" = "+s2+";");
			emit(s2+" = "+s0+";");
		}
	}

	@Override
	public void dup2X1() {
		//	Form 1:
		//	..., value3, value2, value1 →
		//	..., value2, value1, value3, value2, value1
		//	where value1, value2, and value3 are all values of a category 1 computational type (§2.11.1).
		//	Form 2:
		//	..., value2, value1 →
		//	..., value1, value2, value1
		//	where value1 is a value of a category 2 computational type and value2 is a value of a category 1 computational type 	
		if (stackCategory2(1)) {  // Form 2
	        String s1 = stack(1,FRAME_ANY,1);
	        String s2 = stack(2,FRAME_ANY,1);
			String s0 = stack(0,FRAME_ANY,1);
			emit(s0+" = "+s1+";");
			emit(s1+" = "+s2+";");
			emit(s2+" = "+s0+";");
		} else {
	        String s1 = stack(1,FRAME_ANY,1);
	        String s2 = stack(2,FRAME_ANY,1);
	        String s3 = stack(3,FRAME_ANY,1);
			String s0 = stack(0,FRAME_ANY,1);
			String sm = stack(-1,FRAME_ANY,1);
			emit(sm+" = "+s1+";");
			emit(s0+" = "+s2+";");
			emit(s1+" = "+s3+";");
			emit(s2+" = "+sm+";");
			emit(s3+" = "+s0+";");
		}
	}

	@Override
	public void dup2X2() {
		//	Form 1:
		//	..., value4, value3, value2, value1 →
		//	..., value2, value1, value4, value3, value2, value1
		//	where value1, value2, value3, and value4 are all values of a category 1 computational type (§2.11.1).
		//	Form 2:
		//	..., value3, value2, value1 →
		//	..., value1, value3, value2, value1
		//	where value1 is a value of a category 2 computational type and value2 and value3 are both values of a category 1 computational type (§2.11.1).
		//	Form 3:
		//	..., value3, value2, value1 →
		//	..., value2, value1, value3, value2, value1
		//	where value1 and value2 are both values of a category 1 computational type and value3 is a value of a category 2 computational type (§2.11.1).
		//	Form 4:
		//	..., value2, value1 →
		//	..., value1, value2, value1
		//	where value1 and value2 are both values of a category 2 computational type (§2.11.1).
		if (stackCategory2(1)) {
			if (stackCategory2(2)) { // Form 4
		        String s1 = stack(1,FRAME_ANY,1);
		        String s2 = stack(2,FRAME_ANY,1);
				String s0 = stack(0,FRAME_ANY,1);
				emit(s0+" = "+s1+";");
				emit(s1+" = "+s2+";");
				emit(s2+" = "+s0+";");
			} else { // Form 2
		        String s1 = stack(1,FRAME_ANY,1);
		        String s2 = stack(2,FRAME_ANY,1);
		        String s3 = stack(3,FRAME_ANY,1);
				String s0 = stack(0,FRAME_ANY,1);
				emit(s0+" = "+s1+";");
				emit(s1+" = "+s2+";");
				emit(s2+" = "+s3+";");
				emit(s3+" = "+s0+";");
			}
		} else if (stackCategory2(3)) { // Form 3
	        String s1 = stack(1,FRAME_ANY,1);
	        String s2 = stack(2,FRAME_ANY,1);
	        String s3 = stack(3,FRAME_ANY,1);
			String s0 = stack(0,FRAME_ANY,1);
			String sm = stack(-1,FRAME_ANY,1);
			emit(sm+" = "+s1+";");
			emit(s0+" = "+s2+";");
			emit(s1+" = "+s3+";");
			emit(s2+" = "+sm+";");
			emit(s3+" = "+s0+";");
		} else {
	        String s1 = stack(1,FRAME_ANY,1);
	        String s2 = stack(2,FRAME_ANY,1);
	        String s3 = stack(3,FRAME_ANY,1);
	        String s4 = stack(4,FRAME_ANY,1);
			String s0 = stack(0,FRAME_ANY,1);
			String sm = stack(-1,FRAME_ANY,1);
			emit(sm+" = "+s1+";");
			emit(s0+" = "+s2+";");
			emit(s1+" = "+s3+";");
			emit(s2+" = "+s4+";");
			emit(s3+" = "+sm+";");
			emit(s4+" = "+s0+";");
		}
	}

	@Override
	public void dupX1() {
		// ..., value2, value1 →
		// ..., value1, value2, value1
        String s1 = stack(1,FRAME_ANY,1);
        String s2 = stack(2,FRAME_ANY,1);
		String s0 = stack(0,FRAME_ANY,1);
		emit(s0+" = "+s1+";");
		emit(s1+" = "+s2+";");
		emit(s2+" = "+s0+";");
	}

	@Override
	public void dupX2() {
		//	Form 1:
		//	..., value3, value2, value1 →
		//	..., value1, value3, value2, value1
		//	where value1, value2, and value3 are all values of a category 1 computational type (§2.11.1).
		//	Form 2:
		//	..., value2, value1 →
		//	..., value1, value2, value1
		//	where value1 is a value of a category 1 computational type and value2 is a value of a category 2 computational type (§2.11.1). 
		if (stackCategory2(1)) { // Form 2
	        String s1 = stack(1,FRAME_ANY,1);
	        String s2 = stack(2,FRAME_ANY,1);
			String s0 = stack(0,FRAME_ANY,1);
			emit(s0+" = "+s1+";");
			emit(s1+" = "+s2+";");
			emit(s2+" = "+s0+";");
		} else {
	        String s1 = stack(1,FRAME_ANY,1);
	        String s2 = stack(2,FRAME_ANY,1);
	        String s3 = stack(3,FRAME_ANY,1);
			String s0 = stack(0,FRAME_ANY,1);
			emit(s0+" = "+s1+";");
			emit(s1+" = "+s2+";");
			emit(s2+" = "+s3+";");
			emit(s3+" = "+s0+";");
		}
	}

	@Override
	public void fconst(float cst) {
		emit(stack(0,Type.FLOAT_TYPE)+" = "+floatLiteral(cst)+";");
	}
	
	static String floatLiteral(float v) {
		if (Float.isNaN(v)) return "FLOAT_NAN";
		else if (Float.isInfinite(v)) return (v > 0.0) ? "HUGE_VALF" : "-HUGE_VALF";
		else return Float.toString(v)+"F";
	}

	@Override
	public void getfield(String owner, String name, String desc) {
		String ref = stack(1,OBJECT_PSEUDO_TYPE);
		emitReferenceCheck(ref);
		emit(stack(1,Type.getType(desc))+" = "+objectField(ref,owner,name)+";");
	}
	
	private String objectField(String ref, String owner, String name) {
		return "(("+convertClassName(owner)+")"+ref+")->"+escapeName(name);
	}

	private void emitReferenceCheck(String ref) {
		emit("if ("+ref+" == "+NULL_REFERENCE+") "+LIB_THROW_NULL_POINTER_EXCEPTION+"();");
	}

	@Override
	public void getstatic(String owner, String name, String desc) {
		owner = resolveStaticField(owner, name, desc);
		emitClassInitialize(owner);
		emit(stack(0,Type.getType(desc))+" = "+staticFieldName(owner,name)+";");
	}

	private String resolveStaticField(String owner, String name, String desc) {
		ClassNode cn;
		try {
			cn = classCache.get(owner);
		} catch (ClassNotFoundException | IOException e) {
			System.err.println("resolveStaticField could not load class "+owner+": "+e.getMessage());
			return null;
		}
		for (int fx = 0; fx < cn.fields.size(); ++fx) {
			FieldNode field = cn.fields.get(fx);
			if (field.name.equals(name)
				&& (field.access & Opcodes.ACC_STATIC) != 0) {
				assert field.desc.equals(desc);
				return owner;
			}
		}
		for (int ix = 0; ix < cn.interfaces.size(); ++ix) {
            String intfc = cn.interfaces.get(ix);
            String r = resolveStaticField(intfc,name,desc);
            if (r != null) return r;
		}
		if (cn.superName != null) return resolveStaticField(cn.superName,name,desc);
		return null;
	}

	private String staticFieldName(String owner, String name) {
		return CLASS_STRUCT_PREFIX+convertClassName(owner)+"."+CLASS_STATIC_FIELDS+"."+escapeName(name);
	}

	private void emitClassInitialize(String owner) {
		emit("if (!"+CLASS_STRUCT_PREFIX+convertClassName(owner)+"."+CLASS_CLASS+"."+CLASS_INITIALIZED+") "+LIB_INIT_CLASS+"(("+OBJECT_CLASS_TYPE+"*)&"+CLASS_STRUCT_PREFIX+convertClassName(owner)+");");
	}

	public static String convertClassName(String owner) {
		return escapeName(owner);
	}

	static HashSet<String> CKeyword = new HashSet<String>();
	static {
		// really only need the C keywords that are not also Java keywords
		CKeyword.add("auto");
		CKeyword.add("break");
		CKeyword.add("case");
		CKeyword.add("char");
		CKeyword.add("const");
		CKeyword.add("continue");
		CKeyword.add("default");
		CKeyword.add("do");
		CKeyword.add("double");
		CKeyword.add("else");
		CKeyword.add("enum");
		CKeyword.add("extern");
		CKeyword.add("float");
		CKeyword.add("for");
		CKeyword.add("goto");
		CKeyword.add("if");
		CKeyword.add("inline");
		CKeyword.add("int");
		CKeyword.add("long");
		CKeyword.add("register");
		CKeyword.add("restrict");
		CKeyword.add("return");
		CKeyword.add("short");
		CKeyword.add("signed");
		CKeyword.add("sizeof");
		CKeyword.add("static");
		CKeyword.add("struct");
		CKeyword.add("switch");
		CKeyword.add("typedef");
		CKeyword.add("union");
		CKeyword.add("unsigned");
		CKeyword.add("void");
		CKeyword.add("volatile");
		CKeyword.add("while");
			
		CKeyword.add("_Alignas");
		CKeyword.add("_Alignof");
		CKeyword.add("_Atomic");
		CKeyword.add("_Bool");
		CKeyword.add("_Complex");
		CKeyword.add("_Generic");
		CKeyword.add("_Imaginary");
		CKeyword.add("_Noreturn");
		CKeyword.add("_Static_assert");
		CKeyword.add("_Thread_local");
		
		CKeyword.add("asm"); 
	}
	public static String escapeName(String name) {
		if (CKeyword.contains(name)) return name+"_";
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
			} else if (c == '/') {
				sb.append("_");  // / changed to _
			} else if (c == '_') {
				sb.append("__");  // double up underscore
			} else {
				if (formatter == null) formatter = new Formatter(sb); // formatter is now needed
				if (c < 0xFF) formatter.format("_x%02x",(int)c);  // escape Unicode
				else formatter.format("_u%04x",(int)c);  // escape Unicode
			}
		}
		return sb.toString();
	}

	@Override
	public void goTo(Label target) {
		emit("goto "+makeLabel(target)+";");
	}

	@Override
	public void hconst(Handle cst) {
		// TODO 
		emit(stack(0,OBJECT_PSEUDO_TYPE)+" = *TODO*Handle:"+cst.toString()+";");
	}

	@Override
	public void iconst(int cst) {
		emit(stack(0,Type.INT_TYPE)+" = "+intLiteral(cst)+";");
	}
	
	static String intLiteral(int v) {
		if (v == Integer.MIN_VALUE) return "INT32_MIN";
		else return Integer.toString(v);
	}

	@Override
	public void ifacmpeq(Label target) {
		branch(stack(2,OBJECT_PSEUDO_TYPE),"==",stack(1,OBJECT_PSEUDO_TYPE),target);
	}

	@Override
	public void ifacmpne(Label target) {
		branch(stack(2,OBJECT_PSEUDO_TYPE),"!=",stack(1,OBJECT_PSEUDO_TYPE),target);
	}

	@Override
	public void ifeq(Label target) {
		branch(stack(1,Type.INT_TYPE),"==","0",target);
	}

	private void branch(String one, String op, String two, Label target) {
		emit("if ("+one+" "+op+" "+two+") goto "+makeLabel(target)+";");
	}

	@Override
	public void ifge(Label target) {
		branch(stack(1,Type.INT_TYPE),">=","0",target);
	}

	@Override
	public void ifgt(Label target) {
		branch(stack(1,Type.INT_TYPE),">","0",target);
	}

	@Override
	public void ificmpeq(Label target) {
		branch(stack(2,Type.INT_TYPE),"==",stack(1,Type.INT_TYPE),target);
	}

	@Override
	public void ificmpge(Label target) {
		branch(stack(2,Type.INT_TYPE),">=",stack(1,Type.INT_TYPE),target);
	}

	@Override
	public void ificmpgt(Label target) {
		branch(stack(2,Type.INT_TYPE),">",stack(1,Type.INT_TYPE),target);
	}

	@Override
	public void ificmple(Label target) {
		branch(stack(2,Type.INT_TYPE),"<=",stack(1,Type.INT_TYPE),target);
	}

	@Override
	public void ificmplt(Label target) {
		branch(stack(2,Type.INT_TYPE),"<",stack(1,Type.INT_TYPE),target);
	}

	@Override
	public void ificmpne(Label target) {
		branch(stack(2,Type.INT_TYPE),"!=",stack(1,Type.INT_TYPE),target);
	}

	@Override
	public void ifle(Label target) {
		branch(stack(1,Type.INT_TYPE),"<=","0",target);
	}

	@Override
	public void iflt(Label target) {
		branch(stack(1,Type.INT_TYPE),"<","0",target);
	}

	@Override
	public void ifne(Label target) {
		branch(stack(1,Type.INT_TYPE),"!=","0",target);
	}

	@Override
	public void ifnonnull(Label target) {
		branch(stack(1,OBJECT_PSEUDO_TYPE),"!=",NULL_REFERENCE,target);
	}

	@Override
	public void ifnull(Label target) {
		branch(stack(1,OBJECT_PSEUDO_TYPE),"==",NULL_REFERENCE,target);
	}

	@Override
	public void iinc(int lclIndex, int increment) {
		emit(local(lclIndex,Type.INT_TYPE)+" += "+Integer.toString(increment)+";");
	}

	@Override
	public void instanceOf(Type type) {
		String ref = stack(1,OBJECT_PSEUDO_TYPE);
		emit(LIB_INSTANCEOF+"("+ref+", "+classPointer(type)+");");
	}

	@Override
	public void invokedynamic(String name, String desc, Handle bsm, Object[] bsmArgs) {
		//TODO
		unimplemented("invokedynamic");
	}

	@Override
	public void invokeinterface(String owner, String name, String desc) {
		invoke(owner, name, InvokeKind.INTERFACE, desc);
	}

	@Override
	public void invokespecial(String owner, String name, String desc, boolean itf) {
		assert !itf;
		invoke(owner, name, InvokeKind.VIRTUAL, desc);
		//TODO this has not yet captured the distinction from INVOKEVIRTUAL https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-6.html#jvms-6.5.invokespecial
	}

	@Override
	public void invokestatic(String owner, String name, String desc, boolean itf) {
		assert !itf;
		emitClassInitialize(owner);
		invoke(owner, name, InvokeKind.STATIC, desc);
	}

	@Override
	public void invokevirtual(String owner, String name, String desc, boolean itf) {
		assert !itf;
		invoke(owner, name, InvokeKind.VIRTUAL, desc);
	}
	
	enum InvokeKind { STATIC, VIRTUAL, INTERFACE };

	private void invoke(String owner, String name, InvokeKind kind, String desc) {
		Type retType = Type.getReturnType(desc);
		Type[] argTypes = Type.getArgumentTypes(desc);
		int numArgs = argTypes.length;
		int reserve = (kind == InvokeKind.STATIC) ? 0 : 1;
		String[] argList = new String[reserve+numArgs];
		int d = 0;
		for (int i = numArgs-1;  i >= 0;  --i) {
			Type argType = argTypes[i];
			argList[reserve+i] = stack(++d,argType);
			// arguments being passed are on stack, and asm.tree.analysis.Frame does not
			// reserve a second slot there for category 2 values
		}
		StringBuilder call = new StringBuilder();
		String prefix=null, ref, ownerType;
		if (owner.charAt(0) == '[') {
			ownerType = convertType(Type.getType(owner));
		} else {
			ownerType = convertClassName(owner);
		}
		switch (kind) {
		case STATIC:
			//TODO monitorenter on class if synchronized method
			prefix = ownerType+"_";
			break;
		case VIRTUAL:
			ref = stack(++d,OBJECT_PSEUDO_TYPE);
			argList[0] = ref;
			emitReferenceCheck(ref);
			prefix = "(("+ownerType+")"+ref+")->"+OBJECT_CLASS+"->"+CLASS_METHOD_TABLE+".";
			break;
		case INTERFACE:
			ref = stack(++d,OBJECT_PSEUDO_TYPE); //TODO INTERFACE_PSEUDO_TYPE
			argList[0] = ref;
			emitReferenceCheck(ref);
			prefix = "((struct "+METHOD_STRUCT_PREFIX+ownerType+"*)"+stack(d,FRAME_ANY,1)+FRAME_REFER_METHODS+")->";
			break;
		}
		call.append(prefix).append(convertMethodName(name,desc)).append("(")
			.append(ENV_ARG);
		for (int i = 0;  i < argList.length;  ++i) {
			String argVal = argList[i];
			call.append(",").append(argVal);
		}
		call.append(")");
		if (retType == Type.VOID_TYPE) {
			emit(call.toString()+";");
		} else {
			emit(stack(d,retType)+" = "+call.toString()+";");
		}
	}

	@Override
	public void jsr(Label target) {
		unimplemented("jsr "+makeLabel(target));
		// recompile with -target 1.7 or later
	}

	@Override
	public void lcmp() {
		emit(stack(2,Type.INT_TYPE)+" = (" +
    			stack(2,Type.LONG_TYPE) + " > " + stack(1,Type.LONG_TYPE) + ") ? 1 : ((" +
    			stack(2,Type.LONG_TYPE) + " == " + stack(1,Type.LONG_TYPE) + ") ? 0 : -1);");
	}

	@Override
	public void lconst(long cst) {
		emit(stack(0,Type.LONG_TYPE)+" = "+longLiteral(cst)+";");
	}
	
	static String longLiteral(long v) {
		if (v == Long.MIN_VALUE) return "INT64_MIN";
		else return Long.toString(v);
	}

	@Override
	public void load(int lclIndex, Type type) {
		if (lclIndex < argIsInterface.length && argIsInterface[lclIndex] != null) {
			emit(stack(0,type)+" = "+local(lclIndex,type)+";  // interface");
			emit(stack(0,FRAME_ANY,1)+FRAME_REFER_METHODS+" = "+INTERFACE_METHODS+(lclIndex)+";");
		} else emit(stack(0,type)+" = "+local(lclIndex,type)+";");
	}

	static int switchCount = 0;

	private HashMap<Label,ArrayList<TryCatchBlockNode>> tryStart = new HashMap<Label,ArrayList<TryCatchBlockNode>>();

	private HashMap<Label,ArrayList<TryCatchBlockNode>> tryEnd = new HashMap<Label,ArrayList<TryCatchBlockNode>>();

	@Override
	public void lookupswitch(Label defalt, int[] matches, Label[] targets) {
		String tableName = "switchTable"+(switchCount++);
		StringBuilder table = new StringBuilder();
		String sep = "";
        for (int i = 0; i < matches.length; i++) {
        	table.append(sep).append("{").append(matches[i]).append(",&&").append(makeLabel(targets[i])).append("}");
        	sep = ",";
        }
		emit("static "+SWITCH_PAIR_TYPE+" "+tableName+"[] = {"+table+"};");
		emit("goto *"+LIB_LOOKUPSWITCH+"("+stack(1,Type.INT_TYPE)+", "+matches.length+", "+tableName+", &&"+makeLabel(defalt)+");");
	}

	@Override
	public void mark(Label label) {
		emit(makeLabel(label)+": ;");
		ArrayList<TryCatchBlockNode> tryList = tryStart.get(label);
		if (tryList != null) {
			Label startLabel = tryList.get(0).start.getLabel();
			String listName = CATCH_LIST_PREFIX+makeLabel(startLabel);
			StringBuilder list = new StringBuilder();
			String sep = "";
			for (TryCatchBlockNode tryBlock : tryList) {
	        	out.println("\t// try "+makeLabel(tryBlock.start)+" "+makeLabel(tryBlock.end)+" "+makeLabel(tryBlock.handler)+" "+tryBlock.type);
				list.append(sep).append("{");
	        	String type = tryBlock.type;
	        	if (type == null) list.append(NULL_REFERENCE);
	        	else list.append("&").append(CLASS_STRUCT_PREFIX).append(convertClassName(type));
				list.append(",&&").append(makeLabel(tryBlock.handler)).append("}");
	        	sep = ",";
	        }
        	list.append(sep).append("{0,0}");
        	sep = ",";
			emit("static "+CATCH_LIST_TYPE+" "+listName+"[] = {"+list+"};");
			emit(CATCH_PUSH+"("+ENV_ARG+","+listName+");");
		}
		tryList = tryEnd.get(label);
		if (tryList != null) {
			Label startLabel = tryList.get(0).start.getLabel();
			String listName = CATCH_LIST_PREFIX+makeLabel(startLabel);
			out.println("\t// try end "+makeLabel(startLabel));
			emit(CATCH_POP+"("+ENV_ARG+","+listName+");");			
		}
	}

	@Override
	public void monitorenter() {
		emit(LIB_MONITOR_ENTER+"("+stack(1,OBJECT_PSEUDO_TYPE)+");");
	}

	@Override
	public void monitorexit() {
		emit(LIB_MONITOR_EXIT+"("+stack(1,OBJECT_PSEUDO_TYPE)+");");
	}

	@Override
	public void mul(Type type) {
		dyadic(type," * ");
	}

	@Override
	public void multianewarray(String desc, int dims) {
		Type type = Type.getType(desc);
		StringBuilder args = new StringBuilder();
		args.append(dims);
		for (int d = dims;  d > 0;  d--) {
			args.append(',').append(stack(d,Type.INT_TYPE));
		}
		Type elemType = type;
		while (elemType.getSort() == Type.ARRAY) elemType = elemType.getElementType();
		String at;
        switch (elemType.getSort()) {
        case Type.BOOLEAN:
        	at = T_BOOLEAN;
        	break;
        case Type.CHAR:
        	at = T_CHAR;
        	break;
        case Type.BYTE:
        	at = T_BYTE;
        	break;
        case Type.SHORT:
        	at = T_SHORT;
        	break;
        case Type.INT:
        	at = T_INT;
            break;
        case Type.FLOAT:
        	at = T_FLOAT;
            break;
        case Type.LONG:
        	at = T_LONG;
            break;
        case Type.DOUBLE:
        	at = T_DOUBLE;
            break;
        case Type.ARRAY:
        	at = T_BOOLEAN;
            break;
        // case Type.OBJECT:
        default:
        	// we don't have a T_ for objects, so this finishes the method here
        	at = "object";
    		emit(stack(dims,type)+" = "+LIB_NEW_ARRAY_MULTI_+"object"+"("+classPointer(elemType)+","+args+");");
        	return;
		}
		emit(stack(dims,type)+" = "+LIB_NEW_ARRAY_MULTI_+at+"("+args+");");
	}

	@Override
	public void neg(Type type) {
		emit(stack(1,type)+" = "+zero(type)+" - "+stack(1,type)+";");
	}

	@Override
	public void newarray(Type type) {
		Type aType;
		String at;
        switch (type.getSort()) {
        case Type.BOOLEAN:
        	at = T_BOOLEAN; aType = Type.getType("[Z");
        	break;
        case Type.CHAR:
        	at = T_CHAR; aType = Type.getType("[C");
        	break;
        case Type.BYTE:
        	at = T_BYTE; aType = Type.getType("[B");
        	break;
        case Type.SHORT:
        	at = T_SHORT; aType = Type.getType("[S");
        	break;
        case Type.INT:
        	at = T_INT; aType = Type.getType("[I");
            break;
        case Type.FLOAT:
        	at = T_FLOAT; aType = Type.getType("[F");
            break;
        case Type.LONG:
        	at = T_LONG; aType = Type.getType("[J");
            break;
        case Type.DOUBLE:
        	at = T_DOUBLE; aType = Type.getType("[D");
            break;
        case Type.ARRAY:
        	at = T_BOOLEAN; aType = Type.getType("[Z");
            break;
        // case Type.OBJECT:
        default:
        	// we don't have a T_ for objects, so this finishes the method here
        	aType = Type.getType("["+type.getDescriptor());
			emit(stack(1,aType)+" = "+LIB_NEW_ARRAY_OBJECT+"("+stack(1,Type.INT_TYPE)+","+classPointer(type)+");");
			return;
		}
		emit(stack(1,aType)+" = "+LIB_NEW_ARRAY_+at+"("+stack(1,Type.INT_TYPE)+");");
	}

	@Override
	public void nop() {
		// empty
	}

	@Override
	public void or(Type type) {
		dyadic(type," | ");
	}

	@Override
	public void pop() {
		// empty
	}

	@Override
	public void pop2() {
		// empty
	}

	@Override
	public void putfield(String owner, String name, String desc) {
		String ref = stack(2,OBJECT_PSEUDO_TYPE);
		emitReferenceCheck(ref);
		emit(objectField(ref,owner,name)+" = "+stack(1,Type.getType(desc))+";");
	}

	@Override
	public void putstatic(String owner, String name, String desc) {
		owner = resolveStaticField(owner, name, desc);
		emitClassInitialize(owner);
		emit(staticFieldName(owner,name)+" = "+stack(1,Type.getType(desc))+";");
	}

	@Override
	public void rem(Type type) {
		if (type == Type.INT_TYPE) {
			emit(stack(2,type)+" = "+LIB_IREM+"("+stack(2,type)+", "+stack(1,type)+");");
		} else if (type == Type.LONG_TYPE) {
			emit(stack(2,type)+" = "+LIB_LREM+"("+stack(2,type)+", "+stack(1,type)+");");
		} else if (type == Type.DOUBLE_TYPE) {
			emit(stack(2,type)+" = "+LIB_DREM+"("+stack(2,type)+", "+stack(1,type)+");");
		} else if (type == Type.FLOAT_TYPE) {
			emit(stack(2,type)+" = "+LIB_FREM+"("+stack(2,type)+", "+stack(1,type)+");");
		} else {
			emit("if ("+stack(1,type)+" == "+zero(type)+") "+LIB_THROW_DIVISION_BY_ZERO+"();");
			dyadic(type," % ");
		}
	}

	@Override
	public void ret(int lclIndex) {
		unimplemented("ret "+lclIndex);
	}

	@Override
	public void shl(Type type) {
		shift(type," << ",false);
	}
	
	public void shift(Type type, String op, Boolean unsigned) {
		String maxShift = (type == Type.LONG_TYPE) ? "63" : "31";
		String s = stack(2,type);
		if (unsigned) s = "((unsigned "+((type == Type.LONG_TYPE) ? T_LONG : T_INT)+")"+s+")";
		emit(stack(2,type)+" = "+s+op+"("+stack(1,Type.INT_TYPE)+" & "+maxShift+");");
	}

	@Override
	public void shr(Type type) {
		shift(type," >> ",false);
	}

	@Override
	public void store(int lclIndex, Type type) {
		String trunc;
		if (type == Type.BOOLEAN_TYPE) trunc = "("+T_BOOLEAN+")";
		else if (type == Type.BYTE_TYPE) trunc = "("+T_BYTE+")";
		else if (type == Type.CHAR_TYPE) trunc = "("+T_CHAR+")";
		else if (type == Type.SHORT_TYPE) trunc = "("+T_SHORT+")";
		else trunc = "";
		emit(local(lclIndex,type)+" = "+trunc+stack(1,type)+";");
	}

	@Override
	public void sub(Type type) {
		dyadic(type," - ");
	}

	@Override
	public void swap() {
		//	..., value2, value1 →
		//	..., value1, value2
		String s1 = stack(1,FRAME_ANY,1);
        String s2 = stack(2,FRAME_ANY,1);
		emit("{"+FRAME+" "+FRAME_SWAP+" = "+s1+"; "+s1+" = "+s2+"; "+s2+" = "+FRAME_SWAP+";}");
//		emit(FRAME_SWAP+" = "+s1+";");
//		emit(s1+" = "+s2+";");
//		emit(s2+" = "+FRAME_SWAP+";");
	}

	@Override
	public void tableswitch(int low, int high, Label defalt, Label... targets) {
		String tableName = "switchTable"+(switchCount++);
		StringBuilder table = new StringBuilder();
		String sep = "";
        for (int i = low; i <= high; i++) {
        	table.append(sep).append("&&").append(makeLabel(targets[i-low]));
        	sep = ",";
        }
		emit("static "+LABEL_PTR_TYPE+" "+tableName+"[] = {"+table+"};");
		String s = stack(1,Type.INT_TYPE);
		String dflt = makeLabel(defalt);
		emit("if ("+s+" < "+low+") goto "+dflt+";");
		emit("if ("+s+" > "+high+") goto "+dflt+";");
		emit("goto *"+tableName+"["+s+"-("+low+")];");
	}

	@Override
	public void tconst(Type type) {
		emit(stack(0,OBJECT_PSEUDO_TYPE)+" = "+LIB_GET_TYPE+"("+classPointer(type)+");");
	}

	@Override
	public void ushr(Type type) {
		shift(type," >> ",false);
	}

	@Override
	public void xor(Type type) {
		dyadic(type," ^ ");
	}

	public static String makeLabel(Label label) {
		return "L"+Integer.toString(System.identityHashCode(label),36);
	}

	private static String makeLabel(LabelNode labelNode) {
		return makeLabel(labelNode.getLabel());
	}

	private void unimplemented(String string) {
		emit("UNIMPLEMENTED "+string);
		System.out.println("Unimplemented "+string);
	}
}
