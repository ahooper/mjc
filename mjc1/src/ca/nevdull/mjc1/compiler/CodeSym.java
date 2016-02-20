package ca.nevdull.mjc1.compiler;

public class CodeSym {
	Type type;
	ca.nevdull.mjc1.compiler.llvm.Operand operand;
	boolean pointer;

	public Type getType() {
		return type;
	}

	public boolean isPointer() {
		return pointer;
	}

	public CodeSym(Type type, ca.nevdull.mjc1.compiler.llvm.Operand operand, boolean pointer) {
		this.type = type;
		this.operand = operand;
		this.pointer = pointer;
	}
	
/*
	public static CodeSym makePointer(Type type, String regType, String name) {
		CodeSym r = new CodeSym();
		r.type = type;
		r.regType = regType;
		r.pointer = true;
		r.name = name;
		return r;
	}

	public String typeAndName() {
		StringBuilder s = new StringBuilder();
		s.append(regType);
		if (pointer) s.append("*");
		s.append(" ");
		s.append(name);
		return s.toString();
	}

	public static CodeSym makeRef(Type type, String regType, String name) {
		CodeSym r = new CodeSym();
		r.type = type;
		r.regType = regType;
		r.pointer = true;
		r.name = name;
		return r;
	}
*/
}
