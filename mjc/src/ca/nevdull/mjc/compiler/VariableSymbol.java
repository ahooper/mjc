package ca.nevdull.mjc.compiler;

import org.antlr.v4.runtime.Token;

public class VariableSymbol extends Symbol {

	private static final long serialVersionUID = 4514522783018333957L;

    public VariableSymbol(Token token, Type type, Scope currentScope) {
    	super(token, currentScope);
		setType(type);
	}

    public String toString() {
    	return getName()+":"+getType();
    }

}
