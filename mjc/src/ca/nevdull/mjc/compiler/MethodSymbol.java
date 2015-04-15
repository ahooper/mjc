package ca.nevdull.mjc.compiler;

/***
 * Parts excerpted from "Language Implementation Patterns",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpdsl for more book information.
***/

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

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

	public void writeImport(PrintWriter pw) {
		if (isAbstract) pw.append("abstract ");
		super.writeImport(pw);
		pw.append("(");
		String sep = "";
		for (Entry<String, Symbol> paramEnt : parameters.entrySet()) {
			pw.append(sep);  sep = ",";
			paramEnt.getValue().writeImport(pw);
		}
		pw.append(")");
		if (isNative) pw.append(" native");
	}

	public void writeImportType(PrintWriter pw) {
		assert null!="writeImportType should not be called on a method";
	}

}
