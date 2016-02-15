package ca.nevdull.mjc1.compiler.llvm;

public class ArrayType extends Type {
	private long size;
	private Type elementType;

	public ArrayType(long size, Type elementType) {
		super();
		this.size = size;
		this.elementType = elementType;
	}

	public long getSize() {
		return size;
	}

	public Type getElementType() {
		return elementType;
	}

	@Override
	public String toText() {
		return "["+size+" x "+elementType.toString()+"]";
	}

	@Override
	public boolean equals(Type comp) {
		if (this == comp) return true;
		if (comp == null) return false;
		if (this.getClass() != comp.getClass()) return false;
		ArrayType atcomp = (ArrayType)comp;
		if (!this.elementType.equals(atcomp.elementType)) return false;
		return this.size == atcomp.size;
	}
}
