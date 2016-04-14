package ca.nevdull.jbcc3;

import java.io.PrintWriter;
import java.util.List;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.Frame;

public class MethodCompiler extends MethodVisitor {
	static String[] OPCODES = org.objectweb.asm.util.Printer.OPCODES;

	private PrintWriter out;

	private InstructionCompiler inscomp;
	
	public MethodCompiler(PrintWriter out) {
    	super(Opcodes.ASM5);
		this.out = out;
		inscomp = new InstructionCompiler(out);
	}

	void compile(ClassNode classNode, List<MethodNode> methods) throws AnalyzerException {
		for (int mx = 0; mx < methods.size(); ++mx) {
            MethodNode method = methods.get(mx);
            compile(classNode, method);
        }
        out.flush();
	}

	private void compile(ClassNode classNode, MethodNode method) throws AnalyzerException {
		out.println("method "+method.name+" "+method.desc);
		InsnList ins = method.instructions;
		if (ins.size() > 0) {
		    Analyzer a = new Analyzer(new BasicInterpreter());
		    Frame[] frames = a.analyze(classNode.name, method);
		    for (int fx = 0; fx < frames.length; ++fx) {
		    	Frame frame = frames[fx];
		    	AbstractInsnNode insn = ins.get(fx);
		    	insn.accept(this);  
				out.println(frame);
				inscomp.setFrame(frame);  //TODO change to on visitFrame, and initial
				insn.accept(inscomp);
		    }
		}
	}

	@Override
	public void visitParameter(String name, int access) {
		out.println("\tParameter\t"+name+" "+access);
	}

	@Override
	public void visitAttribute(Attribute attr) {
		out.println("\tattribute\t"+attr.type);
	}

	@Override
	public void visitCode() {
		out.println("\tcode");
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		out.println("\tframe "+type+" local "+nLocal+" stack "+nStack);
	}

	@Override
	public void visitLabel(Label label) {
		out.print(makeLabel(label));
		out.println(":");
	}

	private String makeLabel(Label label) {
		return "L"+Integer.toString(System.identityHashCode(label),36);
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		out.println("\tTryCatchBlock\t"+makeLabel(start)+" "+makeLabel(end)+" "+makeLabel(handler)+" "+type);
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		if (signature==null) signature = "";  // not generic
		out.println("\tLocalVariable\t"+name+" "+desc+" "+signature+" "+makeLabel(start)+" "+makeLabel(end)+" "+index);
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		out.println("\t// line\t"+line+" "+makeLabel(start));
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		out.println("\tMaxs\t"+maxStack+" "+maxLocals);
	}

	@Override
	public void visitInsn(int opcode) {
		out.println("\t// \t"+OPCODES[opcode]);
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		out.println("\t// "+OPCODES[opcode]+"\t"+operand);
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		out.println("\t// \t"+OPCODES[opcode]+"\t"+var);
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		out.println("\tType\t"+OPCODES[opcode]+"\t"+type);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		out.println("\t// "+OPCODES[opcode]+"\t"+owner+" "+name+" "+desc);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		out.println("\tMethod\t"+OPCODES[opcode]+"\t"+owner+" "+name+" "+desc+" "+itf);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		out.println("\tInvokeDynamic\t"+name+" "+desc);
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		out.println("\tJump\t"+OPCODES[opcode]+"\t"+makeLabel(label));
	}

	@Override
	public void visitLdcInsn(Object cst) {
		out.println("\t// "+cst.getClass().getSimpleName()+" "+cst);
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		out.println("\tIinc\t"+var+" "+increment);
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label defalt, Label... labels) {
		out.println("\tTableSwitch\t"+min+" "+max+" "+makeLabel(defalt)+" "+labels.length);
	}

	@Override
	public void visitLookupSwitchInsn(Label defalt, int[] keys, Label[] labels) {
		out.println("\tLookupSwitch\t"+makeLabel(defalt)+" "+keys.length+" "+labels.length);
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		out.println("\tMultiANewArray\t"+desc+" "+dims);
	}

	@Override
	public void visitEnd() {
		out.println("\tEnd");
	}

}
