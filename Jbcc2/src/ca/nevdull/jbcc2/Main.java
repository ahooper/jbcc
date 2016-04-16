package ca.nevdull.jbcc2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.bcel6.Const;
import org.apache.commons.bcel6.classfile.Constant;
import org.apache.commons.bcel6.classfile.ConstantClass;
import org.apache.commons.bcel6.classfile.ConstantPool;
import org.apache.commons.bcel6.classfile.ConstantUtf8;
import org.apache.commons.bcel6.classfile.JavaClass;
import org.apache.commons.bcel6.util.ClassPath;
import org.apache.commons.bcel6.util.SyntheticRepository;

public class Main {

	ClassPath opt_classpath = new ClassPath(System.getProperty("java.class.path"));
	public SyntheticRepository repo = SyntheticRepository.getInstance(opt_classpath);
	String opt_directory = null;
	PrintWriter opt_out = new PrintWriter(System.out);
	boolean opt_recurse = false;
	boolean opt_verbose = false;

	static final String FILE_SUFFIX_HEADER = ".h";
	static final String FILE_SUFFIX_CODE = ".c";

	public static void main(String[] args) throws ClassNotFoundException, IOException {
		Main main = new Main();
		main.run(args);
	}
	
	private int numMissing = 0;
	
	public void run(String[] args) throws IOException {
		
		for (ListIterator<String> argIter = Arrays.asList(args).listIterator();
				 argIter.hasNext(); ) {
			String arg = argIter.next();
			if (arg.equals("-cp")) {
				opt_classpath = new ClassPath(argIter.next());
				repo = SyntheticRepository.getInstance(opt_classpath);
			} else if (arg.equals("-d")) {
				opt_directory = argIter.next();
			} else if (arg.equals("-o")) {
				String fileName = argIter.next();
				try {
					opt_out = new PrintWriter(fileName);
				} catch (FileNotFoundException e) {
					System.err.println(fileName+":"+e.getMessage());
				}
			} else if (arg.equals("-r")) {
				opt_recurse = true;
			} else if (arg.equals("-v")) {
				opt_verbose = true;
			} else if (arg.startsWith("-")) {
				System.err.println("Unrecognized option: "+arg);
			} else {

				try {
					
					compileClass(arg);
					
		        } catch (IOException e) {
		            System.err.println("Error loading class " + arg + " (" + e.getMessage() + ")");
		            numMissing += 1;
		        } catch (ClassNotFoundException e) {
		            System.err.println("Class not found " + arg + " (" + e.getMessage() + ")");
		            e.printStackTrace(System.err);
		            numMissing += 1;
				}
        	}
        }
		
		System.err.println("Missing="+numMissing);
		
	}

	public Map<String, CompiledClass> compiledClasses = new HashMap<String, CompiledClass>();

	public CompiledClass compileClass(String name) throws IOException, ClassNotFoundException {
        JavaClass klass = repo.loadClass(name);

        // Compile superclass first
		String superclassName = klass.getSuperclassName();
		if (!superclassName.equals(name)) {
			// The top of the hierarchy, java.lang.Object, is its own superclass
			compileClass(superclassName);
		}

		CompiledClass compiled = compiledClasses.get(name);

        if (compiled != null) {
    		//System.out.println("    "+name);
            return compiled;
        }

        compiled = new CompiledClass(klass, this);
        compiledClasses.put(name, compiled);
		
		System.out.println("---------- "+name);  System.out.flush();
		String fileName = name.replace('.', File.separatorChar);
		compiled.header(new File(opt_directory,fileName+FILE_SUFFIX_HEADER));
        compiled.code(new File(opt_directory,fileName+FILE_SUFFIX_CODE));
        
        if (opt_recurse) {
        	// Compile referenced classes
        	ConstantPool constPool = klass.getConstantPool();
	        int cpsize = constPool.getLength();
	        for (int i = 0;  i < cpsize;  i++) {
	        	Constant c = constPool.getConstant(i);
			    if (c != null && c.getTag() == Const.CONSTANT_Class) {
			        ConstantUtf8 cnb = (ConstantUtf8) constPool.getConstant(((ConstantClass) c).getNameIndex());
			        String cn = cnb.getBytes();
			        if (cn.startsWith("[")) continue;  // skip array references
			        String referencedClass = cn.replace('/', '.');
			        compileClass(referencedClass);
			    }
			}
        }
        
        return compiled;
    }

}
