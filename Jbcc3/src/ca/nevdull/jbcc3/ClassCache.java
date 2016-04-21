package ca.nevdull.jbcc3;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.zip.ZipException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class ClassCache {
	
	HashMap<String,ClassNode> cache = new HashMap<String,ClassNode>();
	private ClassPath classpath;
	
	public ClassCache() throws ZipException, IOException {
		this.classpath = new ClassPath();
	}
	
	public ClassCache(String pathString) throws ZipException, IOException {
		this.classpath = new ClassPath(pathString);
	}

	public void setClasspath(String pathString) throws ZipException, IOException {
		this.classpath = new ClassPath(pathString);
	}

	private ClassNode read(String name) throws IOException, FileNotFoundException, ClassNotFoundException {
		int flags = 0;  //ClassReader.SKIP_DEBUG;
        
		ClassReader cr;
        if (name.endsWith(".class")/* || name.indexOf('\\') > -1
                || name.indexOf('/') > -1*/) {
            cr = new ClassReader(new FileInputStream(name));
        } else {
            cr = new ClassReader(classpath.getInputStream(name));
        }
        ClassNode cn = new ClassNode();
        cr.accept(cn, flags);
        
		return cn;
	}
	
	public boolean contains(String name) {
		return cache.containsKey(name);
	}
	
	public ClassNode get(String name) throws FileNotFoundException, ClassNotFoundException, IOException {
		ClassNode cn = cache.get(name);
		if (cn == null) {
			cn = read(name);
	        cache.put(name, cn);
		}
		return cn;
	}
	
}
