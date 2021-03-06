package ca.nevdull.cob.compiler;

// Representation of an array type

import java.io.PrintWriter;

public class ArrayType implements Type {
	Type elementType;

	public ArrayType(Type elementType) {
		super();
		this.elementType = elementType;
	}

	public Type getElementType() {
		return elementType;
	}

	public String toString() {
		if (elementType == null) return "unresolved[]";
		return elementType.toString()+"[]";
	}

	public void writeImportType(PrintWriter pw) {
		elementType.writeImportType(pw);
		pw.append("[]");
	}

	@Override
	public String getNameString() {
		return elementType.getNameString();
	}

	@Override
	public String getArrayString() {
		return "*";
	}
}
