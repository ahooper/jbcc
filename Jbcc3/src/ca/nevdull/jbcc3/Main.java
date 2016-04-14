package ca.nevdull.jbcc3;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.zip.ZipException;

import org.objectweb.asm.tree.analysis.AnalyzerException;

public class Main {

	public static void main(final String[] args) throws Exception {
	    Main main = new Main();
		main.run(args);
	}

	String opt_directory = null;
	boolean opt_recurse = false;
	private int numMissing;
	private ClassCompiler classCompiler;

	private void run(final String[] args) throws ZipException, IOException {
		classCompiler = new ClassCompiler();
		for (ListIterator<String> argIter = Arrays.asList(args).listIterator();
				 argIter.hasNext(); ) {
			String arg = argIter.next();
			if (arg.equals("-cp")) {
				classCompiler.setClasspath(new ClassPath(argIter.next()));
			} else if (arg.equals("-d")) {
				opt_directory = argIter.next();
			} else if (arg.equals("-o")) {
				String fileName = argIter.next();
				try {
					classCompiler.setOut(new PrintWriter(fileName));
				} catch (FileNotFoundException e) {
					System.err.println(fileName+":"+e.getMessage());
				}
			} else if (arg.equals("-r")) {
				opt_recurse = true;
			} else if (arg.startsWith("-")) {
				System.err.println("Unrecognized option: "+arg);
			} else {
	
				if (classCompiler.getClasspath() == null) classCompiler.setClasspath(new ClassPath(System.getProperty("java.class.path")));
	
				try {
					
					classCompiler.compile(arg);
					
		        } catch (IOException e) {
		            System.err.println("Error loading class " + arg + " (" + e.getMessage() + ")");
		            numMissing += 1;
		        } catch (ClassNotFoundException e) {
		            System.err.println("Class not found " + arg + " (" + e.getMessage() + ")");
		            e.printStackTrace(System.err);
		            numMissing += 1;
				} catch (AnalyzerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	   }
		
		System.err.println("Missing="+numMissing);
	}

}