package ca.nevdull.mjc.compiler;

import java.io.PrintWriter;

public class ArrayType extends Symbol implements Type {
	
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

	public void writeImportType(PrintWriter pw) {
		elementType.writeImportType(pw);
		pw.append("[]");
	}
}
