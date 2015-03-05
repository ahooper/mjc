package ca.nevdull.j1.compiler;

import org.antlr.v4.runtime.Token;

public class LocalScope extends BaseScope {
	Token start;

	public LocalScope(Scope enclosingScope, Token start) {
		super(enclosingScope);
		this.start = start;
	}

	public String getName() {
		return "block@"+start.getLine()+":"+(start.getCharPositionInLine()+1);
	}

}
