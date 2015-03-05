package ca.nevdull.j1.compiler;

import org.antlr.v4.runtime.Token;

public class Symbol {
	String name;
	Token token;
	Scope scope;
	Type type;
	int location;

	public Symbol(String name) {
		this.name = name;
	}

	public Symbol(Token token) {
		this.name = token.getText();
		this.token = token;
	}
	
	public String getName() {
		return this.name;
	}

	public void setType(Type type) {
		Compiler.debug(getName()+" setType "+type);
		this.type = type;
	}

	public Type getType() {
		return this.type;
	}

	public Scope getScope() {
		return scope;
	}

	public void setScope(Scope scope) {
		this.scope = scope;
	}

	public Token getToken() {
		return token;
	}
}
