package ca.nevdull.mjc1.compiler;

import java.util.Collection;

import org.antlr.v4.runtime.Token;

public class MethodSymbol extends Scope {

	public MethodSymbol(Token name, Scope enclosingScope) {
		super(name, enclosingScope);
	}
	
	public Collection<Symbol> getParameters() {
		return symbols.values();
	}
	
}
