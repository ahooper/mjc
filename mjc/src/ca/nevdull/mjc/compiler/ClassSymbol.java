package ca.nevdull.mjc.compiler;

/***
 * Excerpted from "Language Implementation Patterns",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpdsl for more book information.
***/

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.antlr.v4.runtime.Token;

public class ClassSymbol extends ScopingSymbol implements Scope, Type {
	
	ClassSymbol superClass;
    Map<String, Symbol> members = new LinkedHashMap<String, Symbol>();
    MethodSymbol constructor = null;
    boolean isAbstract = false;
	private final int importVersion = 1;
	// NB: update importVersion, writeImport, and readImport if adding or changing fields!

	public ClassSymbol(Token nameToken, Scope enclosingScope, ClassSymbol superClass) {
		super(nameToken, enclosingScope);
		this.superClass = superClass;
	}

	public ClassSymbol(String nameString, Scope enclosingScope, ClassSymbol superClass) {
		super(nameString, enclosingScope);
		this.superClass = superClass;
	}

	public ClassSymbol getSuperClass() {
		return superClass;
	}

	public void setSuperClass(ClassSymbol superClass) {
		this.superClass = superClass;
	}

	public MethodSymbol getConstructor() {
		return constructor;
	}

	public void setConstructor(MethodSymbol method) {
		this.constructor = method;
	}

	@Override
	public Scope getParentScope() {
        if (superClass == null) return enclosingScope;
        return superClass;
    }

	@Override
	public Map<String, Symbol> getMembers() {
		return members;
	}

	@Override
	public ClassSymbol getInheritance() {
		return superClass;
	}

    public String toString() {
    	return "class "+getName()+":"+members.values();
    }

    public void writeImport(DataOutput out)
            throws IOException {
     	out.writeInt(importVersion);
    	super.writeImport(out);  // ScopingSymbol => Symbol
    	if (superClass != null) superClass.writeImportTypeContent(out);
    	else out.writeUTF("");
    	out.writeBoolean(isAbstract);
    	out.writeInt(members.size());
    	for (String memberName : members.keySet()) {
    		Symbol member = members.get(memberName);
    		assert member.getName().equals(memberName);
    		member.writeImport(out);
    	}
    	if (constructor!=null) {
    		String constructorName = constructor.getName();
    		assert members.containsKey(constructorName);
    		out.writeUTF(constructorName);
    	} else {
    		out.writeUTF("");    		
    	}
    }
	
	public ClassSymbol() {
    	// for readImport
    }

    public void readImport(DataInput in)
            throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
     	int v = in.readInt();
     	if (v > importVersion) throw new IOException("incompatible version "+v+">"+importVersion);
     	else if (v == importVersion) ;
     	else throw new IOException("incompatible version "+v+"<1");
     	String discard = in.readUTF();
     	System.out.println("ClassSymbol readImport discard "+discard);
    	super.readImport(in);  // ScopingSymbol => Symbol
    	superClass = readImportTypeContent(in);
    	isAbstract = in.readBoolean();
    	for (int n = in.readInt(); n-- > 0; ) {
    		Symbol member = (Symbol)Symbol.readImportNewInstance(in);
    		member.readImport(in);
    		members.put(member.getName(), member);
    	}
    	String consName = in.readUTF();
    	if (consName.length() == 0) {
    		constructor = null;
    	} else {
    		Symbol s = members.get(consName);
    		assert s != null;
    		constructor = (MethodSymbol) s;
    	}
    }
    
    public void writeImportTypeContent(DataOutput out)
            throws IOException {
		out.writeUTF(name);  //TODO fully qualified name
    }

    public ClassSymbol readImportTypeContent(DataInput in)
            throws IOException {
    	String n = in.readUTF();
    	if (n.length() == 0) return null;
    	//TODO lookup name in globals and import if not present
    	return null;
    }
    
}
