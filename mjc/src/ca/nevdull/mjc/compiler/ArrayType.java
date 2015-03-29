package ca.nevdull.mjc.compiler;

public class ArrayType extends Symbol implements Type {

	private static final long serialVersionUID = 8427707270005661330L;
	
	Type elementType;

	/**
	 * @param elementType
	 */
	public ArrayType(Type elementType) {
		super(elementType.getName()+"[]", null);
		this.elementType = elementType;
	}

	public ArrayType() {
		super("unresolved[]", null);
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
