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
	
	public static Constant integer(byte value) {
		return new Constant(Type.I8, Byte.toString(value));
	}
	
	public static Constant integer(short value) {
		return new Constant(Type.I16, Short.toString(value));
	}
	
	public static Constant integer(int value) {
		return new Constant(Type.I32, Integer.toString(value));
	}
	
	public static Constant integer(long value) {
		return new Constant(Type.I64, Long.toString(value));
	}
	
	public static Constant floating(float value) {
		return new Constant(Type.FLOAT, Float.toString(value));
	}
	
	public static Constant floating(double value) {
		return new Constant(Type.DOUBLE, Double.toString(value));
	}
	
	public static Constant nulll(PointerType type) {
		return new Constant(type, "null");
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
