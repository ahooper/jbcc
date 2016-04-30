package ca.nevdull.jbcc3;

import java.io.PrintWriter;
import java.util.HashMap;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class StringConstants extends MethodVisitor implements CCode {

	private PrintWriter out;
	private HashMap<String, String> stringConstants = new HashMap<String, String>();

	public StringConstants() {
    	super(Opcodes.ASM5);
	}

	public void setOut(PrintWriter out) {
		this.out = out;
	}

	public void clear() {
		stringConstants.clear();	
	}
	
    public void visitLdcInsn(final Object cst) {
    	if (cst instanceof String) {
    		String scon = (String)cst;
    		if (!stringConstants.containsKey(scon)) {
		        String scn = STRING_CONSTANT_STRING+Integer.toString(System.identityHashCode(scon),36);
		        out.print("static "+STRING_CONST_TYPE+" "+scn+" = {0,"+scon.length()+",");
		        putStringCharArray(scon,"};");
		        stringConstants.put(scon,scn);
    		}
    	} else {
    		//out.println("StringConstants visitLdcInsn "+cst.getClass().getSimpleName());
    	}
    }

	protected void putStringCharArray(String scon, String lineEnd) {
		out.print("{");
		String sep = "";
		for (char ch : scon.toCharArray()) {out.print(sep); sep = ","; out.print((int)ch); }
		out.print("}"+lineEnd+" // ");
		out.println(CCode.safeCommentSubstring(scon));
	}

	public String get(String scon) {
		return stringConstants.get(scon);
	}

}
