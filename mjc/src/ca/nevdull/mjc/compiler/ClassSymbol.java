package ca.nevdull.mjc.compiler;

import java.util.LinkedHashMap;
import java.util.Map;

import org.antlr.v4.runtime.Token;

public class ClassSymbol extends ScopingSymbol implements Scope {
	ClassSymbol superClass;
    Map<String, Symbol> members = new LinkedHashMap<String, Symbol>();

	public ClassSymbol(Token nameToken, Scope enclosingScope, ClassSymbol superClass) {
		super(nameToken, enclosingScope);
		this.superClass = superClass;
	}

	@Override
	public void define(Symbol sym) {
        members.put(sym.name, sym);
        sym.scope = this; // track the scope in each symbol
	}

	@Override
	public Symbol resolve(String name) {
        Symbol s = members.get(name);
        if (s != null) return s;
        // if not here, check the parent class
        if (superClass != null ) {
            return superClass.resolve(name);
        }
        return null; // not found
	}

	public Map<String, Symbol> getMembers() {
		return members;
	}

    public String toString() {
    	return "class "+getName()+":"+members.values();
    }

}
