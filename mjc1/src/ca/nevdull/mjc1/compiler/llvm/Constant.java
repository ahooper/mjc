package ca.nevdull.mjc1.compiler.llvm;

public class Constant extends Operand {
	protected String text;

	public Constant(Type type, String text) {
		super(type);
		this.text = text;
	}

	public String getText() {
		return text;
	}

	@Override
	public String asOperand() {
		return text;
	}

	public static Constant TRUE = new Constant(Type.I1, "true");
	public static Constant FALSE = new Constant(Type.I1, "false");
	
	public Constant(byte value) {
		this(Type.I8, Byte.toString(value));
	}
	
	public Constant(short value) {
		this(Type.I16, Short.toString(value));
	}
	
	public Constant(int value) {
		this(Type.I32, Integer.toString(value));
	}
	
	public Constant(long value) {
		this(Type.I64, Long.toString(value));
	}
	
	public Constant(float value) {
		this(Type.FLOAT, Float.toString(value));
	}
	
	public Constant(double value) {
		this(Type.DOUBLE, Double.toString(value));
	}
	
	public Constant(PointerType type) {
		this(type, "null");
	}
	
	public static Constant array(Type type, Operand ... values) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		String sep = "";
		for (int i = 0; i < values.length; i++) {
			sb.append(sep).append(values[i].toString());
			sep = ",";
		}
		sb.append("]");
		return new Constant(type,sb.toString());
	}
	
	public static Constant string(Type type, byte[] value) {
		StringBuilder sb = new StringBuilder();
		sb.append("c\"");
		for (int i = 0; i < value.length; i++) {
			int b = value[i];
            if (b < ' ' || b > '~' || b == '"' || b == '\\') {
                sb.append(String.format("\\%02X", b));
            } else {
                sb.append((char) b);
            }
		}
		sb.append('"');
		return new Constant(type,sb.toString());
	}
}
