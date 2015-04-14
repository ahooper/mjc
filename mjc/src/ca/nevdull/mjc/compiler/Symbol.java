package ca.nevdull.mjc.compiler;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

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

    public void writeImport(DataOutput out)
            throws IOException {
		out.writeUTF(this.getClass().getSimpleName());		
    	out.writeUTF(name);
    	if (access != null) out.writeUTF(access.toString());
    	else out.writeUTF("");
        out.writeBoolean(isFinal);
        out.writeBoolean(isStatic);
		writeImportType(type,out);
    }

	public void writeImportType(Type type,DataOutput out) throws IOException {
		if (type == null) {
			out.writeUTF("");
			return;
		}
		out.writeUTF(type.getClass().getSimpleName());		
    	type.writeImportTypeContent(out);
	}
	
    public Symbol() {
    }

    public void readImport(DataInput in)
            throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
    	name = in.readUTF();
    	String an = in.readUTF();
    	if (an.length() == 0) access = null;
    	else access = Access.valueOf(an);
    	isFinal = in.readBoolean();
    	isStatic = in.readBoolean();
    	type = readImportType(in);
    }

	public static Type readImportType(DataInput in)
            throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		Type type = (Type)readImportNewInstance(in);
    	if (type != null) {
    		type = type.readImportTypeContent(in);  // class instance may be discarded	
    	}
    	return type;
	}
    
	static String myPackagePrefix = "ca.nevdull.mjc.compiler.";
	
    public static Object readImportNewInstance(DataInput in)
            throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
    	String className = in.readUTF();
    	if (className.length() == 0) return null;
		Class<?> klass = Class.forName(myPackagePrefix+className);
		System.out.println("Symbol readImportNewInstance "+klass.getTypeName());
		return klass.newInstance();
    }
}
