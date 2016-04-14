package ca.nevdull.jbcc3;

import java.io.PrintWriter;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

public class MethodCompiler /*extends MethodVisitor*/ implements CCode {

	private PrintWriter out;

	private InstructionCompiler insnCompiler;
	private InstructionCommenter insnCommenter;
	private StringConstants stringCons;
	
	public MethodCompiler(PrintWriter out) {
    	/*super(Opcodes.ASM5);*/
		this.out = out;
		stringCons = new StringConstants(out);
		insnCompiler = new InstructionCompiler(out, stringCons);
		insnCommenter = new InstructionCommenter(out);
	}

	public void setOut(PrintWriter out) {
		this.out.flush();
		this.out = out;
		insnCompiler.setOut(out);
		insnCommenter.setOut(out);
		stringCons.setOut(out);
	}

	void compile(ClassNode classNode, List<MethodNode> methods) throws AnalyzerException {
		// Collect all string constants
		for (int mx = 0; mx < methods.size(); ++mx) {
            MethodNode method = methods.get(mx);
            method.accept(stringCons);
        }
		// Compile all methods
		for (int mx = 0; mx < methods.size(); ++mx) {
            MethodNode method = methods.get(mx);
            compile(classNode, method);
        }
        out.flush();
	}

	private void compile(ClassNode classNode, MethodNode method) throws AnalyzerException {
		out.println("\n//--// "+method.name+" "+method.desc);
        List<String> exceptions = method.exceptions;
        if (exceptions != null && exceptions.size() > 0) {
        	out.print("\t// exceptions");
        	for (String exc : exceptions) {
        		out.print(" ");
        		out.print(exc);
        	}
        	out.println();
        }
        
        if ((method.access & Opcodes.ACC_NATIVE) != 0) return;
        if ((method.access & Opcodes.ACC_ABSTRACT) != 0) return;  // does this occur?

		Type[] argTypes = Type.getArgumentTypes(method.desc);
		Type returnType = Type.getReturnType(method.desc);
        boolean virtual = (method.access & Opcodes.ACC_STATIC) == 0;
		
        out.print(convertType(returnType));
		out.print(" ");
		String convClassName = InstructionCompiler.convertClassName(classNode.name);
		out.print(convClassName+"_"+InstructionCompiler.convertMethodName(method.name,method.desc));
		out.print("(");

		String sep = "";
		if (virtual) {
        	out.print(convClassName);
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
		    	Frame<BasicValue> frame = frames[fx];
		    	AbstractInsnNode insn = ins.get(fx);
		    	insn.accept(insnCommenter);  
				//out.println(frame);
				insnCompiler.setFrame(frame);
				insn.accept(insnCompiler);
		    }
		}
		
        out.println("}");
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
        	return InstructionCompiler.convertClassName(type.getInternalName());
        default:
    		throw new RuntimeException("Unexpected convertType "+type);
        }
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
		out.println("\tTryCatchBlock\t"+InstructionCompiler.makeLabel(start)+" "+InstructionCompiler.makeLabel(end)+" "+InstructionCompiler.makeLabel(handler)+" "+type);
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		if (signature==null) signature = "";  // not generic
		out.println("\tLocalVariable\t"+name+" "+desc+" "+signature+" "+InstructionCompiler.makeLabel(start)+" "+InstructionCompiler.makeLabel(end)+" "+index);
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
}
