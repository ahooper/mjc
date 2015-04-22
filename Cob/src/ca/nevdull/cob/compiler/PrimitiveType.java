package ca.nevdull.cob.compiler;

// Representation of the primitive types, with a single instance for each
// void is included as pseudo-type

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class PrimitiveType extends Symbol implements Type {

	// table for reading by name on import
	private static Map<String,PrimitiveType> byName = new HashMap<String,PrimitiveType>(10);
	
	String hostType;
	
	private PrimitiveType(String name, String hostType) {
		super(name, null);
		this.hostType = hostType;
		// populate table as each named primitive type is created
		byName.put(name, this);
	}
	
	public static PrimitiveType getByName(String name) {
		return byName.get(name);
	}
	
	public String toString() {
		return name;
	}
	
	static PrimitiveType booleanType	= new PrimitiveType("boolean",	"cob_boolean");
	static PrimitiveType byteType		= new PrimitiveType("byte",		"cob_byte");
	static PrimitiveType charType		= new PrimitiveType("char",		"cob_char");
	static PrimitiveType doubleType		= new PrimitiveType("double",	"cob_double");
	static PrimitiveType floatType		= new PrimitiveType("float",	"cob_float");
	static PrimitiveType intType		= new PrimitiveType("int",		"cob_int");
	static PrimitiveType longType		= new PrimitiveType("long",		"cob_long");
	static PrimitiveType shortType		= new PrimitiveType("short",	"cob_short");
	static PrimitiveType voidType		= new PrimitiveType("void",		"void");

	public void writeImportType(PrintWriter pw) {
		pw.append(name);
	}

	@Override
	public String getNameString() {
		return hostType;
	}

	@Override
	public String getArrayString() {
		return "";
	}

}
