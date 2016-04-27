package ca.nevdull.jbcc3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipException;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public class ClassCompiler /*extends ClassVisitor*/ implements Opcodes, CCode {

	private PrintWriter out;
	private File outDirectory;
	private MethodCompiler methodCompiler;
	private ClassCache classCache;
	
	static final String FILE_SUFFIX_HEADER = ".h";
	static final String FILE_SUFFIX_CODE = ".c";

	public ClassCompiler() throws ZipException, IOException {
    	/*super(Opcodes.ASM5);*/
		classCache = new ClassCache();
        methodCompiler = new MethodCompiler(classCache);
	}

	void setClasspath(String pathString) throws ZipException, IOException {
		classCache.setClasspath(pathString);
	}
	
	void setOutDirectory(String outDirectory) {
		this.outDirectory = new File(outDirectory);
	}

	void setOut(PrintWriter out) {
		if (this.out != null) this.out.close();
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
	
	private HashSet<String> compiled = new HashSet<String>();
	private ArrayDeque<String> classQueue = new ArrayDeque<String>();

	void compileReferenced() throws ClassNotFoundException, IOException, AnalyzerException {
		for (String name = classQueue.poll();  name != null;  name = classQueue.poll()) {
	        compile(name);		
		}
	}

	private void compile(ClassNode cn) throws AnalyzerException, ClassNotFoundException, IOException {
		if (compiled.contains(cn.name)) return;
		System.out.println(cn.name);
		compiled.add(cn.name);
		
		String convClassName = MethodCompiler.convertClassName(cn.name);
        typeReferences.clear();
        classInfoStrings.clear();
		
		// Produce class include header

		setOut(new File(outDirectory,compiledFileName(cn.name,FILE_SUFFIX_HEADER)));
		
		out.println("#ifndef H_"+convClassName);
		out.println("#define H_"+convClassName);
		if (cn.superName != null) {
			out.println("#include \""+compiledFileName(cn.superName,FILE_SUFFIX_HEADER)+"\"");
		}
        referenceTypeClass(cn.name);

        // class references
/*
		for (int ix = 0; ix < cn.interfaces.size(); ++ix) {
            String intfc = cn.interfaces.get(ix);
            referenceClass(intfc);
		}
*/
		for (int fx = 0; fx < cn.fields.size(); ++fx) {
            FieldNode field = cn.fields.get(fx);
            referenceType(Type.getType(field.desc));
		}
		for (int mx = 0; mx < cn.methods.size(); ++mx) {
            MethodNode method = cn.methods.get(mx);
    		for (Type argType : Type.getArgumentTypes(method.desc)) {
    			referenceType(argType);
    		}
			referenceType(Type.getReturnType(method.desc));
            //if (   /*(method.access & Opcodes.ACC_STATIC) != 0
            //	&& */(method.access & Opcodes.ACC_PRIVATE) == 0) {
        	// inheriting classes need all the method declarations to complete their
			// tables, even PRIVATE methods
        	// these could conceivably go in a separate include file only children would use
        	declareMethod(cn.name, method);
        }
		referenceInterfaceTypes(cn);  // to generate typedefs
		
		/*
		cn.accept(this);
		*/
		
        // virtual method table
        out.println("struct "+METHOD_STRUCT_PREFIX+convClassName+" {");
        //##LinkedHashMap<String, String> virtualMethodTable = inheritedMethodTable(cn,false/*fromInterface*/,false/*silent*/);
        LinkedHashMap<String,String> methodPrototypes = new LinkedHashMap<String,String>();
	    LinkedHashMap<String,String> methodInstances = new LinkedHashMap<String,String>();
		collectInheritedMethods(methodPrototypes, methodInstances, cn, true/*followSuperclass*/);
		for (String nameAndDesc : methodPrototypes.keySet()) {
			out.println("    "+methodPrototypes.get(nameAndDesc));
		}
        out.println("};");

        //##// interface list
        //##LinkedHashMap<String,LinkedHashMap<String, String>> interfacesList = new LinkedHashMap<String,LinkedHashMap<String, String>>();
        //##for (int ix = 0; ix < cn.interfaces.size(); ++ix) {
        //##    String intfc = cn.interfaces.get(ix);
        //##    String convIntfcName = MethodCompiler.convertClassName(intfc);
        //##	LinkedHashMap<String, String> interfaceMethodTable = inheritedInterfaceTable(cn,intfc);
        //##	interfacesList.put(convIntfcName, interfaceMethodTable);
        //##}
		
		// inherited static methods
		LinkedHashSet<String> inheritedStaticMethods = new LinkedHashSet<String>();
		for (ClassNode sup = cn; sup.superName != null; ) {
			sup = classCache.get(sup.superName);
			for (int mx = 0; mx < sup.methods.size(); ++mx) {
			    MethodNode method = sup.methods.get(mx);
				if ((method.access & Opcodes.ACC_STATIC) != 0) {
			        String nameAndDesc = method.name + method.desc;
			        if (!inheritedStaticMethods.contains(nameAndDesc)) {
				        String extName = MethodCompiler.externalMethodName(cn.name, method);
				        String supName = MethodCompiler.externalMethodName(sup.name, method);
				        out.println("#define "+extName+" "+supName);
				        // could also do this as a map for MethodCompiler
			        	inheritedStaticMethods.add(nameAndDesc);
			        }
				}
			}
		}
        
        // class structure
        out.println("extern struct "+CLASS_STRUCT_PREFIX+convClassName+" {");
        out.println("    "+OBJECT_CLASS_TYPE+" "+CLASS_CLASS+";");
        out.println("    struct "+METHOD_STRUCT_PREFIX+convClassName+" "+CLASS_METHOD_TABLE+";");
        out.println("    struct {");
		for (int fx = 0; fx < cn.fields.size(); ++fx) {
            FieldNode field = cn.fields.get(fx);
        	if ((field.access & Opcodes.ACC_STATIC) != 0) {
        		out.print("    ");
        		declareField(field);
        	}
        }
        out.println("    } "+CLASS_STATIC_FIELDS+";");
        out.println("} "+CLASS_STRUCT_PREFIX+convClassName+";");
        
		out.println("extern struct "+CLASS_STRUCT_PREFIX+T_ARRAY_+convClassName+" {");
        out.println("    "+OBJECT_CLASS_TYPE+" "+CLASS_CLASS+";");
        out.println("    "+ARRAY_METHODS_TYPE+" "+CLASS_METHOD_TABLE+";");
		out.println("} "+CLASS_STRUCT_PREFIX+T_ARRAY_+convClassName+";");
        
        // instance field declarations
        out.println("struct "+OBJECT_STRUCT_PREFIX+convClassName+" {");
        out.println("    "+OBJECT_HEAD);
        out.println("    struct "+CLASS_STRUCT_PREFIX+convClassName+" *"+OBJECT_CLASS+";");
        ListIterator<String> ftIter = inheritedFieldsTable(cn);
		while (ftIter.hasPrevious()) {
    		out.print("    ");
			out.println(ftIter.previous());
		}
        out.println("};");
        out.println("struct "+ARRAY_STRUCT_PREFIX+convClassName+"{");
        out.println("    "+OBJECT_HEAD);
        out.println("    struct "+CLASS_STRUCT_PREFIX+T_ARRAY_+convClassName+" *"+OBJECT_CLASS+";");
        out.println("    "+ARRAY_HEAD);
		out.println("    "+convClassName+" "+ARRAY_ELEMENTS+"[];");
        out.println("};");
        
		out.println("#endif /*H_"+convClassName+"*/");
		
		// Produce class implementation
		
        setOut(new File(outDirectory,compiledFileName(cn.name,FILE_SUFFIX_CODE)));
		
        out.println("#include \""+LIB_H+"\"");
		out.println("#include \""+compiledFileName(cn.name,FILE_SUFFIX_HEADER)+"\"");
		
		// forward declarations for private static methods
		//NOT NEEDED: inheriting classes need all methods to be extern
		//for (int mx = 0; mx < cn.methods.size(); ++mx) {
        //    MethodNode method = cn.methods.get(mx);
        //    if ((method.access & Opcodes.ACC_STATIC) != 0
        //    	&& (method.access & Opcodes.ACC_PRIVATE) != 0) {
        //		out.print("static ");
        //		putMethodPrototype(MethodCompiler.externalMethodName(cn.name, method), method, convClassName);
        //    }
        //}
		
		// collect all class references
		ClassReferences references = new ClassReferences(out);
		referenceRequired(cn, references);  // to generate includes
		for (int mx = 0; mx < cn.methods.size(); ++mx) {
            MethodNode method = cn.methods.get(mx);
            methodCompiler.compileClassReferences(method,references);
        }
		
		// collect all string constants
		for (int mx = 0; mx < cn.methods.size(); ++mx) {
            MethodNode method = cn.methods.get(mx);
            methodCompiler.compileStrings(method);
        }
		
		// code for all methods
		for (int mx = 0; mx < cn.methods.size(); ++mx) {
            MethodNode method = cn.methods.get(mx);
            methodCompiler.compileCode(cn, method);
		}

		// interface method tables
		// no class-specific interface static fields, I think
    	boolean classIsInterface = (cn.access & Opcodes.ACC_INTERFACE) != 0;
    	//##if (classIsInterface) {
    	//##	// an interface implements its own interface
    	//##	// this will be a duplicate of the class method table with the exception of java.lang.Object methods
    	//##	out.println("static struct "+METHOD_STRUCT_PREFIX+convClassName+" "+METHOD_STRUCT_PREFIX+convClassName+"_"+convClassName+" = {");
    	//##	for (Entry<String, String> ent : virtualMethodTable.entrySet()) {
    	//##		out.println("        "+ent.getValue()+",");
    	//##	}
    	//##	out.println("};");
    	//##}
    	//##for (Entry<String, LinkedHashMap<String, String>> intfcEnt : interfacesList.entrySet()) {
    	//##	String convIntfcName = intfcEnt.getKey();
    	//##	out.println("static struct "+METHOD_STRUCT_PREFIX+convIntfcName+" "+METHOD_STRUCT_PREFIX+convClassName+"_"+convIntfcName+" = {");
    	//##	for (Entry<String, String> ent : intfcEnt.getValue().entrySet()) {
    	//##		out.println("    "+ent.getValue()+",");
    	//##	}
    	//##	out.println("};");
    	//##}
		if (classIsInterface) {
        	out.println("static struct "+METHOD_STRUCT_PREFIX+convClassName+" "+METHOD_STRUCT_PREFIX+convClassName+"_"+convClassName+" = {");
		    LinkedHashMap<String,String> intfcPrototypes = new LinkedHashMap<String,String>();
		    LinkedHashMap<String,String> intfcInstances = new LinkedHashMap<String,String>();
			collectInterfaceMethods(intfcPrototypes, intfcInstances, cn, cn);
			for (String nameAndDesc : intfcPrototypes.keySet()) {
				out.println("    "+intfcInstances.get(nameAndDesc)+",");
			}
	    	out.println("};");
		}
		for (int ix = 0; ix < cn.interfaces.size(); ++ix) {
            String intfc = cn.interfaces.get(ix);
		    ClassNode intfcNode = classCache.get(intfc);
        	String convIntfcName = MethodCompiler.convertClassName(intfc);
        	out.println("static struct "+METHOD_STRUCT_PREFIX+convIntfcName+" "+METHOD_STRUCT_PREFIX+convClassName+"_"+convIntfcName+" = {");
		    LinkedHashMap<String,String> intfcPrototypes = new LinkedHashMap<String,String>();
		    LinkedHashMap<String,String> intfcInstances = new LinkedHashMap<String,String>();
			collectInterfaceMethods(intfcPrototypes, intfcInstances, cn, intfcNode);
			for (String nameAndDesc : intfcPrototypes.keySet()) {
				out.println("    "+intfcInstances.get(nameAndDesc)+",");
			}
	    	out.println("};");
		}
		out.println("static struct Interface_List_Entry "+CLASS_INTERFACES_PREFIX+convClassName+"[] = {");
		if (classIsInterface) {
			// an interface implements its own interface
        	out.println("    {&"+CLASS_STRUCT_PREFIX+convClassName+", &"+METHOD_STRUCT_PREFIX+convClassName+"_"+convClassName+"},");
    	}
		for (int ix = 0; ix < cn.interfaces.size(); ++ix) {
            String intfc = cn.interfaces.get(ix);
            while (intfc != null) {
            	if (intfc.equals("java/lang/Object")) break;  // don't need this as an interface
            	String convIntfcName = MethodCompiler.convertClassName(intfc);
            	out.println("    {&"+CLASS_STRUCT_PREFIX+convIntfcName+", &"+METHOD_STRUCT_PREFIX+convClassName+"_"+convIntfcName+"},");
            	intfc = classCache.get(intfc).superName;
            }
        }
    	out.println("    {0, 0}");  // list terminator
		out.println("};");

		// static string value arrays
		HashSet<String> staticStringValue = new HashSet<String>();
		for (int fx = 0; fx < cn.fields.size(); ++fx) {
			FieldNode field = cn.fields.get(fx);
			if ((field.access & Opcodes.ACC_STATIC) != 0) {
				Object iv = field.value;
				if (iv != null && iv instanceof String) {
					String siv = (String)iv;
					if (!staticStringValue.contains(iv)) {  // avoid duplicates
						putStaticStringValue(siv);
						staticStringValue.add(siv);
					}
				}
			}
		}
		
		// class reflection information : class, fields, interfaces, methods
		// IDEA: could just write the class file data, with the method code attributes deleted
		out.println("#ifdef "+OPT_CLASS_INFO);
		ArrayList<String> classInfo = new ArrayList<String>();
		classInfo.add("static "+CLASS_INFO_TYPE+" info = {");
		classInfo.add("    .access_flags = 0x"+Integer.toHexString(cn.access)+",");
	    classInfo.add("    .this_class = &"+putClassInfoString(cn.name)+","); // TODO direct class data pointer
	    if (cn.superName != null) {
	    	classInfo.add("    .super_class = &"+putClassInfoString(cn.superName)+","); // TODO direct class data pointer
	    } else {
	    	classInfo.add("    .super_class = 0,");
	    }
		classInfo.add("    .interfaces_count = "+cn.interfaces.size()+",");
		classInfo.add("    .interfaces = &"+putClassInterfaces(cn.interfaces)+",");
		classInfo.add("    .fields_count = "+cn.fields.size()+",");
		classInfo.add("    .fields = &"+putClassFields(cn.fields)+",");
		classInfo.add("    .methods_count = "+cn.methods.size()+",");
		classInfo.add("    .methods = &"+putClassMethods(cn.methods)+",");
		if (cn.attrs != null) {
			classInfo.add("    .attributes_count = "+cn.attrs.size()+",");
			classInfo.add("    .attributes = &"+putClassAttributes(cn.attrs)+",");
		} else {
			classInfo.add("    .attributes_count = 0,");
			classInfo.add("    .attributes = 0,");
		}
		classInfo.add("};");
		for (String ci : classInfo) {
			out.println(ci);
		}
		out.println("#endif /*"+OPT_CLASS_INFO+"*/");
        
		// class structure
		out.println("struct "+CLASS_STRUCT_PREFIX+convClassName+" "+CLASS_STRUCT_PREFIX+convClassName+" = {");
		out.println("    ."+CLASS_CLASS+" = { /*struct Class*/");
		out.println("        ."+CLASS_OBJ_SIZE+" = sizeof(struct "+OBJECT_STRUCT_PREFIX+convClassName+"),");
		out.println("        ."+CLASS_INTERFACES+" = "+CLASS_INTERFACES_PREFIX+convClassName+",");
		out.println("        ."+CLASS_NAME+" = \""+cn.name.replace('/','.')+"\",");
		//TODO class_info link
		for (int mx = 0; mx < cn.methods.size(); ++mx) {
            MethodNode method = cn.methods.get(mx);
            if (method.name.equals("<clinit>") 
            	&& method.desc.equals("()V")
            	&& (method.access & Opcodes.ACC_STATIC) != 0) {
        		out.println("        ."+CLASS_CLASS_INIT+" = &"+MethodCompiler.externalMethodName(cn.name,method)+",");
            	break;
            }
		}
		out.println("        ."+CLASS_INITIALIZED+" = 0 },");
		out.println("    ."+CLASS_METHOD_TABLE+" = { /*struct "+METHOD_STRUCT_PREFIX+convClassName+"*/");
		//##for (Entry<String, String> ent : virtualMethodTable.entrySet()) {
		//##	out.println("        "+ent.getValue()+",");
		//##}
		for (String nameAndDesc : methodPrototypes.keySet()) {
			out.println("        "+methodInstances.get(nameAndDesc)+",");
		}
		out.println("    },");
		out.println("    ."+CLASS_STATIC_FIELDS+" = {");
		for (int fx = 0; fx < cn.fields.size(); ++fx) {
            FieldNode field = cn.fields.get(fx);
        	if ((field.access & Opcodes.ACC_STATIC) != 0) {
        		out.print("        ."+MethodCompiler.escapeName(field.name)+" = ");
        		Object iv = field.value;
        		if (iv == null) out.print("0");
        		else if (iv instanceof Integer) out.print(MethodCompiler.intLiteral(((Integer) iv).intValue()));
        		else if (iv instanceof Float) out.print(MethodCompiler.floatLiteral(((Float) iv).floatValue()));
        		else if (iv instanceof Long) out.print(MethodCompiler.longLiteral(((Long) iv).longValue()));
        		else if (iv instanceof Double) out.print(MethodCompiler.doubleLiteral(((Double) iv).doubleValue()));
        		else if (iv instanceof String) putStaticString(iv.toString());
        		else throw new RuntimeException("Unexpected static value type "+iv.getClass().getName());
        		out.println(",");
        	}
        }
		out.println("    }");
		out.println("};");
		
		out.println("struct "+CLASS_STRUCT_PREFIX+T_ARRAY_+convClassName+" "+CLASS_STRUCT_PREFIX+T_ARRAY_+convClassName+" = {");
		out.println("    ."+CLASS_CLASS+" = { /*struct Class*/");
		out.println("        ."+CLASS_OBJ_SIZE+" = sizeof(struct "+ARRAY_STRUCT_PREFIX+convClassName+"),");
		out.println("        ."+CLASS_INTERFACES+" = "+ARRAY_INTERFACES+",");
		out.println("        ."+CLASS_NAME+" = \""+cn.name.replace('/','.')+"[]\",");
		out.println("        ."+CLASS_INITIALIZED+" = 1 },");  // no <clinit> for arrays
		out.println("    ."+CLASS_METHOD_TABLE+" = { /*struct "+METHOD_STRUCT_PREFIX+convClassName+"*/");
		out.println("        "+ARRAY_METHODS);
		out.println("    },");
		out.println("};");
		
		out.close();  out = null;
		 
		// Add referenced classes to list for compilation
		
		for (String ref : references) {
			if (!compiled.contains(ref)) classQueue.add(ref);
		}
		for (int ix = 0; ix < cn.interfaces.size(); ++ix) {
            String intfc = cn.interfaces.get(ix);
			if (!compiled.contains(intfc)) classQueue.add(intfc);
		}
		if (cn.superName != null && !compiled.contains(cn.superName)) classQueue.add(cn.superName);

	}

	private void putStaticStringValue(String string) {
		String stringID = staticStringID(string);
		
		out.println("struct Char_array "+stringID+"_value = {");
		out.println("    ."+OBJECT_CLASS+" = &c_Array_Char,");
		out.println("    ."+ARRAY_LENGTH+" = "+string.length()+",");
		out.print("    ."+ARRAY_ELEMENTS+" = ");
		methodCompiler.stringCons.putStringCharArray(string, "};");  // modularization boundary hack!
		
		out.println("struct o_java_lang_String "+stringID+" = {");
		//NOTE this must match the layout in classpath java/lang/String.h
		out.println("    ."+OBJECT_CLASS+" = &c_java_lang_String,");
		out.println("    .value = &"+stringID+"_value,");
		out.println("    .count = "+string.length()+",");
		out.println("    .cachedHashCode = 0,");
		out.println("    .offset = 0};");
	    // OpenJDK version:
		//out.print("                .hash = "+string.hashCode()+"}");
		//			// this relies on the target java.lang.String having the same
		//			// hash calculation as this one does
	}

	private String staticStringID(String string) {
		return "SC"+Integer.toString(System.identityHashCode(string),36);
	}

	private void putStaticString(String string) {
		out.print("&"+staticStringID(string));
	}
	
	HashSet<String> classInfoStrings = new HashSet<String>();
	private String putClassInfoString(String string) {
		String name = "CS"+Integer.toString(System.identityHashCode(string),36);
		if (classInfoStrings.contains(string)) return name;
		classInfoStrings.add(string);
		out.print("static "+CLASS_INFO_UTF8Z_TYPE+" "+name+" = \"");
		ByteBuffer utf8Bytes = StandardCharsets.UTF_8.encode(string);
		for (int n = utf8Bytes.remaining();  n-- > 0; ) {
			byte b = utf8Bytes.get();
			if (b >= 32 && b < 127) out.print((char)b);
			else { out.print("\\x"); out.print(Integer.toHexString(b)); }	
		}
		out.println("\";");
		return name;
	}

	private String putClassAttributes(List<Attribute> attrs) {
		String name = "attributes";
		ArrayList<String> list = new ArrayList<String>();
		for (int ax = 0; ax < attrs.size(); ++ax) {
            Attribute attr = attrs.get(ax);
    		// TODO Auto-generated method stub
		}
		for (String s : list) {
			out.println(s);
		}
		return name;
	}

	private String putClassFields(List<FieldNode> fields) {
		String name = "fields";
		ArrayList<String> list = new ArrayList<String>();
		list.add("static "+CLASS_INFO_FIELD_TYPE+" "+name+"[] = {");
		for (int fx = 0; fx < fields.size(); ++fx) {
            FieldNode field = fields.get(fx);
            list.add(putNameAndDesc(field.access,field.name,field.desc,field.attrs));
		}
		list.add("};");
		for (String s : list) {
			out.println(s);
		}
		return name;
	}

	private String putClassInterfaces(List<String> interfaces) {
		String name = "interfaces";
		ArrayList<String> list = new ArrayList<String>();
		list.add("static "+CLASS_INFO_INTERFACE_TYPE+" "+name+"[] = {");
		for (int ix = 0; ix < interfaces.size(); ++ix) {
            String intfc = interfaces.get(ix);
    		list.add("    {&"+putClassInfoString(intfc)
		      			+"}, // "+intfc);
		}
		list.add("};");
		for (String s : list) {
			out.println(s);
		}
		return name;
	}

	private String putClassMethods(List<MethodNode> methods) {
		String name = "methods";
		ArrayList<String> list = new ArrayList<String>();
		list.add("static "+CLASS_INFO_METHOD_TYPE+" "+name+"[] = {");
		for (int mx = 0; mx < methods.size(); ++mx) {
            MethodNode method = methods.get(mx);
            list.add(putNameAndDesc(method.access,method.name,method.desc,method.attrs));
		}
		list.add("};");
		for (String s : list) {
			out.println(s);
		}
		return name;
	}

	private String putNameAndDesc(int access, String name, String desc, List<Attribute> attrs) {
		return "    {0x"+Integer.toHexString(access)
		      +",&"+putClassInfoString(name)
		      +",&"+putClassInfoString(desc)
		      +"}, // "+accessToString(access)+" "+name+" "+desc;
		//TODO attributes
	}

	//OBSOLETE
	private LinkedHashMap<String,String> inheritedMethodTable(ClassNode cn, boolean fromInterface, boolean silent) throws FileNotFoundException, ClassNotFoundException, IOException {
		LinkedHashMap<String,String> table;
		
		// start with superclass methods
		if (cn.superName == null || fromInterface) {
			table = new LinkedHashMap<String,String>();
		} else  {
			table = inheritedMethodTable(classCache.get(cn.superName), fromInterface, silent);
		}
		// interface methods
		for (int ix = 0; ix < cn.interfaces.size(); ++ix) {
		    String intfc = cn.interfaces.get(ix);
		    LinkedHashMap<String, String> it = inheritedMethodTable(classCache.get(intfc), true/*fromInterface*/, true);
		    for (Entry<String, String> itEnt : it.entrySet() ) {
		        String nameAndDesc = itEnt.getKey();
		        if (!table.containsKey(nameAndDesc)) {
		        	//if (Main.opt_debug) System.out.println("    "+cn.name+" include interface "+intfc+" "+nameAndDesc);
		        	table.put(nameAndDesc, itEnt.getValue());
		        }
		    }
		}
		// class member methods
		String convClassName = MethodCompiler.convertClassName(cn.name);
		for (int mx = 0; mx < cn.methods.size(); ++mx) {
		    MethodNode method = cn.methods.get(mx);
			if ((method.access & Opcodes.ACC_STATIC) == 0) {
		        String nameAndDesc = method.name + method.desc;
		        // TODO signature polymorphic methods 
		        String override = "";
		        if (table.containsKey(nameAndDesc)) {
		        	if (fromInterface) continue;  // don't override with a method from an interface
		        	override = "/*override-incompatible pointer types*/";
		        } else {
		        	if (!silent) {
			        	//if (Main.opt_debug) System.out.println("    "+cn.name+" member "+nameAndDesc);
			    		out.print("    ");
			    		declareMethodPointer(cn.name, method, convClassName);
		        	}
		        }
		        String extName = MethodCompiler.externalMethodName(cn.name, method);
				if ((method.access & Opcodes.ACC_ABSTRACT) != 0) {
		        	table.put(nameAndDesc, METHOD_ABSTRACT+"/*"+extName+"*/");
		        } else  {
		        	table.put(nameAndDesc, override+"&"+extName);
		 		}
			}
		}
		return table;
	}

 	private void collectInheritedMethods(LinkedHashMap<String,String> prototypes,
										 LinkedHashMap<String,String> instances,
										 ClassNode cn,
										 boolean followSuperclass)
										 throws FileNotFoundException, ClassNotFoundException, IOException {
		// start with superclass methods
		if (cn.superName != null && followSuperclass) {
			collectInheritedMethods(prototypes, instances, classCache.get(cn.superName), followSuperclass);
		}
		// interface methods
		for (int ix = 0; ix < cn.interfaces.size(); ++ix) {
		    String intfc = cn.interfaces.get(ix);
		    ClassNode intfcNode = classCache.get(intfc);
		    LinkedHashMap<String,String> intfcPrototypes = new LinkedHashMap<String,String>();
		    LinkedHashMap<String,String> intfcInstances = new LinkedHashMap<String,String>();
			collectInheritedMethods(intfcPrototypes, intfcInstances, intfcNode, false/*followSuperclass*/);
		    for (String nameAndDesc : intfcPrototypes.keySet() ) {
		        if (!prototypes.containsKey(nameAndDesc)) {
		        	if (Main.opt_debug) System.out.println("    "+cn.name+" include interface "+intfc+" "+nameAndDesc);
		        	prototypes.put(nameAndDesc, intfcPrototypes.get(nameAndDesc));
		        	instances.put(nameAndDesc, intfcInstances.get(nameAndDesc));
		        }
		    }
		}
		// class member methods
		String convClassName = MethodCompiler.convertClassName(cn.name);
		for (int mx = 0; mx < cn.methods.size(); ++mx) {
		    MethodNode method = cn.methods.get(mx);
			if ((method.access & Opcodes.ACC_STATIC) == 0) {
		        String nameAndDesc = method.name + method.desc;
		        // TODO signature polymorphic methods 
		        String override = "";
		        if (prototypes.containsKey(nameAndDesc)) {
		        	override = "/*override-incompatible pointer types*/";
		        } else {
		        	prototypes.put(nameAndDesc, methodPrototype("(*"+MethodCompiler.convertMethodName(method.name,method.desc)+")", method, convClassName));
		        	if (Main.opt_debug) System.out.println("    "+cn.name+" member "+nameAndDesc);
		        }
		        String initName = "."+MethodCompiler.convertMethodName(method.name,method.desc)+" = ";
		        String extName = MethodCompiler.externalMethodName(cn.name, method);
				if ((method.access & Opcodes.ACC_ABSTRACT) != 0) {
					instances.put(nameAndDesc, initName+METHOD_ABSTRACT+"/*"+extName+"*/");
		        } else  {
		        	instances.put(nameAndDesc, initName+override+"&"+extName);
		 		}
			}
		}
	}
 
	//OBSOLETE
 	private LinkedHashMap<String, String> inheritedInterfaceTable(ClassNode cn, String intfc) throws FileNotFoundException, ClassNotFoundException, IOException {
		LinkedHashMap<String, String> table = inheritedMethodTable(classCache.get(intfc), true/*fromInterface*/, true/*silent*/);
		for (int mx = 0; mx < cn.methods.size(); ++mx) {
		    MethodNode method = cn.methods.get(mx);
			if ((method.access & Opcodes.ACC_STATIC) == 0) {
		        String nameAndDesc = method.name + method.desc;
		        if (table.containsKey(nameAndDesc)) {
		        	//only add if required in interface
		        	String override = "/*override-incompatible pointer types*/";
			        String extName = MethodCompiler.externalMethodName(cn.name, method);
					if ((method.access & Opcodes.ACC_ABSTRACT) != 0) {
			        	table.put(nameAndDesc, METHOD_ABSTRACT+"/*"+extName+"*/");
			        } else  {
			        	table.put(nameAndDesc, override+"&"+extName);
			 		}
		        }
			}
		}
		return table;
	}

	private void collectInterfaceMethods(LinkedHashMap<String,String> prototypes,
										 LinkedHashMap<String,String> instances,
										 ClassNode cn,
										 ClassNode intfc)
										 throws FileNotFoundException, ClassNotFoundException, IOException {
		// interface methods
		LinkedHashMap<String,String> intfcPrototypes = new LinkedHashMap<String,String>();
	    LinkedHashMap<String,String> intfcInstances = new LinkedHashMap<String,String>();
		collectInheritedMethods(intfcPrototypes, intfcInstances, intfc, true/*followSuperclass*/);
	    for (String nameAndDesc : intfcPrototypes.keySet() ) {
	        if (!prototypes.containsKey(nameAndDesc)) {
	        	if (Main.opt_debug) System.out.println("    "+intfc.name+" include interface "+intfc.name+" "+nameAndDesc);
	        	prototypes.put(nameAndDesc, intfcPrototypes.get(nameAndDesc));
	        	instances.put(nameAndDesc, intfcInstances.get(nameAndDesc));
	        }
	    }
		// class member methods
		for (int mx = 0; mx < cn.methods.size(); ++mx) {
		    MethodNode method = cn.methods.get(mx);
			if ((method.access & Opcodes.ACC_STATIC) == 0) {
		        String nameAndDesc = method.name + method.desc;
		        // TODO signature polymorphic methods 
		        if (prototypes.containsKey(nameAndDesc)) {
		        	// include only if overriding, i.e. the interface requires the method
		        	String override = "/*override-incompatible pointer types*/";
			        String initName = "."+MethodCompiler.convertMethodName(method.name,method.desc)+" = ";
			        String extName = MethodCompiler.externalMethodName(cn.name, method);
					if ((method.access & Opcodes.ACC_ABSTRACT) != 0) {
						instances.put(nameAndDesc, initName+METHOD_ABSTRACT+"/*"+extName+"*/");
			        } else  {
			        	instances.put(nameAndDesc, initName+override+"&"+extName);
			 		}
		        }
			}
		}
	}
	
	private ListIterator<String> inheritedFieldsTable(ClassNode cn) throws FileNotFoundException, ClassNotFoundException, IOException {
		// NOTE result should be iterated in reverse order!
		LinkedHashMap<String,String> table = new LinkedHashMap<String,String>();
		while (true) {
			String convClassName = null;
			for (int fx = cn.fields.size()-1; fx >= 0; --fx) {
	            FieldNode field = cn.fields.get(fx);
	        	if ((field.access & Opcodes.ACC_STATIC) == 0) {
	                String name = MethodCompiler.escapeName(field.name);
	                if (table.containsKey(name)) {
	            		if (convClassName == null) convClassName = MethodCompiler.convertClassName(cn.name);
	                	name = "hidden_"+convClassName+"_"+name;
	                }
	        		String decl = MethodCompiler.convertTypeDesc(field.desc)+" "+name+";";
		        	table.put(name, decl);
	        	}
	        }
			if (cn.superName == null) break;
			cn = classCache.get(cn.superName);
		}
		return new ArrayList<String>(table.values()).listIterator(table.size());
	}

	private HashSet<String> typeReferences = new HashSet<String>();
	private void referenceTypeClass(String internalName) {
		if (!typeReferences.contains(internalName)) {
			String convClassName = MethodCompiler.convertClassName(internalName);
			out.println("typedef struct "+OBJECT_STRUCT_PREFIX+convClassName+" *"+convClassName+";");
			out.println("typedef struct "+ARRAY_STRUCT_PREFIX+convClassName+" *"+T_ARRAY_+convClassName+";");
			typeReferences.add(internalName);
		}
	}
	
	void referenceType(Type type) {
		int s = type.getSort();
		if (s == Type.OBJECT) {
			referenceTypeClass(type.getInternalName());
		} else if (s == Type.ARRAY) {
			Type elemType = type.getElementType();
			referenceType(elemType);
		}
	}

	private void referenceInterfaceTypes(ClassNode cn) throws FileNotFoundException, ClassNotFoundException, IOException {
		for (int ix = 0; ix < cn.interfaces.size(); ++ix) {
            String intfc = cn.interfaces.get(ix);
            referenceTypeClass(intfc);
            ClassNode intfcNode = classCache.get(intfc);
			referenceInterfaceTypes(intfcNode);
			for (int mx = 0; mx < intfcNode.methods.size(); ++mx) {
	            MethodNode method = intfcNode.methods.get(mx);
	    		for (Type argType : Type.getArgumentTypes(method.desc)) {
	    			referenceType(argType);
	    		}
				referenceType(Type.getReturnType(method.desc));
	        }
		}
	}

	private void referenceRequired(ClassNode cn, ClassReferences references) throws FileNotFoundException, ClassNotFoundException, IOException {
		for (int mx = 0; mx < cn.methods.size(); ++mx) {
            MethodNode method = cn.methods.get(mx);
    		for (Type argType : Type.getArgumentTypes(method.desc)) {
    			references.typeReference(argType);
    		}
    		references.typeReference(Type.getReturnType(method.desc));
        }
		for (int ix = 0; ix < cn.interfaces.size(); ++ix) {
            String intfc = cn.interfaces.get(ix);
            references.classReference(intfc);
            ClassNode intfcNode = classCache.get(intfc);
			referenceRequired(intfcNode, references);
		}
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
    
	private void declareMethod(String className, MethodNode method) {
		if ((method.access & Opcodes.ACC_NATIVE) != 0) out.print("/*NATIVE*/ ");
		out.print("extern ");
		out.println(methodPrototype(MethodCompiler.externalMethodName(className, method), method, MethodCompiler.convertClassName(className)));
	}
    
	private void declareMethodPointer(String className, MethodNode method, String thisType) {
		out.println(methodPrototype("(*"+MethodCompiler.convertMethodName(method.name,method.desc)+")", method, thisType));
	}

	private String methodPrototype(String name, MethodNode method, String thisType) {
		StringBuilder r = new StringBuilder();
		r.append(MethodCompiler.convertType(Type.getReturnType(method.desc))+" "+name+"(");
		String sep = "";
    	if ((method.access & Opcodes.ACC_STATIC) == 0) {
    		r.append(thisType);
			sep = ",";
		}
		for (Type argType : Type.getArgumentTypes(method.desc)) {
			r.append(sep);  sep = ",";
			r.append(MethodCompiler.convertType(argType));
		}
		r.append(");");
		return r.toString();
	}

	private void declareField(FieldNode field) {
		out.println(MethodCompiler.convertTypeDesc(field.desc)+" "+MethodCompiler.escapeName(field.name)+";");
	}

	static String accessToString(int access) {
		StringBuilder s = new StringBuilder();
		if ((access & ACC_PUBLIC) != 0)		s.append("Pub");
		if ((access & ACC_PRIVATE) != 0)	s.append("Pri");
		if ((access & ACC_PROTECTED) != 0)	s.append("Pro");
		if ((access & ACC_STATIC) != 0)		s.append("Sta");
		if ((access & ACC_FINAL) != 0)		s.append("Fin");
		if ((access & ACC_SUPER) != 0)		s.append("Sup");
		if ((access & ACC_SYNCHRONIZED) != 0)	s.append("Syn");
		if ((access & ACC_VOLATILE) != 0)	s.append("Vol");
		if ((access & ACC_BRIDGE) != 0)		s.append("Bri");
		if ((access & ACC_VARARGS) != 0)	s.append("Var");
		if ((access & ACC_TRANSIENT) != 0)	s.append("Tra");
		if ((access & ACC_NATIVE) != 0)		s.append("Nat");
		if ((access & ACC_INTERFACE) != 0)	s.append("Int");
		if ((access & ACC_ABSTRACT) != 0)	s.append("Abs");
		if ((access & ACC_STRICT) != 0)		s.append("Str");
		if ((access & ACC_SYNTHETIC) != 0)	s.append("Sth");
		if ((access & ACC_ANNOTATION) != 0)	s.append("Ann");
		if ((access & ACC_ENUM) != 0)		s.append("Enu");
		if ((access & ACC_MANDATED) != 0)	s.append("Man");
		if ((access & ACC_DEPRECATED) != 0)	s.append("Dep");
		return s.toString();
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
