package ca.nevdull.cob.compiler;

import java.util.LinkedHashMap;
import java.util.Map;

import org.antlr.v4.runtime.Token;

public class BaseScope implements Scope {
	Scope enclosingScope;
	String name;
	LinkedHashMap<String,Symbol> members = new LinkedHashMap<String,Symbol>();
	
	public BaseScope(String name, Scope enclosingScope) {
		this.enclosingScope = enclosingScope;
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public Scope getEnclosingScope() {
		return enclosingScope;
	}

	public Map<String, Symbol> getMembers() {
		return members;
	}
	
	@Override
	public Symbol find(String name) {
		Symbol s = members.get(name);
		if (s != null) return s;
		if (enclosingScope == null) return null;  // not found
		return enclosingScope.find(name);
	}
	
	public void add(Symbol symbol) {
		String name = symbol.getName();
		Symbol s = members.get(name);
		if (s != null) {
			Token token = symbol.getToken();
			if (token != null) {
				Main.error(token,name+" previously defined in "+getName());
			} else {
				Main.error(name+" previously defined in "+getName());
			}
        	// TODO mark symbols as duplicate definition
			return;
		};
		members.put(name, symbol);
		symbol.setScope(this);
	}
	
    public String toString() {
    	return getName()+members.keySet().toString();
    }

}
