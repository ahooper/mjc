package ca.nevdull.mjc.compiler;

import org.antlr.v4.runtime.Token;

public class VarSymbol extends Symbol {

	public VarSymbol(Token nameToken) {
		super(nameToken);
		// TODO Auto-generated constructor stub
	}

    public String toString() {
    	return getName()+":"+getType();
    }

}
