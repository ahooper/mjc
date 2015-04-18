package ca.nevdull.cob.compiler;

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

}
