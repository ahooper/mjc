package ca.nevdull.mjc1.compiler.llvm;

public class PointerType extends Type {
	
	private Type base;

	public PointerType(Type base) {
		this.base = base;
	}

	public Type getBase() {
		return base;
	}

	@Override
	public String toText() {
		return base.toString()+"*";
	}

	@Override
	public boolean equals(Type comp) {
		if (this == comp) return true;
		if (comp == null) return false;
		if (this.getClass() != comp.getClass()) return false;
		PointerType ptcomp = (PointerType)comp;
		return this.base.equals(ptcomp.base);
	}
}
