package ca.nevdull.jbcc3;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public class ClassCompiler extends ClassVisitor implements Opcodes, CCode {

	private ClassPath classpath;

	private PrintWriter out;
	private MethodCompiler methodCompiler;
	
	public ClassCompiler() {
    	super(Opcodes.ASM5);
		this.out = new PrintWriter(System.out);
        methodCompiler = new MethodCompiler(out);
	}

	public void setClasspath(ClassPath classpath) {
		this.classpath = classpath;
	}

	public ClassPath getClasspath() {
		return classpath;
	}

	public void setOut(PrintWriter out) {
		this.out.flush();
		this.out = out;
		methodCompiler.setOut(out);
	}

	void compile(String name)
			throws IOException, AnalyzerException, ClassNotFoundException {
        int flags = 0;  //ClassReader.SKIP_DEBUG;
        
		ClassReader cr;
        if (name.endsWith(".class") || name.indexOf('\\') > -1
                || name.indexOf('/') > -1) {
            cr = new ClassReader(new FileInputStream(name));
        } else {
            cr = new ClassReader(classpath.getInputStream(name));
        }
        ClassNode cn = new ClassNode();
        cr.accept(cn, flags);
        
        compile(cn);
	}

	private void compile(ClassNode cn) throws AnalyzerException {
		cn.accept(this);
		methodCompiler.compile(cn, cn.methods);
        out.flush();
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (signature==null) signature = "";  // not generic
		out.println("Class v"+version+" "+accessToString(access)+" "+name+" "+signature+" extends "+superName);
		for (String ifc : interfaces) {
			out.println("interface "+ifc);
		}
	}

	static String accessToString(int access) {
		StringBuilder s = new StringBuilder();
		if ((access & ACC_PUBLIC) != 0)		s.append("Pb");
		if ((access & ACC_PRIVATE) != 0)	s.append("Pv");
		if ((access & ACC_PROTECTED) != 0)	s.append("Pt");
		if ((access & ACC_STATIC) != 0)		s.append("St");
		if ((access & ACC_FINAL) != 0)		s.append("Fn");
		if ((access & ACC_SUPER) != 0)		s.append("Su");
		if ((access & ACC_SYNCHRONIZED) != 0)	s.append("Sy");
		if ((access & ACC_VOLATILE) != 0)	s.append("Vo");
		if ((access & ACC_BRIDGE) != 0)		s.append("Br");
		if ((access & ACC_VARARGS) != 0)	s.append("Va");
		if ((access & ACC_TRANSIENT) != 0)	s.append("Tr");
		if ((access & ACC_NATIVE) != 0)		s.append("Na");
		if ((access & ACC_INTERFACE) != 0)	s.append("In");
		if ((access & ACC_ABSTRACT) != 0)	s.append("Ab");
		if ((access & ACC_STRICT) != 0)		s.append("Sr");
		if ((access & ACC_SYNTHETIC) != 0)	s.append("Sy");
		if ((access & ACC_ANNOTATION) != 0)	s.append("An");
		if ((access & ACC_ENUM) != 0)		s.append("En");
		if ((access & ACC_MANDATED) != 0)	s.append("Md");
		if ((access & ACC_DEPRECATED) != 0)	s.append("Dp");
		return s.toString();
	}

	@Override
	public void visitAttribute(Attribute attr) {
		out.println("Attribute\t"+attr.type);
	}

	@Override
	public void visitSource(String source, String debug) {
		out.println("Source\t"+source+" debug "+debug);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		out.println("Annotation\t"+desc+" visible "+visible);
		return null;
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		out.println("Type annotation\t"+typeRef+" "+typePath+" "+desc+" visible "+visible);
		return null;
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		out.println("Inner class\t"+name+" "+outerName+" "+innerName+" "+accessToString(access));
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		out.println("Outer class\t"+owner+" "+name+" "+desc);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if (signature==null) signature = "";  // not generic
		out.println("Field\t"+accessToString(access)+"\t"+name+" "+desc+" "+signature+" value "+value);
		return null;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (signature==null) signature = "";  // not generic
		out.println("Method\t"+accessToString(access)+"\t"+name+" "+desc+" "+signature);
		for (String exc : exceptions) {
			out.println("\t\tException "+exc);
		}
		return null;
	}

	@Override
	public void visitEnd() {
		//super.visitEnd();
		out.println("End\n");
	}

}
