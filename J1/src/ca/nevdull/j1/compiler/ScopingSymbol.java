package ca.nevdull.j1.compiler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.Token;

public class ScopingSymbol extends Symbol implements Scope {
	Scope enclosingScope;
	Map<String,Symbol> members = new LinkedHashMap<String,Symbol>();

	public ScopingSymbol(Scope enclosingScope, Token name) {
		super(name);
		this.enclosingScope = enclosingScope;
	}

	public ScopingSymbol(Scope enclosingScope, String name) {
		super(name);
		this.enclosingScope = enclosingScope;
	}

	@Override
	public void define(Symbol sym) {
		Symbol prev = members.get(sym.name);
		if (prev != null) {
			if (sym.token != null) Compiler.error(sym.token,"multiple definition of "+sym.name);
			else Compiler.error("multiple definition of "+sym.name);
			if (prev.token != null) Compiler.note(prev.token,"is previous definition");
		} else {
			members.put(sym.name,sym);
			sym.setScope(this);
			sym.location = location;
			location += 1;
		}
	}

	public Symbol resolveMember(String name) {
		return members.get(name);
	}

	@Override
	public Symbol resolve(String name) {
		//Compiler.debug(getName()+" resolve "+name);
		Symbol sym = resolveMember(name);
		if (sym != null) return sym;
		if (enclosingScope != null) return enclosingScope.resolve(name);
		return null;
	}

	@Override
	public Scope getEnclosingScope() {
		return enclosingScope;
	}

	public Set<String> getMemberNames() {
		return members.keySet();
	}

	public String toString() {
		return getName()+members.keySet().toString() ;
	}

	public String getName() {
		return getClass().getSimpleName();
	}

}
