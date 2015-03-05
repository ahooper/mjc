package ca.nevdull.j1.compiler;

public class ArrayType implements Type {
	Type elementType;
	
	public ArrayType(Type elementType) {
		this.elementType = elementType;
	}
	
	public Type getElementType() {
		return elementType;
	}

	static Type create(Type elementType, int dimensions) {
		assert dimensions >= 0;
		if (elementType instanceof ErrorType) return elementType;
		if (dimensions == 0) return elementType;
		return new ArrayType(create(elementType, dimensions-1));
	}
	
}
