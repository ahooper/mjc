package ca.nevdull.j1.compiler;

import org.antlr.v4.runtime.Token;

public class MethodSymbol extends ScopingSymbol implements Type {

	public MethodSymbol(Scope enclosingScope, Token name) {
		super(enclosingScope, name);
	}

	public String getName() {
		return "method "+name;
	}

	public String toString() {
		return getName()+getMemberNames().toString()+":"+type;
	}

}
