package ca.nevdull.mjc1.compiler.llvm;

public abstract class Metadata extends Constant {

	public Metadata(Type type, String text) {
		super(Type.METADATA, text);
	}

}
