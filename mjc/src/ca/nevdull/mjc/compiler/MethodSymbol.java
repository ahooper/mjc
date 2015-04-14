package ca.nevdull.mjc.compiler;

/***
 * Parts excerpted from "Language Implementation Patterns",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpdsl for more book information.
***/

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.antlr.v4.runtime.Token;

public class MethodSymbol extends ScopingSymbol implements Scope, Type {
	
	Map<String, Symbol> parameters = new LinkedHashMap<String, Symbol>();
	boolean isAbstract = false;
	boolean isNative = false;

	public MethodSymbol(Token nameToken, Scope enclosingScope) {
		super(nameToken, enclosingScope);
	}

	@Override
	public Map<String, Symbol> getMembers() {
		return parameters;
	}

    public String toString() {
    	return "method "+getScopeName();//+":"+parameters.values();//+":"+getType();
    }

	@Override
	public ScopingSymbol getInheritance() {
		// TODO Auto-generated method stub
		return null;
	}

    public void writeImport(DataOutput out)
            throws IOException {
    	super.writeImport(out);
        out.writeBoolean(isAbstract);
        out.writeBoolean(isNative);
    	out.writeInt(parameters.size());
    	for (String parameterName : parameters.keySet()) {
    		Symbol parameter = parameters.get(parameterName);
    		parameter.writeImport(out);
    	}
    }
	
	public MethodSymbol() {
    	// for readImport
    }

    public void readImport(DataInput in)
            throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
    	super.readImport(in);  // ScopingSymbol
    	isAbstract = in.readBoolean();
    	isNative = in.readBoolean();
    	for (int n = in.readInt(); n-- > 0; ) {
    		Symbol parameter = (Symbol)Symbol.readImportNewInstance(in);
    		parameter.readImport(in);
    		parameters.put(parameter.getName(), parameter);
    	}
    }

    public void writeImportTypeContent(DataOutput out)
            throws IOException {
    }

	@Override
	public Type readImportTypeContent(DataInput in) throws IOException {
		return null;
	}

}
