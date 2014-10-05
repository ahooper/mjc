package ca.nevdull.mjc.compiler;

/***
 * Excerpted from "Language Implementation Patterns",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpdsl for more book information.
***/

import java.util.Map;

import org.antlr.v4.runtime.Token;

public abstract class ScopingSymbol extends Symbol {
	public Scope enclosingScope;

	public ScopingSymbol(Token nameToken, Scope enclosingScope) {
		super(nameToken);
		this.enclosingScope = enclosingScope;
	}

	public String getScopeName() {
		return name;
	}

	public Scope getEnclosingScope() {
    	return enclosingScope;
	}
	
    public Scope getParentScope() {
    	return getEnclosingScope();
    }
    
    public abstract Map<String, Symbol> getMembers();
    
	public void define(Symbol sym) {
		getMembers().put(sym.name, sym);
        sym.scope = (Scope) this; // track the scope in each symbol
	}
	
	public Symbol resolve(String name) {
        Symbol s = getMembers().get(name);
        if (s != null) return s;
        // if not here, check the parent class
        if (getParentScope() != null) {
        	System.out.println(getScopeName()+" resolve parent "+getParentScope().toString());
            return getParentScope().resolve(name);
        }       
        return null; // not found
	}

	public abstract ScopingSymbol getInheritance();

	public Symbol resolveMember(String name) {
        Symbol s = getMembers().get(name);
        if (s != null) return s;
        // if not here, check the parent class
        if (getInheritance() != null ) {
        	System.out.println(getScopeName()+" resolveMember inheritance "+getInheritance().toString());
            return getInheritance().resolveMember(name);
        }
        return null; // not found
	}

}
