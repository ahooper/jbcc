package ca.nevdull.jbcc3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.zip.ZipException;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public class ClassCompiler /*extends ClassVisitor*/ implements Opcodes, CCode {

	private ClassCache classCache;

	private PrintWriter out;
	private File outDirectory;
	private MethodCompiler methodCompiler;
	
	static final String FILE_SUFFIX_HEADER = ".h";
	static final String FILE_SUFFIX_CODE = ".c";

	public ClassCompiler() throws ZipException, IOException {
    	/*super(Opcodes.ASM5);*/
		this.out = new PrintWriter(System.out);
		this.classCache = new ClassCache();
        methodCompiler = new MethodCompiler(out);
	}

	public void setClasspath(String pathString) throws ZipException, IOException {
		classCache.setClasspath(pathString);
	}
	
	public void setOutDirectory(String outDirectory) {
		this.outDirectory = new File(outDirectory);
	}

	public void setOut(PrintWriter out) {
		this.out.flush();
		this.out = out;
		methodCompiler.setOut(out);
	}

	private void setOut(File file) throws FileNotFoundException {
		File parent = file.getParentFile();
		if (parent != null) parent.mkdirs();
		setOut(new PrintWriter(file));
	}

	ClassNode compile(String name)
			throws IOException, AnalyzerException, ClassNotFoundException {
		
        ClassNode cn = classCache.get(name);
        
        compile(cn);
        
        return cn;
	}
	
	private ArrayDeque<String> classQueue = new ArrayDeque<String>();

	public void recurse() throws ClassNotFoundException, IOException, AnalyzerException {
		for (String name = classQueue.poll();  name != null;  name = classQueue.poll()) {
	        compile(name);		
		}
	}

	private void compile(ClassNode cn) throws AnalyzerException, FileNotFoundException {
		System.out.println(cn.name);
		
		String convClassName = MethodCompiler.convertClassName(cn.name);
		
		// Produce class include header

		setOut(new File(outDirectory,compiledFileName(cn.name,FILE_SUFFIX_HEADER)));
		
		out.println("#ifndef H_"+convClassName);
		out.println("#define H_"+convClassName);
		if (cn.superName != null) {
			out.println("#include \""+compiledFileName(cn.superName,FILE_SUFFIX_HEADER)+"\"");
		}
        out.println("typedef struct "+OBJECT_STRUCT_PREFIX+convClassName+" *"+convClassName+";");
        out.println("typedef struct {");
		out.println("    "+T_ARRAY_HEAD+" H;");
		out.println("    "+convClassName+" "+ARRAY_ELEMENTS+"[];");
        out.println("} *"+T_ARRAY_+convClassName+";");

		//TODO Interfaces

		ClassReferences references = new ClassReferences(out);
		for (int mx = 0; mx < cn.methods.size(); ++mx) {
            MethodNode method = cn.methods.get(mx);
            methodCompiler.compileClassReferences(method,references);
            declareMethod(method, convClassName);
        }
		for (String ref : references) {
			if (!classCache.contains(ref)) classQueue.add(ref);
		}
		
		/*
		cn.accept(this);
		*/
        
        // virtual method table
        out.print("struct "+METHOD_STRUCT_PREFIX);
        out.print(convClassName);
        out.println(" {");
		for (int mx = 0; mx < cn.methods.size(); ++mx) {
            MethodNode method = cn.methods.get(mx);
			 //TODO superclass methods that are not overridden
        	if ((method.access & Opcodes.ACC_STATIC) == 0) {
        		out.print("    ");
        		declareMethodPointer(method, convClassName);
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
		for (int fx = 0; fx < cn.fields.size(); ++fx) {
            FieldNode field = cn.fields.get(fx);
        	if ((field.access & Opcodes.ACC_STATIC) != 0) {
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
		for (int fx = 0; fx < cn.fields.size(); ++fx) {
            FieldNode field = cn.fields.get(fx);
        	if ((field.access & Opcodes.ACC_STATIC) == 0) {
        		out.print("    ");
        		declareField(field);
        	}
        }
        out.println("};");
        //TODO inherited fields
        
		out.println("#endif /*H_"+convClassName+"*/");
		
		// Produce class implementation
		
        setOut(new File(outDirectory,compiledFileName(cn.name,FILE_SUFFIX_CODE)));
		
        out.println("#include \""+LIB_H+"\"");
		out.println("#include \""+compiledFileName(cn.name,FILE_SUFFIX_HEADER)+"\"");
		
		// Collect all string constants
		for (int mx = 0; mx < cn.methods.size(); ++mx) {
            MethodNode method = cn.methods.get(mx);
            methodCompiler.compileStrings(method);
        }
		
		// Code for all methods
		for (int mx = 0; mx < cn.methods.size(); ++mx) {
            MethodNode method = cn.methods.get(mx);
            methodCompiler.compileCode(cn, method);
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
		 out.print(cn.name);
		 out.println("\" },");
		 out.print("    /*struct "+METHOD_STRUCT_PREFIX);
		 out.print(convClassName);
		 out.println("*/ {");
		 for (int mx = 0; mx < cn.methods.size(); ++mx) {
			 MethodNode method = cn.methods.get(mx);
			 //TODO superclass methods that are not overridden
			 if ((method.access & Opcodes.ACC_STATIC) == 0) {
				 out.print("        ");
				 out.print(MethodCompiler.externalMethodName(convClassName, method));
				 out.println(",");
			}
		 }
		 out.println("    }");
		 out.println("};");
		
        out.flush();
	}

	static String compiledFileName(String owner, String suffix) {
		StringBuilder fileName = new StringBuilder();
		int w = 0;
		for (int x; (x = owner.indexOf('/',w)) >= 0;  w = x+1) {
			fileName.append(MethodCompiler.escapeName(owner.substring(w,x))).append(File.separatorChar);
		}
		fileName.append(MethodCompiler.escapeName(owner.substring(w)));
		return fileName.append(suffix).toString();
	}
    
	private void declareMethod(MethodNode method, String convClassName) {
		if ((method.access & Opcodes.ACC_NATIVE) != 0) out.print("/*NATIVE*/ ");
		putMethodPrototype(MethodCompiler.externalMethodName(convClassName, method), method, convClassName);
	}
    
	private void declareMethodPointer(MethodNode method, String thisType) {
		putMethodPrototype("(*"+MethodCompiler.convertMethodName(method.name,method.desc)+")", method, thisType);
	}

	private void putMethodPrototype(String name, MethodNode method, String thisType) {
		out.print(MethodCompiler.convertType(Type.getReturnType(method.desc)));
		out.print(" ");
		out.print(name);
		out.print("(");
		String sep = "";
    	if ((method.access & Opcodes.ACC_STATIC) == 0) {
			out.print(thisType);
			sep = ",";
		}
		for (Type argType : Type.getArgumentTypes(method.desc)) {
			out.print(sep);  sep = ",";
			out.print(MethodCompiler.convertType(argType));
		}
		out.println(");");
	}

	private void declareField(FieldNode field) {
		out.print(MethodCompiler.convertType(field.desc));
		out.print(" ");
		out.print(MethodCompiler.escapeName(field.name));			
		out.println(";");
	}

/*
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (signature==null) signature = "";  // not generic
		out.println("Class v"+version+" "+accessToString(access)+" "+name+" "+signature+" extends "+superName);
		for (String ifc : interfaces) {
			out.println("Interface "+ifc);
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
		out.println("End\n");
	}
*/
}
