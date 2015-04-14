package ca.nevdull.mjc.compiler;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.antlr.v4.runtime.Token;

public class VariableSymbol extends Symbol {

    public VariableSymbol(Token token, Type type, Scope currentScope) {
    	super(token, currentScope);
		setType(type);
	}

    public String toString() {
    	return getName()+":"+getType();
    }

    public void writeImport(DataOutput out)
            throws IOException {
    	super.writeImport(out);
    }
	
    public VariableSymbol() {
    }

    public void readImport(DataInput in)
            throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
    	super.readImport(in);  // Symbol
    }

}
