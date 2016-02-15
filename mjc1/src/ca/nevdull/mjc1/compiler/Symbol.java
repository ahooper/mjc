package ca.nevdull.mjc1.compiler;

import org.antlr.v4.runtime.Token;

public class Symbol {
	String name;
	Type type;
	Token token;
	Scope definingScope;
	boolean isStatic;

	public Symbol(String name) {
		this.name = name;
	}

	public Symbol(Token name) {
		this(name.getText());
		this.token = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Scope getDefiningScope() {
		return definingScope;
	}

	public void setDefiningScope(Scope definingScope) {
		this.definingScope = definingScope;
	}

}
