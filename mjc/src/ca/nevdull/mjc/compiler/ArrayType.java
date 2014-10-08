package ca.nevdull.mjc.compiler;

public class ArrayType extends Symbol implements Type {
	Type elementType;

	/**
	 * @param elementType
	 */
	public ArrayType(Type elementType) {
		super(elementType.getName()+"[]");
		this.elementType = elementType;
	}

	public ArrayType() {
		super("unresolved[]");
		this.elementType = null;
	}
	
	/**
	 * @return the elementType
	 */
	public Type getElementType() {
		return elementType;
	}

	/**
	 * @param elementType the elementType to set
	 */
	public void setElementType(Type elementType) {
		this.elementType = elementType;
	}

	public String toString() {
		if (elementType == null) return "unresolved[]";
		return elementType.toString()+"[]";
	}

}
