package ca.nevdull.mjc1.compiler.llvm;

public class StructureType extends Type {
	private Type[] types;

	public StructureType(Type ... types) {
		super();
		this.types = types;
	}

	public long getTypeCount() {
		return types.length;
	}

	public Type getTypeAt(int index) {
		return types[index];
	}

	@Override
	public String toText() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		String sep = "";
		for (int i = 0; i < types.length; i++) {
			sb.append(sep).append(types[i].toString());
			sep = ",";
		}
		sb.append("}");
		return sb.toString();
	}
	
	@Override
	public boolean equals(Type comp) {
		if (this == comp) return true;
		if (comp == null) return false;
		if (this.getClass() != comp.getClass()) return false;
		StructureType stcomp = (StructureType)comp;
		if (this.types.length != stcomp.types.length) return false;
		for (int i = 0; i < types.length; i++) {
			if (!this.types[i].equals(stcomp.types[i])) return false;
		}
		return true;
	}
}
