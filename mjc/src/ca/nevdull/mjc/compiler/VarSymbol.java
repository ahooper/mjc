package ca.nevdull.mjc.compiler;

import org.antlr.v4.runtime.Token;

public class VarSymbol extends Symbol {

	public VarSymbol(Token nameToken, Type type) {
		super(nameToken);
		setType(type);
	}

    public String toString() {
    	return getName()+":"+getType();
    }

}
