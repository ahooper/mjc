package ca.nevdull.mjc1.compiler;

import java.util.LinkedHashMap;

import org.antlr.v4.runtime.Token;

public class Scope extends Symbol {
	LinkedHashMap<String,Symbol> symbols;
	Scope enclosingScope;

	public Scope(String name, Scope enclosingScope) {
		super(name);
		symbols = new LinkedHashMap<String,Symbol>(10);
		this.enclosingScope = enclosingScope;
	}

	public Scope(Token name, Scope enclosingScope) {
		super(name);
		symbols = new LinkedHashMap<String,Symbol>(10);
		this.enclosingScope = enclosingScope;
	}
	
	public Scope getEnclosingScope() {
		return enclosingScope;
	}

	public void add(Symbol symbol) {
		Symbol prev = symbols.get(symbol.name);
		if (prev != null) {
			if (symbol.token != null) Main.error(symbol.token,"multiple definition of "+symbol.name);
			else Main.error("multiple definition of "+symbol.name);
			if (prev.token != null) Main.note(prev.token,"is previous definition");
		} else {
			symbol.setDefiningScope(this);
			symbols.put(symbol.name,symbol);
		}
	}

	public Symbol find(String name) {
		Symbol r = symbols.get(name);
		if (r == null && enclosingScope != null) return enclosingScope.find(name);
		return r;
	}
	
	public Symbol find(Token name) {
		return find(name.getText());
	}
	
	public Scope close() {
		Main.debug("end %s %s", name, symbols.keySet().toString());
		return enclosingScope;
	}

}
