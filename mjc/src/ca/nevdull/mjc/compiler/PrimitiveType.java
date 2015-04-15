package ca.nevdull.mjc.compiler;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class PrimitiveType extends Symbol implements Type {
	
	// table for reading by name on import
	private static Map<String,PrimitiveType> byName = new HashMap<String,PrimitiveType>(8);
	
	private PrimitiveType(String name) {
		super(name, null);
		// populate import table as each named primitive type is created
		byName.put(name, this);
	}
	
	public String toString() {
		return name;
	}
	
	static PrimitiveType booleanType	= new PrimitiveType("boolean");
	static PrimitiveType byteType		= new PrimitiveType("byte");
	static PrimitiveType charType		= new PrimitiveType("char");
	static PrimitiveType doubleType		= new PrimitiveType("double");
	static PrimitiveType floatType		= new PrimitiveType("float");
	static PrimitiveType intType		= new PrimitiveType("int");
	static PrimitiveType longType		= new PrimitiveType("long");
	static PrimitiveType shortType		= new PrimitiveType("short");

	public void writeImportType(PrintWriter pw) {
		pw.append(name);
	}
}
