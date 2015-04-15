package ca.nevdull.mjc.compiler;

import java.io.PrintWriter;

import org.antlr.v4.runtime.Token;

public class VariableSymbol extends Symbol {

    public VariableSymbol(Token token, Type type, Scope currentScope) {
    	super(token, currentScope);
		setType(type);
	}

    public String toString() {
    	return getName()+":"+getType();
    }

	public void writeImport(PrintWriter pw) {
		super.writeImport(pw);
	}
}
