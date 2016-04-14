package ca.nevdull.jbcc3;

import java.io.PrintWriter;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

public class InstructionCompiler extends InstructionAdapter implements CCode {

	private PrintWriter out;

	public InstructionCompiler(PrintWriter out) {
		super(Opcodes.ASM5, null);
		this.out = out;
	}
	
	Frame/*<BasicValue>*/ frame;
	
	public void setFrame(Frame frame) {
		this.frame = frame;
	}
	
	String stack(int d, Type type) {
		String variant;  int width = 1;
        switch (type.getSort()) {
        case Type.BOOLEAN:
        case Type.CHAR:
        case Type.BYTE:
        case Type.SHORT:
        	// all above treated as INT
        case Type.INT:
            variant = FRAME_INT;
            break;
        case Type.FLOAT:
            variant = FRAME_FLOAT;
            break;
        case Type.LONG:
            variant = FRAME_LONG;  width = 2;
            break;
        case Type.DOUBLE:
            variant = FRAME_DOUBLE;  width = 2;
            break;
        case Type.ARRAY:
            variant = FRAME_ARRAY;
            break;
        // case Type.OBJECT:
        default:
            variant = FRAME_OBJECT;
        }
		return stack(d, variant, width);		
	}

	private String stack(int d, String variant, int width) {
		int loc = frame.getStackSize() - d*width + frame.getLocals();
		return FRAME+loc+variant;
	}
	
	String local(int n, Type type) {
		String v;  int w = 1;
        switch (type.getSort()) {
        case Type.BOOLEAN:
        case Type.CHAR:
        case Type.BYTE:
        case Type.SHORT:
        case Type.INT:
            v = ".I";
            break;
        case Type.FLOAT:
            v = ".F";
            break;
        case Type.LONG:
            v = ".L";  w = 2;
            break;
        case Type.DOUBLE:
            v = ".D";  w = 2;
            break;
        case Type.ARRAY:
            v = ".A";
            break;
        // case Type.OBJECT:
        default:
            v = ".O";
        }
		int l = n;  //TODO direct type for arguments
		return "_"+n+v;		
	}

	
	private final static Type OBJECT_TYPE = Type.VOID_TYPE;  //TODO this is a hack

	@Override
	public void aconst(Object cst) {
		if (cst == null) emit(stack(0,OBJECT_TYPE)+" = null;");
		else emit(stack(0,OBJECT_TYPE)+" = "+cst+";");
		// TODO special cases String, Class
	}

	private void emit(String string) {
		out.println(string);
	}

	@Override
	public void add(Type type) {
		emit(stack(2,type)+" = "+stack(2,type)+" + "+stack(1,type)+";");
	}

	@Override
	public void aload(Type type) {
		// TODO 
	}

	@Override
	public void and(Type type) {
		emit(stack(2,type)+" = "+stack(2,type)+" & "+stack(1,type)+";");
	}

	@Override
	public void anew(Type type) {
		// TODO 
	}

	@Override
	public void areturn(Type type) {
		if (type == OBJECT_TYPE) emit("return;");
		else emit("return "+stack(1,type)+";");
	}

	@Override
	public void arraylength() {
		// TODO 
	}

	@Override
	public void astore(Type type) {
		// TODO 
	}

	@Override
	public void athrow() {
		// TODO 
	}

	@Override
	public void cast(Type type, Type arg1) {
		// TODO 
	}

	@Override
	public void checkcast(Type type) {
		// TODO 
	}

	@Override
	public void cmpg(Type type) {
		// TODO 
	}

	@Override
	public void cmpl(Type type) {
		// TODO 
	}

	@Override
	public void dconst(double cst) {
		emit(stack(0,Type.DOUBLE_TYPE)+" = "+Double.toString(cst)+";");
	}

	@Override
	public void div(Type type) {
		//TODO special cases
		//TODO check division by zero
		emit(stack(2,type)+" = "+stack(2,type)+" / "+stack(1,type)+";");
	}

	@Override
	public void dup() {
		// don't get any type information
		emit(stack(0,FRAME_ANY,1)+" = "+stack(1,FRAME_ANY,1)+";");
	}

	@Override
	public void dup2() {
		// TODO 
	}

	@Override
	public void dup2X1() {
		// TODO 
	}

	@Override
	public void dup2X2() {
		// TODO 
	}

	@Override
	public void dupX1() {
		// TODO 
	}

	@Override
	public void dupX2() {
		// TODO 
	}

	@Override
	public void fconst(float cst) {
		emit(stack(0,Type.FLOAT_TYPE)+" = "+Float.toString(cst)+";");
	}

	@Override
	public void getfield(String owner, String name, String desc) {
		checkReference(1);
		emit(stack(1,Type.getType(desc))+" = (("+escapeName(owner)+")"+stack(1,OBJECT_TYPE)+")->"+name+";");
	}

	private void checkReference(int i) {
		// TODO check s(i) for null reference
	}

	@Override
	public void getstatic(String owner, String name, String desc) {
		emit(stack(0,Type.getType(desc))+" = "+staticFieldName(owner,name)+";");
	}

	private String staticFieldName(String owner, String name) {
		return CLASS_STRUCT_PREFIX+convertClassName(owner)+"."+CLASS_STATIC_FIELDS+"."+escapeName(name);
	}

	private String convertClassName(String owner) {
		return owner.replace('/','_');
	}

	private String escapeName(String name) {
		// TODO 
		return name;
	}

	@Override
	public void goTo(Label target) {
		emit("goto "+makeLabel(target)+";");
	}

	@Override
	public void hconst(Handle arg0) {
		// TODO 
	}

	@Override
	public void iconst(int cst) {
		emit(stack(0,Type.INT_TYPE)+" = "+Integer.toString(cst)+";");
	}

	@Override
	public void ifacmpeq(Label target) {
		// TODO 
	}

	@Override
	public void ifacmpne(Label target) {
		// TODO 
	}

	@Override
	public void ifeq(Label target) {
		// TODO 
	}

	@Override
	public void ifge(Label target) {
		// TODO 
	}

	@Override
	public void ifgt(Label target) {
		// TODO 
	}

	@Override
	public void ificmpeq(Label target) {
		// TODO 
	}

	@Override
	public void ificmpge(Label target) {
		// TODO 
	}

	@Override
	public void ificmpgt(Label target) {
		// TODO 
	}

	@Override
	public void ificmple(Label target) {
		// TODO 
	}

	@Override
	public void ificmplt(Label target) {
		// TODO 
	}

	@Override
	public void ificmpne(Label target) {
		// TODO 
	}

	@Override
	public void ifle(Label target) {
		// TODO 
	}

	@Override
	public void iflt(Label target) {
		// TODO 
	}

	@Override
	public void ifne(Label target) {
		// TODO 
	}

	@Override
	public void ifnonnull(Label target) {
		// TODO 
	}

	@Override
	public void ifnull(Label target) {
		// TODO 
	}

	@Override
	public void iinc(int arg0, int arg1) {
		// TODO 
	}

	@Override
	public void instanceOf(Type type) {
		// TODO 
	}

	@Override
	public void invokedynamic(String arg0, String arg1, Handle arg2, Object[] arg3) {
		// TODO 
	}

	@Override
	public void invokeinterface(String arg0, String arg1, String arg2) {
		// TODO 
	}

	@Override
	public void invokespecial(String arg0, String arg1, String arg2, boolean arg3) {
		// TODO 
	}

	@Override
	public void invokestatic(String arg0, String arg1, String arg2, boolean arg3) {
		// TODO 
	}

	@Override
	public void invokevirtual(String arg0, String arg1, String arg2, boolean arg3) {
		// TODO 
	}

	@Override
	public void jsr(Label target) {
		// TODO 
	}

	@Override
	public void lcmp() {
		// TODO 
	}

	@Override
	public void lconst(long cst) {
		emit(stack(0,Type.LONG_TYPE)+" = "+Long.toString(cst)+";");
	}

	@Override
	public void load(int n, Type type) {
		emit(stack(0,type)+" = "+local(n,type)+";");
	}

	@Override
	public void lookupswitch(Label arg0, int[] arg1, Label[] arg2) {
		// TODO 
	}

	@Override
	public void mark(Label label) {
		emit(makeLabel(label)+":");
	}

	@Override
	public void monitorenter() {
		// TODO 
	}

	@Override
	public void monitorexit() {
		// TODO 
	}

	@Override
	public void mul(Type type) {
		emit(stack(2,type)+" = "+stack(2,type)+" * "+stack(1,type)+";");
	}

	@Override
	public void multianewarray(String arg0, int arg1) {
		// TODO 
	}

	@Override
	public void neg(Type type) {
		// TODO 
	}

	@Override
	public void newarray(Type type) {
		// TODO 
	}

	@Override
	public void nop() {
		// empty
	}

	@Override
	public void or(Type type) {
		emit(stack(2,type)+" = "+stack(2,type)+" | "+stack(1,type)+";");
	}

	@Override
	public void pop() {
		// empty
	}

	@Override
	public void pop2() {
		// empty
	}

	@Override
	public void putfield(String owner, String name, String desc) {
		checkReference(2);
		emit("(("+escapeName(owner)+")"+stack(2,OBJECT_TYPE)+")->"+name+" = "+stack(1,Type.getType(desc))+";");
	}

	@Override
	public void putstatic(String owner, String name, String desc) {
		emit(staticFieldName(owner,name)+" = "+stack(1,Type.getType(desc))+";");
	}

	@Override
	public void rem(Type type) {
		//TODO special cases
		//TODO check division by zero
		emit(stack(2,type)+" = "+stack(2,type)+" % "+stack(1,type)+";");
	}

	@Override
	public void ret(int arg0) {
		// TODO 
	}

	@Override
	public void shl(Type type) {
		emit(stack(2,type)+" = "+stack(2,type)+" << ("+stack(1,Type.INT_TYPE)+" & "+maxShift(type)+");");
	}

	private String maxShift(Type type) {
		return (type.getSort() == Type.LONG) ? "63" : "31";
	}

	@Override
	public void shr(Type type) {
		emit(stack(2,type)+" = "+stack(2,type)+" << ("+stack(1,Type.INT_TYPE)+" & "+maxShift(type)+");");
	}

	@Override
	public void store(int n, Type type) {
		emit(local(n,type)+" = "+stack(0,type)+";");  // TODO integer truncation cast
	}

	@Override
	public void sub(Type type) {
		emit(stack(2,type)+" = "+stack(2,type)+" - "+stack(1,type)+";");
	}

	@Override
	public void swap() {
		// TODO 
	}

	@Override
	public void tableswitch(int arg0, int arg1, Label arg2, Label... arg3) {
		// TODO 
	}

	@Override
	public void tconst(Type type) {
		// TODO 
	}

	@Override
	public void ushr(Type type) {
		// TODO 
	}

	private String makeLabel(Label label) {
		return "L"+Integer.toString(System.identityHashCode(label),36);
	}

	@Override
	public void xor(Type type) {
		emit(stack(2,type)+" = "+stack(2,type)+" ^ "+stack(1,type)+";");
	}

}
