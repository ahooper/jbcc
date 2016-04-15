package ca.nevdull.jbcc3;

import java.io.PrintWriter;
import java.util.Formatter;
import java.util.List;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

public class MethodCompiler extends InstructionAdapter implements CCode {

	private PrintWriter out;

	private InstructionCommenter insnCommenter;
	private StringConstants stringCons;
	
	public MethodCompiler(PrintWriter out) {
		super(Opcodes.ASM5, null);
		this.out = out;
		stringCons = new StringConstants(out);
		insnCommenter = new InstructionCommenter(out);
	}

	public void setOut(PrintWriter out) {
		this.out.flush();
		this.out = out;
		insnCommenter.setOut(out);
		stringCons.setOut(out);
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
        
        if ((method.access & Opcodes.ACC_NATIVE) != 0) {
        	out.println("/*NATIVE*/");
        	return;
        }
        if ((method.access & Opcodes.ACC_ABSTRACT) != 0) return;  // does this occur?

		Type[] argTypes = Type.getArgumentTypes(method.desc);
		Type returnType = Type.getReturnType(method.desc);
        boolean virtual = (method.access & Opcodes.ACC_STATIC) == 0;
		
        out.print(convertType(returnType));
		out.print(" ");
		out.print(externalMethodName(classNode.name, method));
		out.print("(");

		String sep = "";
		if (virtual) {
        	out.print(convertClassName(classNode.name)/*this*/);
        	out.print(" ");
        	out.print(FRAME);
        	out.print(0);
        	sep = ", ";
        }
		int v = virtual ? 1 : 0;
        for (int i = 0;  i < argTypes.length;  i++) {
        	out.print(sep);  sep = ", ";
        	out.print(convertType(argTypes[i]));
        	out.print(" ");
        	out.print(FRAME);
        	out.print(i+v);
        }
		out.println(") {");
		
		InsnList ins = method.instructions;
		if (ins.size() > 0) {
		    Analyzer<BasicValue> a = new Analyzer<BasicValue>(new BasicInterpreter());
		    Frame<BasicValue>[] frames = a.analyze(classNode.name, method);
		    for (int fx = 0; fx < frames.length; ++fx) {
		    	frame = frames[fx];
		    	AbstractInsnNode insn = ins.get(fx);
		    	insn.accept(insnCommenter);  
				//out.println(frame);
				insn.accept(this);
		    }
		}
		
        out.println("}");
	}

	public static String externalMethodName(String owner, MethodNode method) {
		return convertClassName(owner)+"_"+convertMethodName(method.name,method.desc);
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

	public static String convertType(String desc) {
		return convertType(Type.getType(desc));
	}
/*

	@Override
	public void visitParameter(String name, int access) {
		out.println("\tParameter\t"+name+" "+access);
	}

	@Override
	public void visitAttribute(Attribute attr) {
		out.println("\tattribute\t"+attr.type);
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		out.println("\tTryCatchBlock\t"+makeLabel(start)+" "+makeLabel(end)+" "+makeLabel(handler)+" "+type);
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		if (signature==null) signature = "";  // not generic
		out.println("\tLocalVariable\t"+name+" "+desc+" "+signature+" "+makeLabel(start)+" "+makeLabel(end)+" "+index);
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		out.println("\tMaxs\t"+maxStack+" "+maxLocals);
	}

	@Override
	public void visitEnd() {
		out.println("\tEnd");
	}
*/
	
	Frame<BasicValue> frame;
	
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
            variant = FRAME_OBJECT;
        }
		return stack(d, variant, width);		
	}

	private String stack(int d, String variant, int width) {
		int loc = frame.getStackSize() - d*width + frame.getLocals();
		return FRAME+loc+variant;
	}
	
	String local(int n, Type type) {
		String v;  int w = 1;
        switch (type.getSort()) {
        case Type.BOOLEAN:
        case Type.CHAR:
        case Type.BYTE:
        case Type.SHORT:
        case Type.INT:
            v = ".I";
            break;
        case Type.FLOAT:
            v = ".F";
            break;
        case Type.LONG:
            v = ".L";  w = 2;
            break;
        case Type.DOUBLE:
            v = ".D";  w = 2;
            break;
        case Type.ARRAY:
            v = ".A";
            break;
        // case Type.OBJECT:
        default:
            v = ".O";
        }
		int l = n;  //TODO direct type for arguments
		return "_"+n+v;		
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
		if (cst == null) emit(stack(0,OBJECT_PSEUDO_TYPE)+" = null;");
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
		checkIndex(ref, ind);
		emit(stack(2,type)+" = "+arrayIndex(ref,ind,type)+";");
	}

	private void checkIndex(String ref, String ind) {
		emit("if ("+ref+" == null) "+LIB_THROW_NULL_POINTER_EXCEPTION+"();");
		emit("if ("+ind+" < 0 || "+ind+" >= (("+T_ARRAY_HEAD+"*)"+ref+")."+ARRAY_LENGTH+") "+LIB_THROW_ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION+"("+ind+");");
	}
	
	private String arrayIndex(String ref, String ind, Type eType) {
		return "(("+T_ARRAY_+eType.getClassName()+")"+ref+")."+ARRAY_ELEMENTS+"["+ind+"]";
	}

	@Override
	public void and(Type type) {
		dyadic(type," & ");
	}

	@Override
	public void anew(Type type) {
		emit(stack(0,type)+" = "+LIB_NEW+"("+classPointer(type)+");");
	}

	private String classPointer(Type type) {
		return "&"+CLASS_STRUCT_PREFIX+convertClassName(type.getInternalName());
	}

	@Override
	public void areturn(Type type) {
		if (type == Type.VOID_TYPE) emit("return;");
		else emit("return "+stack(1,type)+";");
	}

	@Override
	public void arraylength() {
		String ref = stack(1,FRAME_ARRAY,1);
		checkReference(ref);
		emit(stack(0,Type.INT_TYPE)+" = "+"(("+T_ARRAY_HEAD+"*)"+ref+")."+ARRAY_LENGTH+";");
	}

	@Override
	public void astore(Type type) {
		String ref = stack(3,FRAME_ARRAY,1);
		String ind = stack(2,Type.INT_TYPE);
		checkIndex(ref, ind);
		emit(arrayIndex(ref,ind,type)+" = "+stack(1,type)+";");
	}
	

	@Override
	public void athrow() {
		emit(LIB_THROW+"("+stack(1,OBJECT_PSEUDO_TYPE)+");");
	}

	@Override
	public void cast(Type from, Type to) {
		String f = stack(1,from), t;
        if (from == Type.DOUBLE_TYPE) {
            if (to == Type.FLOAT_TYPE) {
            	t = "("+T_FLOAT+")"+f;
            } else if (to == Type.LONG_TYPE) {
                t = LIB_D2L+"("+f+")";
            } else {
                t = LIB_D2I+"("+f+")";
            }
        } else if (from == Type.FLOAT_TYPE) {
            if (to == Type.DOUBLE_TYPE) {
            	t = "("+T_DOUBLE+")"+f;
            } else if (to == Type.LONG_TYPE) {
                t = LIB_F2L+"("+f+")";
            } else {
                t = LIB_F2I+"("+f+")";
            }
        } else if (from == Type.LONG_TYPE) {
        	if (to == Type.DOUBLE_TYPE) {
            	t = "("+T_DOUBLE+")"+f;
            } else if (to == Type.FLOAT_TYPE) {
            	t = "("+T_FLOAT+")"+f;
            } else {
            	t = "("+T_INT+")"+f;
            }
        } else {
            if (to == Type.BYTE_TYPE) {
            	t = "("+T_BYTE+")"+f;
            } else if (to == Type.CHAR_TYPE) {
            	t = "("+T_CHAR+")"+f;
            } else if (to == Type.DOUBLE_TYPE) {
            	t = "("+T_DOUBLE+")"+f;
            } else if (to == Type.FLOAT_TYPE) {
            	t = "("+T_FLOAT+")"+f;
            } else if (to == Type.LONG_TYPE) {
            	t = "("+T_LONG+")"+f;
            } else if (to == Type.SHORT_TYPE) {
            	t = "("+T_SHORT+")"+f;
            } else return;  // INT to INT
        }
		emit(stack(1,to)+" = "+t+";");
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
		emit(stack(0,Type.DOUBLE_TYPE)+" = "+Double.toString(cst)+";");
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
        String s1 = stack(1,FRAME_ANY,1);
        String s2 = stack(2,FRAME_ANY,1);
		String s0 = stack(0,FRAME_ANY,1);
		String sm = stack(-1,FRAME_ANY,1);
		emit(sm+" = "+s1+";");
		emit(s0+" = "+s2+";");
		emit(s2+" = "+s0+";");
		//TODO does this work for Form 2?
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
		//TODO does this work for Form 2?
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
		//TODO does this work for Forms 2, 3 & 4?
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
        String s1 = stack(1,FRAME_ANY,1);
        String s2 = stack(2,FRAME_ANY,1);
        String s3 = stack(3,FRAME_ANY,1);
		String s0 = stack(0,FRAME_ANY,1);
		emit(s0+" = "+s1+";");
		emit(s1+" = "+s2+";");
		emit(s2+" = "+s3+";");
		emit(s3+" = "+s0+";");
		//TODO does this work for Form 2?
	}

	@Override
	public void fconst(float cst) {
		emit(stack(0,Type.FLOAT_TYPE)+" = "+Float.toString(cst)+"F;");
	}

	@Override
	public void getfield(String owner, String name, String desc) {
		String ref = stack(1,OBJECT_PSEUDO_TYPE);
		checkReference(ref);
		emit(stack(1,Type.getType(desc))+" = "+objectField(ref,owner,name)+";");
	}
	
	private String objectField(String ref, String owner, String name) {
		return "(("+convertClassName(owner)+")"+ref+")->"+name;
	}

	private void checkReference(String ref) {
		emit("if ("+ref+" == null) "+LIB_THROW_NULL_POINTER_EXCEPTION+"();");
	}

	@Override
	public void getstatic(String owner, String name, String desc) {
		emit(stack(0,Type.getType(desc))+" = "+staticFieldName(owner,name)+";");
	}

	private String staticFieldName(String owner, String name) {
		return CLASS_STRUCT_PREFIX+convertClassName(owner)+"."+CLASS_STATIC_FIELDS+"."+escapeName(name);
	}

	public static String convertClassName(String owner) {
		return escapeName(owner);  // was owner.replace('/','_');
	}

	public static String escapeName(String name) {
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
		emit(stack(0,Type.INT_TYPE)+" = "+Integer.toString(cst)+";");
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
		branch(stack(1,OBJECT_PSEUDO_TYPE),"!=","null",target);
	}

	@Override
	public void ifnull(Label target) {
		branch(stack(1,OBJECT_PSEUDO_TYPE),"==","null",target);
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
		invoke(owner, name, true, desc);
		//TODO not yet looking for interfaces
	}

	@Override
	public void invokespecial(String owner, String name, String desc, boolean itf) {
		//TODO interface
		invoke(owner, name, true, desc);
		//TODO this has not yet captured the distinction from INVOKEVIRTUAL https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-6.html#jvms-6.5.invokespecial
	}

	@Override
	public void invokestatic(String owner, String name, String desc, boolean itf) {
		//TODO interface
		invoke(owner, name, false, desc);
	}

	@Override
	public void invokevirtual(String owner, String name, String desc, boolean itf) {
		//TODO interface
		invoke(owner, name, true, desc);
	}

	public static String convertMethodName(String name, String desc) {
		// uniformly using signature on all method names is simpler
		int descHash = desc.hashCode();
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

	private void invoke(String owner, String name, boolean virtual, String desc) {
		Type retType = Type.getReturnType(desc);
		Type[] argTypes = Type.getArgumentTypes(desc);
		int numArgs = argTypes.length;
		int reserve = virtual ? 1 : 0;
		String[] argList = new String[reserve+numArgs];
		int d = 1;
		for (int i = numArgs-1;  i >= 0;  --i) {
			Type argType = argTypes[i];
			argList[reserve+i] = stack(d++,argType);
			if (argType == Type.DOUBLE_TYPE || argType == Type.LONG_TYPE) d++;  // kludgy!!
		}
		StringBuilder call = new StringBuilder();
		String prefix;
		if (virtual) {
			String ref = stack(d,OBJECT_PSEUDO_TYPE);
			argList[0] = ref;
			checkReference(ref);
			prefix = "(("+convertClassName(owner)+")"+ref+")->"+OBJECT_CLASS+"->"+CLASS_METHOD_TABLE+".";
		} else {
			prefix = convertClassName(owner)+"_";
		}
		call.append(prefix).append(convertMethodName(name,desc)).append("(");
		String sep = "";
		for (int i = 0;  i < argList.length;  ++i) {
			call.append(sep);  sep = ",";
			String argVal = argList[i];
			call.append(argVal);
		}
		call.append(")");
		if (retType == Type.VOID_TYPE) {
			emit(call.toString()+";");
		} else {
			emit(stack(d,retType)+" = "+call.toString()+";");
			//TODO stack position calculation is wrong for wide result or any wide arguments!
		}
	}

	@Override
	public void jsr(Label target) {
		unimplemented("jsr "+makeLabel(target));
	}

	@Override
	public void lcmp() {
		emit(stack(2,Type.INT_TYPE)+" = (" +
    			stack(2,Type.LONG_TYPE) + " > " + stack(1,Type.LONG_TYPE) + ") ? 1 : ((" +
    			stack(2,Type.LONG_TYPE) + " == " + stack(1,Type.LONG_TYPE) + ") ? 0 : -1);");
	}

	@Override
	public void lconst(long cst) {
		emit(stack(0,Type.LONG_TYPE)+" = "+Long.toString(cst)+";");
	}

	@Override
	public void load(int lclIndex, Type type) {
		emit(stack(0,type)+" = "+local(lclIndex,type)+";");
	}

	static int switchCount = 0;

	@Override
	public void lookupswitch(Label defalt, int[] matches, Label[] targets) {
		String tableName = "switchTable"+(switchCount++);
		StringBuilder table = new StringBuilder();
		String sep = "";
        for (int i = 0; i < matches.length; i++) {
        	table.append(sep).append("{").append(matches[i]).append(",&&").append(makeLabel(targets[i])).append("}");
        	sep = ",";
        }
		emit("static "+SWITCH_PAIR+" "+tableName+"[] = {"+table+"};");
		emit("goto *"+LIB_LOOKUPSWITCH+"("+stack(1,Type.INT_TYPE)+", "+matches.length+", "+tableName+", &&"+makeLabel(defalt)+");");
	}

	@Override
	public void mark(Label label) {
		emit(makeLabel(label)+": ;");
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
		// TODO 
		unimplemented("multianewarray "+desc+" "+dims);
	}

	@Override
	public void neg(Type type) {
		emit(stack(1,type)+" = "+zero(type)+" - "+stack(1,type)+";");
	}

	@Override
	public void newarray(Type type) {
		String at;
		if (type == Type.BOOLEAN_TYPE) at = T_BOOLEAN;
		else if (type == Type.BYTE_TYPE) at = T_BYTE;
		else if (type == Type.CHAR_TYPE) at = T_CHAR;
		else if (type == Type.DOUBLE_TYPE) at = T_DOUBLE;
		else if (type == Type.FLOAT_TYPE) at = T_FLOAT;
		else if (type == Type.INT_TYPE) at = T_INT;
		else if (type == Type.LONG_TYPE) at = T_LONG;
		else if (type == Type.SHORT_TYPE) at = T_SHORT;
		else { // Object
			emit(stack(1,type)+" = "+LIB_NEW_ARRAY_OBJECT+"("+stack(1,Type.INT_TYPE)+","+classPointer(type)+");");
			return;
		}
		emit(stack(1,type)+" = "+LIB_NEW_ARRAY_+at+"("+stack(1,Type.INT_TYPE)+");");
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
		checkReference(ref);
		emit(objectField(ref,owner,name)+" = "+stack(1,Type.getType(desc))+";");
	}

	@Override
	public void putstatic(String owner, String name, String desc) {
		emit(staticFieldName(owner,name)+" = "+stack(1,Type.getType(desc))+";");
	}

	@Override
	public void rem(Type type) {
		if (type == Type.INT_TYPE) {
			emit(stack(2,type)+" = "+LIB_IREM+"("+stack(2,type)+", "+stack(1,type)+");");
		} else if (type == Type.LONG_TYPE) {
			emit(stack(2,type)+" = "+LIB_LREM+"("+stack(2,type)+", "+stack(1,type)+");");
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
		String trunc = "";
		if (type == Type.BOOLEAN_TYPE) trunc = "("+T_BOOLEAN+")";
		else if (type == Type.BYTE_TYPE) trunc = "("+T_BYTE+")";
		else if (type == Type.CHAR_TYPE) trunc = "("+T_CHAR+")";
		else if (type == Type.SHORT_TYPE) trunc = "("+T_SHORT+")";
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
		emit(FRAME_SWAP+" = "+s1+";");
		emit(s1+" = "+s2+";");
		emit(s2+" = "+FRAME_SWAP+";");
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
		emit("static "+LABEL_PTR+" "+tableName+"[] = {"+table+"};");
		String s = stack(1,Type.INT_TYPE);
		String dflt = makeLabel(defalt);
		emit("if ("+s+" < "+low+") goto "+dflt+";");
		emit("if ("+s+" > "+high+") goto "+dflt+";");
		emit("goto *"+tableName+"["+s+"-"+low+"];");
	}

	@Override
	public void tconst(Type type) {
		// TODO
		emit(stack(0,OBJECT_PSEUDO_TYPE)+" = *TODO*Class:"+type.getInternalName()+";");
	}

	@Override
	public void ushr(Type type) {
		shift(type," >> ",false);
	}

	public static String makeLabel(Label label) {
		return "L"+Integer.toString(System.identityHashCode(label),36);
	}

	@Override
	public void xor(Type type) {
		dyadic(type," ^ ");
	}

	private void unimplemented(String string) {
		emit("UNIMPLEMENTED "+string);
		System.out.println("Unimplemented "+string);
	}
}
