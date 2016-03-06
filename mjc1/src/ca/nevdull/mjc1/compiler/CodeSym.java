package ca.nevdull.mjc1.compiler;

public class CodeSym {
	Type type;
	String irType;
	boolean address;
	String irName;

	public Type getType() {
		return type;
	}

	public String getIRType() {
		return irType;
	}

	public boolean isAddress() {
		return address;
	}

	public String getIRName() {
		return irName;
	}

	public CodeSym(Type type, String irType, String literal) {
		this.type = type;
		this.irType = irType;
		this.address = false;
		this.irName = literal;
	}

	public CodeSym(Type type, String irType, String irName, boolean address) {
		this.type = type;
		this.irType = irType;
		this.address = address;
		this.irName = irName;
	}
	
}
