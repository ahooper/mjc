package ca.nevdull.mjc.compiler;

public class PrimitiveType extends Symbol implements Type {

	private static final long serialVersionUID = 7696320589029199833L;
	
	private PrimitiveType(String name) {
		super(name, null);
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
}
