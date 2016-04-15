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
		//System.out.println(System.getProperty("java.boot.class.path"));
		//System.out.println(System.getProperty("sun.boot.class.path"));
	    Main main = new Main();
		main.run(args);
	}

	boolean opt_recurse = false;
	private int numMissing;
	private ClassCompiler classCompiler;

	private void run(final String[] args) throws ZipException, IOException {
		classCompiler = new ClassCompiler();
		for (ListIterator<String> argIter = Arrays.asList(args).listIterator();
				 argIter.hasNext(); ) {
			String arg = argIter.next();
			if (arg.equals("-cp")) {
				classCompiler.setClasspath(argIter.next());
			} else if (arg.equals("-d")) {
				classCompiler.setOutDirectory(argIter.next());
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
	
				try {
					
					classCompiler.compile(arg);
					
					if (opt_recurse) classCompiler.recurse();
					
		        } catch (IOException e) {
		            System.err.println("(" + arg +") " + e.getMessage());
		            numMissing += 1;
		        } catch (ClassNotFoundException e) {
		            System.err.println("(" + arg +") " + e.getMessage());
		            //e.printStackTrace(System.err);
		            numMissing += 1;
				} catch (AnalyzerException e) {
		            System.err.println("Class analysis failed " + e.getMessage());
		            e.printStackTrace(System.err);
		            numMissing += 1;
				}
			}
	   }
		
		System.err.println("Missing="+numMissing);
	}

}