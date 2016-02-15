package ca.nevdull.mjc1.compiler.llvm;

public class FunctionType extends Type {
	private Type returnType;
	private Type[] parameterTypes;
	private boolean varArgs;

	public FunctionType(Type returnType, Type[] parameterTypes, boolean varArgs) {
		super();
		this.returnType = returnType;
		this.parameterTypes = parameterTypes;
		this.varArgs = varArgs;
	}

	public Type getReturnType() {
		return returnType;
	}

	public Type[] getParameterTypes() {
		return parameterTypes;
	}

	public boolean isVarArgs() {
		return varArgs;
	}
	
	@Override
	public String toText() {
		StringBuilder sb = new StringBuilder();
		sb.append(returnType.toString());
		sb.append("(");
		String sep = "";
		for (int i = 0; i < parameterTypes.length; i++) {
			sb.append(sep).append(parameterTypes[i].toString());
			sep = ",";
		}
		if (varArgs) sb.append(sep).append("...");
		sb.append(")");
		return sb.toString();
	}

	@Override
	public boolean equals(Type comp) {
		if (this == comp) return true;
		if (comp == null) return false;
		if (this.getClass() != comp.getClass()) return false;
		FunctionType ftcomp = (FunctionType)comp;
		if (!this.returnType.equals(ftcomp.returnType)) return false;
		if (this.parameterTypes.length != ftcomp.parameterTypes.length) return false;
		for (int i = 0; i < parameterTypes.length; i++) {
			if (!this.parameterTypes[i].equals(ftcomp.parameterTypes[i])) return false;
		}
		return this.varArgs == ftcomp.varArgs;
	}
}
