package ca.nevdull.mjc1.compiler.llvm;

public abstract class Operand {
	private Type type;

	public Operand(Type type) {
		this.type = type;
	}

	public Type getType() {
		return type;
	}
	
	public abstract String asOperand();

}
