package ca.nevdull.mjc.compiler;

import java.util.LinkedHashMap;
import java.util.Map;

import org.antlr.v4.runtime.Token;

public class MethodSymbol extends ScopingSymbol implements Scope {
	Map<String, Symbol> parameters = new LinkedHashMap<String, Symbol>();

	public MethodSymbol(Token nameToken, Scope enclosingScope) {
		super(nameToken, enclosingScope);
	}

	@Override
	public void define(Symbol sym) {
		parameters.put(sym.name, sym);
        sym.scope = this; // track the scope in each symbol
	}

	@Override
	public Symbol resolve(String name) {
        Symbol s = parameters.get(name);
        if (s != null) return s;
        // if not here, check any enclosing scope
        if (getEnclosingScope() != null) {
            return getEnclosingScope().resolve(name);
        }
        return null; // not found
	}

    public String toString() {
    	return "method "+getScopeName()+":"+parameters.values()+":"+getType();
    }

}
