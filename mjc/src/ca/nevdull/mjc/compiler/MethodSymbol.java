package ca.nevdull.mjc.compiler;

/***
 * Excerpted from "Language Implementation Patterns",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpdsl for more book information.
***/

import java.util.LinkedHashMap;
import java.util.Map;

import org.antlr.v4.runtime.Token;

public class MethodSymbol extends ScopingSymbol implements Scope {
	Map<String, Symbol> parameters = new LinkedHashMap<String, Symbol>();

	public MethodSymbol(Token nameToken, Scope enclosingScope) {
		super(nameToken, enclosingScope);
	}

	@Override
	public Map<String, Symbol> getMembers() {
		return parameters;
	}

    public String toString() {
    	return "method "+getScopeName()+":"+parameters.values()+":"+getType();
    }

	@Override
	public ScopingSymbol getInheritance() {
		// TODO Auto-generated method stub
		return null;
	}

}
