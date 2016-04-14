package ca.nevdull.jbcc3;

import java.io.PrintWriter;
import java.util.HashMap;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class StringConstants extends MethodVisitor implements CCode {

	private HashMap<String, String> stringConstants = new HashMap<String, String>();
	private PrintWriter out;

	public StringConstants(PrintWriter out) {
    	super(Opcodes.ASM5);
    	this.out = out;
	}

	public void setOut(PrintWriter out) {
		this.out.flush();
		this.out = out;
	}
	
    public void visitLdcInsn(final Object cst) {
    	if (cst instanceof String) {
    		String scon = (String)cst;
    		if (!stringConstants.containsKey(scon)) {
		        String scn = STRING_CONSTANT_STRING+Integer.toString(System.identityHashCode(scon),36);
		        out.print("static ");
		        out.print(STRING_CONST);
		        out.print(" ");
		        out.print(scn);
		        out.print(" = {0,");
		        out.print(scon.length());
		        out.print(",{");
		        String sep = "";
		        for (char ch : scon.toCharArray()) {out.print(sep); sep = ","; out.print((int)ch); }
		        out.print("}}; // ");
		        int newline = scon.indexOf('\n');
		        out.println((newline < 0) ? scon : scon.substring(0,newline));
		        stringConstants.put(scon,scn);
    		}
    	}
    }

	public String get(String scon) {
		return stringConstants.get(scon);
	}

}
