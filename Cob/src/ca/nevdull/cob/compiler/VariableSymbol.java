package ca.nevdull.cob.compiler;

// Representation of a named program variable, which includes
// object fields, method arguments, and local variables.

import java.io.PrintWriter;

import org.antlr.v4.runtime.Token;

public class VariableSymbol extends Symbol {

	public VariableSymbol(Token id, Type type) {
		super(id, type);
	}

	public void writeImport(PrintWriter pw) {
		super.writeImport(pw);
	}

    public String toString() {
    	return getName()+":"+getType();
    }

}
