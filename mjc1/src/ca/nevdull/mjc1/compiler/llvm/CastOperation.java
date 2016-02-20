package ca.nevdull.mjc1.compiler.llvm;

public class CastOperation extends Instruction {

	protected String operation;
	protected Operand op1;

	public CastOperation(Type type1, Type type2, Operand op) {
		super(type2);
		this.operation = operation;
		assert op.getType().equals(type1);
		this.op1 = op;
	}

	@Override
	public String toString() {
		return getResultName()+" = "+operation+" "+op1.getType().toText()+" "+op1.asOperand()+" to "+getType().toText();
	}
}
