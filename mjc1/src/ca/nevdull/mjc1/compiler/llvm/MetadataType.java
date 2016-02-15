package ca.nevdull.mjc1.compiler.llvm;

public class MetadataType extends Type {

	@Override
	public boolean equals(Type comp) {
		return false;
	}

	@Override
	public String toText() {
		return "metadata";
	}

}
