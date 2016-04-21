package ca.nevdull.jbcc3;

import java.io.PrintWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class InstructionCommenter extends MethodVisitor {
	static String[] OPCODES = org.objectweb.asm.util.Printer.OPCODES;

	private PrintWriter out;

	public InstructionCommenter() {
    	super(Opcodes.ASM5);
	}

	public void setOut(PrintWriter out) {
		this.out = out;
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		out.println("\t// frame "+type+" local "+nLocal+" stack "+nStack);
	}

	@Override
	public void visitLabel(Label label) {
		/* done in InstructionCompiler.mark
		out.print(InstructionCompiler.makeLabel(label));
		out.println(":");
		*/
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		out.println("\tTryCatchBlock\t"+MethodCompiler.makeLabel(start)+" "+MethodCompiler.makeLabel(end)+" "+MethodCompiler.makeLabel(handler)+" "+type);
	}
	
	@Override
	public void visitLineNumber(int line, Label start) {
		out.println("\t// line\t"+line+" "+MethodCompiler.makeLabel(start));
	}

	@Override
	public void visitInsn(int opcode) {
		out.println("\t// "+OPCODES[opcode]);
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		out.println("\t// "+OPCODES[opcode]+"\t"+operand);
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		out.println("\t// "+OPCODES[opcode]+"\t"+var);
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		out.println("\t// "+OPCODES[opcode]+"\t"+type);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		out.println("\t// "+OPCODES[opcode]+"\t"+owner+" "+name+" "+desc);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		out.println("\t// "+OPCODES[opcode]+"\t"+owner+" "+name+" "+desc+" "+itf);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		out.println("\t// InvokeDynamic\t"+name+" "+desc);
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		out.println("\t// "+OPCODES[opcode]+"\t"+MethodCompiler.makeLabel(label));
	}

	@Override
	public void visitLdcInsn(Object cst) {
		out.print("\t// Ldc\t"+cst.getClass().getSimpleName()+" ");
		if (cst instanceof String) {
			String str = (String)cst;
			int nl = str.indexOf('\n');
			if (nl >= 0) str = str.substring(0,nl)+"\\n";
			while (str.endsWith("\\")) str = str.substring(0,str.length()-1);
			out.println(str);
		} else {
			out.println(cst);
		}
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		out.println("\t// Iinc\t"+var+" "+increment);
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label defalt, Label... labels) {
		out.println("\t// TableSwitch\t"+min+" "+max+" "+MethodCompiler.makeLabel(defalt)+" "+labels.length);
	}

	@Override
	public void visitLookupSwitchInsn(Label defalt, int[] keys, Label[] labels) {
		out.println("\t// LookupSwitch\t"+MethodCompiler.makeLabel(defalt)+" "+keys.length+" "+labels.length);
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		out.println("\t// MultiANewArray\t"+desc+" "+dims);
	}

}
