package ca.nevdull.jbcc3;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.zip.ZipException;

import org.objectweb.asm.tree.analysis.AnalyzerException;

public class Main {
	static boolean opt_debug = false;

	public static void main(final String[] args) throws ZipException, IOException  {

		boolean opt_referenced = false;
		int numMissing = 0;
		ClassCompiler classCompiler = new ClassCompiler();
		
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
					System.setOut(new PrintStream(fileName));
				} catch (FileNotFoundException e) {
					System.err.println(arg+" "+fileName+":"+e.getMessage());
				}
			} else if (arg.equals("-r")) {
				opt_referenced = true;
			} else if (arg.equals("-D")) {
				opt_debug = true;
			} else if (arg.startsWith("-")) {
				System.err.println("Unrecognized option: "+arg);
			} else {
	
				try {
					
					classCompiler.compile(arg);
					
					if (opt_referenced) classCompiler.compileReferenced();
					
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