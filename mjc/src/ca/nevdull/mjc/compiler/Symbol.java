package ca.nevdull.mjc.compiler;

import java.io.PrintWriter;

import org.antlr.v4.runtime.Token;

public class Symbol {
	
	String name;
    transient Token token;		// token for definition source position
	Type type;
    Scope scope;      // All symbols know what scope contains them.
	Access access;
    boolean isFinal = false;
    boolean isStatic = false;

    public Symbol(String name, Scope scope) {
    	this.name = name;
    	this.token = null;
    	this.scope = scope;
    }

    public Symbol(Token nameToken, Scope scope) {
    	this(nameToken.getText(), scope);
    	this.token = nameToken;
    }
    
    public String getName() {
    	return name;
    }
    
	public Token getToken() {
		return token;
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

	public void setType(Type type) {
		this.type = type;
	}

	public void setScope(Scope scope) {
		this.scope = scope;
	}

	public void setAccess(Access access) {
		this.access = access;
		System.out.println(getName()+" setAccess "+access);
	}

    public String toString() {
        if ( ! (type == null || type instanceof UnknownType) ) return getName()+":"+type.getName();
        return getName();
    }

	public void writeImport(PrintWriter pw) {
		if (access != null && access != Access.DEFAULT) pw.append(access.toString()).append(' ');
		if (isFinal) pw.append("final ");
		if (isStatic) pw.append("static ");
		type.writeImportType(pw);
		pw.append(' ').append(name);
	}
}
