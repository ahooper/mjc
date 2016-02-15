package ca.nevdull.mjc1.compiler;

public class ArrayType extends Type {
	Type base;

	public ArrayType(Type base) {
		super(base.name+"[]", 1);
		this.base = base;
	}

	public Type getBase() {
		return base;
	}

}
