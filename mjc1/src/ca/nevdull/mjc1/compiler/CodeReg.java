package ca.nevdull.mjc1.compiler;

public class CodeReg {
	Type type;
	String regType;
	String name;
	boolean pointer;
	private static int sequence = 0;
	
	public Type getType() {
		return type;
	}

	public String getRegType() {
		return regType;
	}

	public String getName() {
		return name;
	}

	public boolean isPointer() {
		return pointer;
	}

	private String nextReg() {
		return "%r"+Integer.toHexString(++sequence);
	}
	
	CodeReg(Type type, String regType, boolean pointer) {
		this.type = type;
		this.regType = regType;
		this.pointer = pointer;
		name = nextReg();
	}
	
	private CodeReg() {
	}

	public static CodeReg makePointer(Type type, String regType, String name) {
		CodeReg r = new CodeReg();
		r.type = type;
		r.regType = regType;
		r.pointer = true;
		r.name = name;
		return r;
	}

	public static CodeReg makeLiteral(Type type, String regType, String name) {
		CodeReg r = new CodeReg();
		r.type = type;
		r.regType = regType;
		r.pointer = false;
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

	public static CodeReg makeRef(Type type, String regType, String name) {
		CodeReg r = new CodeReg();
		r.type = type;
		r.regType = regType;
		r.pointer = true;
		r.name = name;
		return r;
	}
}
