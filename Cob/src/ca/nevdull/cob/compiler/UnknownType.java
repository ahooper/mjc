package ca.nevdull.cob.compiler;

// Representation of unknown types, just a single, common instance

import java.io.PrintWriter;

public class UnknownType implements Type {
	private UnknownType() {
	}
	
	public String toString() {
		return "unknown";
	}
	
	static UnknownType instance	= new UnknownType();

	public void writeImportType(PrintWriter pw) {
		pw.append("?unknown?");
	}

	@Override
	public String getNameString() {
		return "_unknown_";
	}

	@Override
	public String getArrayString() {
		return "";
	}

}
