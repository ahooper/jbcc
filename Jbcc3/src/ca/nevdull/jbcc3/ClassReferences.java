package ca.nevdull.jbcc3;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ClassReferences extends MethodVisitor implements CCode, Iterable<String> {

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

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		if (owner.charAt(0) == '[') {
			// TODO special case for array clone
			System.err.println("invoke "+owner+" "+name+" "+desc);
			Type elemType = Type.getType(owner).getElementType();
			if (elemType.getSort() == Type.OBJECT) classReference(elemType.getInternalName());
			return;
		}
		classReference(owner);
	}

	private void classReference(String owner) {
		if (owner.charAt(0) == 'L') {
			System.err.println("Reference "+owner);
		}
		if (!classReferences.contains(owner)) {
	        String crcn = MethodCompiler.convertClassName(owner);
	        //out.println("typedef struct "+OBJECT_STRUCT_PREFIX+crcn+" *"+crcn+";");
			out.println("#include \""+ClassCompiler.compiledFileName(owner,ClassCompiler.FILE_SUFFIX_HEADER)+"\"");
			classReferences.add(owner);
		}
	}
	
	public Iterator<String> iterator() {
		return classReferences.iterator();
	}

}
