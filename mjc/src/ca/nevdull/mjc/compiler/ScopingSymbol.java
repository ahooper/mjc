package ca.nevdull.mjc.compiler;

import org.antlr.v4.runtime.Token;

public class ScopingSymbol extends Symbol {
	public Scope enclosingScope;

	public ScopingSymbol(Token nameToken, Scope enclosingScope) {
		super(nameToken);
		this.enclosingScope = enclosingScope;
	}

	public String getScopeName() {
		return name;
	}

	public Scope getEnclosingScope() {
    	return enclosingScope;
	}

}
