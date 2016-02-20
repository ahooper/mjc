package ca.nevdull.mjc1.compiler.llvm;

public class CompareOperation extends Instruction {

	protected String operation;
	protected Operand op1;
	protected Operand op2;

	public CompareOperation(String operation, Type type, Operand op1, Operand op2) {
		super(Type.I1);
		this.operation = operation;
		assert op1.getType().equals(type);
		assert op2.getType().equals(type);
		this.op1 = op1;
		this.op2 = op2;
	}

	@Override
	public String toString() {
		return getResultName()+" = "+operation+" "+getType().toText()+" "+op1.asOperand()+", "+op2.asOperand();
	}
}
