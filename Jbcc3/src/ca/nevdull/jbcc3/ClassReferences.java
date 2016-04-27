package ca.nevdull.jbcc3;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ClassReferences extends MethodVisitor implements Iterable<String> {

	private PrintWriter out;
	private HashSet<String> classReferences = new HashSet<String>();

	public ClassReferences(PrintWriter out) {
    	super(Opcodes.ASM5);
    	this.out = out;
	}

	public void setOut(PrintWriter out) {
		this.out.flush();
		this.out = out;
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		classReference(owner);
	}
	
    public void visitLdcInsn(final Object cst) {
    	if (cst instanceof Type) {
    		if (Main.opt_debug) System.out.println("    visitLdcInsn "+cst);
    		typeReference((Type)cst);
    	}
    }

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		if (owner.charAt(0) == '[') {
			// TODO special case for array clone
			System.out.println("    invoke "+owner+" "+name+" "+desc);
			arrayReference(owner);
		} else classReference(owner);
		/*
		Type methodType = Type.getMethodType(desc);
		if (Main.opt_debug) System.out.println("    visitMethodInsn "+org.objectweb.asm.util.Printer.OPCODES[opcode]+" "+methodType);
		for (Type argType : methodType.getArgumentTypes()) {
			typeReference(argType);
		}
		typeReference(methodType.getReturnType());
		*/
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		if (Main.opt_debug) System.out.println("    visitTypeInsn "+org.objectweb.asm.util.Printer.OPCODES[opcode]+" "+type);
		if (type.charAt(0) == '[') {
			arrayReference(type);
		} else classReference(type);
	}

	private void arrayReference(String desc) {
		Type t = Type.getType(desc);
		typeReference(t);
	}

	void typeReference(Type type) {
		while (type.getSort() == Type.ARRAY) {
			type = type.getElementType();
		}
		if (type.getSort() == Type.OBJECT) classReference(type.getInternalName());
		else {
			if (Main.opt_debug) System.out.println("    typeReference "+type.getSort()+" "+type);
		}
	}

	void classReference(String owner) {
		if (Main.opt_debug) System.out.println("    classReference "+owner);
		if (owner.charAt(0) == 'L') {
			System.err.println("Reference "+owner);
		}
		if (!classReferences.contains(owner)) {
			out.println("#include \""+ClassCompiler.compiledFileName(owner,ClassCompiler.FILE_SUFFIX_HEADER)+"\"");
			classReferences.add(owner);
		}
	}
	
	public Iterator<String> iterator() {
		return classReferences.iterator();
	}

}
