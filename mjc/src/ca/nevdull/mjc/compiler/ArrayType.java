package ca.nevdull.mjc.compiler;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

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

    public void writeImportTypeContent(DataOutput out)
            throws IOException {
    	out.writeUTF(elementType.getClass().getSimpleName());
    	elementType.writeImportTypeContent(out);
    }

    public Type readImportTypeContent(DataInput in)
            throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		elementType = Symbol.readImportType(in);
		return this;
    }

}
