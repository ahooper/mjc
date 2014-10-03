package ca.nevdull.mjc.compiler;

public class ArrayType extends Type {
	Type elementType;

	/**
	 * @param elementType
	 */
	public ArrayType(Type elementType) {
		super();
		this.elementType = elementType;
	}
	

}
