package ca.nevdull.mjc1.compiler;

public class Type extends Symbol {
	int size;

	public Type(String name, int size) {
		super(name);
		this.size = size;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public int getSize() {
		return this.size;
	}
	
	final static Type booleanType = new Type("boolean",1);
	final static Type byteType = new Type("byte",1);
	final static Type charType = new Type("char",1);
	final static Type doubleType = new Type("double",1);
	final static Type floatType = new Type("float",1);
	final static Type intType = new Type("int",1);
	final static Type longType = new Type("long",1);
	final static Type shortType = new Type("short",1);
	final static Type stringType = new Type("string",1);
	final static Type voidType = new Type("void",0);
	final static Type errorType = new Type("unknown",0);

}
