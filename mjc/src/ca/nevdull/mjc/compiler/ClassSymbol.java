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

public class ClassSymbol extends ScopingSymbol implements Scope, Type, java.io.Serializable {
	/**
	 *  
	 */
	private static final long serialVersionUID = -8534718845378057061L;
	
	ClassSymbol superClass;
    Map<String, Symbol> members = new LinkedHashMap<String, Symbol>();
    MethodSymbol constructor = null;

	public ClassSymbol(Token nameToken, Scope enclosingScope, ClassSymbol superClass) {
		super(nameToken, enclosingScope);
		this.superClass = superClass;
	}

	public ClassSymbol(String nameString, Scope enclosingScope, ClassSymbol superClass) {
		super(nameString, enclosingScope);
		this.superClass = superClass;
	}

	public ClassSymbol getSuperClass() {
		return superClass;
	}

	public void setSuperClass(ClassSymbol superClass) {
		this.superClass = superClass;
	}

	public MethodSymbol getConstructor() {
		return constructor;
	}

	public void setConstructor(MethodSymbol method) {
		this.constructor = method;
	}

	@Override
	public Scope getParentScope() {
        if (superClass == null) return enclosingScope;
        return superClass;
    }

	@Override
	public Map<String, Symbol> getMembers() {
		return members;
	}

	@Override
	public ClassSymbol getInheritance() {
		return superClass;
	}

    public String toString() {
    	return "class "+getName()+":"+members.values();
    }

}
