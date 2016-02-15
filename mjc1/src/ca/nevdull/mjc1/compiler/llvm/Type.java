package ca.nevdull.mjc1.compiler.llvm;

public abstract class Type {

	public static final PrimitiveType VOID = new PrimitiveType("void");
	public static final PrimitiveType I1 = new PrimitiveType("i1");
	public static final PrimitiveType I8 = new PrimitiveType("i8");
	public static final PrimitiveType I16 = new PrimitiveType("i16");
	public static final PrimitiveType I32 = new PrimitiveType("i32");
	public static final PrimitiveType I64 = new PrimitiveType("i64");
	public static final PrimitiveType FLOAT = new PrimitiveType("float");
	public static final PrimitiveType DOUBLE = new PrimitiveType("double");
	public static final Type METADATA = new MetadataType();
	
	public abstract boolean equals(Type comp);

	public abstract String toText();
}
