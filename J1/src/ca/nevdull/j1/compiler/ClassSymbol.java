package ca.nevdull.j1.compiler;

import org.antlr.v4.runtime.Token;

public class ClassSymbol extends ScopingSymbol implements Type {

	public ClassSymbol(Scope enclosingScope, Token name) {
		super(enclosingScope, name);
	}

	public ClassSymbol(Scope enclosingScope, String name) {
		super(enclosingScope, name);
	}

	public String getName() {
		return "class "+name;
	}

	public String toString() {
		return getName()+getMemberNames().toString()+":"+type;
	}

}
