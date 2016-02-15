package ca.nevdull.mjc1.compiler;

import org.antlr.v4.runtime.Token;

public class MethodSymbol extends Scope {

	public MethodSymbol(Token name, Scope enclosingScope) {
		super(name, enclosingScope);
	}
	
}
