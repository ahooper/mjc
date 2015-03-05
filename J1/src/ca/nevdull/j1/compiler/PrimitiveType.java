package ca.nevdull.j1.compiler;

import java.util.Map;
import java.util.HashMap;

public class PrimitiveType implements Type {
	String name;
	static Map<String,PrimitiveType>primitiveTypes = new HashMap<String,PrimitiveType>();
	
	static PrimitiveType booleanType = definePrimitiveType("boolean");
	static PrimitiveType charType = definePrimitiveType("char");
	static PrimitiveType byteType = definePrimitiveType("byte");
	static PrimitiveType shortType = definePrimitiveType("short");
	static PrimitiveType intType = definePrimitiveType("int");
	static PrimitiveType longType = definePrimitiveType("long");
	static PrimitiveType floatType = definePrimitiveType("float");
	static PrimitiveType doubleType = definePrimitiveType("double");
	static PrimitiveType voidType = definePrimitiveType("void");
	static PrimitiveType nullType = definePrimitiveType("null");

	public static PrimitiveType definePrimitiveType(String name) {
		PrimitiveType type = new PrimitiveType();
		type.name = name;
		primitiveTypes.put(name, type);
		return type;
	}

	public static PrimitiveType resolve(String name) {
		return primitiveTypes.get(name);
	}
	
	public String toString() {
		return name;
	}

}
