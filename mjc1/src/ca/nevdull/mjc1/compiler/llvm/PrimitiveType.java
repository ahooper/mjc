package ca.nevdull.mjc1.compiler.llvm;

public class PrimitiveType extends Type {
	String name;

	public PrimitiveType(String name) {
		this.name = name;
	}
	
	@Override
	public String toText() {
		return name;
	}

	@Override
	public boolean equals(Type comp) {
		if (this == comp) return true;  // primitive types should be singletons
		if (comp == null) return false;
		if (this.getClass() != comp.getClass()) return false;
		PrimitiveType ptcomp = (PrimitiveType)comp;
		return this.name.equals(ptcomp.name);
	}
	
}
