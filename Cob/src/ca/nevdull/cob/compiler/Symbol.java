package ca.nevdull.cob.compiler;

// Representation of a named program symbol
// There are specializations for classes, methods, and variables (which includes
// fields, method arguments, and local variables.

import java.io.PrintWriter;

import org.antlr.v4.runtime.Token;

public class Symbol {
	Token token;
	String name;
	Type type;
	Scope scope;
	boolean isStatic;
	
	public Symbol(String name, Type type) {
		this.name = name;
		this.type = type;
	}
	
	public Symbol(Token id, Type type) {
		this.token = id;
		this.name = token.getText();
		this.type = type;
	}
	
	public Token getToken() {
		return token;
	}

	public String getName() {
		return name;
	}
	
	public Type getType() {
		return type;
	}
	
	public void setType(Type type) {
		this.type = type;
	}
	
	public Scope getScope() {
		return scope;
	}

	public void setScope(Scope scope) {
		this.scope = scope;	
	}

	public boolean isStatic() {
		return isStatic;
	}

	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}

	public void writeImport(PrintWriter pw) {
		type.writeImportType(pw);
		pw.append(' ').append(name);
	}

}
