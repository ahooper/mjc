package ca.nevdull.mjc1.compiler;

import org.antlr.v4.runtime.Token;

public class FieldSymbol extends Symbol {

	public FieldSymbol(String name) {
		super(name);
	}

	public FieldSymbol(Token name) {
		super(name);
	}

}
