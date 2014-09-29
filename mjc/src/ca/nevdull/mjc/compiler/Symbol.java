package ca.nevdull.mjc.compiler;

import org.antlr.v4.runtime.Token;

public class Symbol {
	String name;
    Token token;		// token for definition source position
	Type type;
    Scope scope;      // All symbols know what scope contains them.
	Access access;

    public Symbol(String name) {
    	this.name = name;
    	this.token = null;
    }

    public Symbol(Token nameToken) {
    	this(nameToken.getText());
    	this.token = nameToken;
    }
    
    public String getName() {
    	return name;
    }
    
    public Type getType() {
    	return type;
    }
    
    public Scope getScope() {
    	return scope;
    }
    
    public Access getAccess() {
    	return access;
    }
}
