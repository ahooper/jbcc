package ca.nevdull.jbcc3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ClassPath {

	private File[] pathList;
	private ZipFile[] zipList;

	public ClassPath() throws ZipException, IOException {
		this(System.getProperty("java.class.path"));
	}

	public ClassPath(String pathString) throws ZipException, IOException {
		this(pathSplit(pathString));
	}
	
	static Pattern pathPat = Pattern.compile(File.pathSeparator,Pattern.LITERAL);
	private static String[] pathSplit(String arg) {
		return pathPat.split(arg,-1);
	}

	public ClassPath(String[] pathNames) throws ZipException, IOException {
		pathList = new File[pathNames.length];
		zipList = new ZipFile[pathNames.length];
    	for (int i = 0;  i < pathNames.length;  i++) {
    		File file = new File(pathNames[i]);
    		pathList[i] = file;
    		if (file.exists()) {
    			if (file.isFile()) zipList[i] = new ZipFile(file);
    		}
    	}
	}
	
    public InputStream getInputStream(String className) throws IOException, ClassNotFoundException {
    	String name = className.replace('.', '/') + ".class";
    	for (int i = 0;  i < pathList.length;  i++) {
    		if (zipList[i] != null) {
    			ZipEntry entry = zipList[i].getEntry(name);
    			if (entry != null) {
    				//System.out.println("ClassPath getInputStream "+className+" zip "+entry.getSize());
    				return zipList[i].getInputStream(entry);
    			}
    		} else {
    			File file = new File(pathList[i],name);
    			if (file.exists()) {
    				//System.out.println("ClassPath getInputStream "+className+" file "+file.length());
    				return new FileInputStream(file);
    			}
    		}
    	}
    	//System.out.println("ClassPath getInputStream "+className+" not found");
    	throw new ClassNotFoundException("class "+name+" not found");
    }

}
