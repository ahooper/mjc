package ca.nevdull.mjc1.compiler;

public class BlockScope extends Scope {

	public BlockScope(String name, Scope enclosingScope) {
		super(name, enclosingScope);
	}

}
