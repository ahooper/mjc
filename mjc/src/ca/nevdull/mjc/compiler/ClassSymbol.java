package ca.nevdull.mjc.compiler;

/***
 * Excerpted from "Language Implementation Patterns",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpdsl for more book information.
***/

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.antlr.v4.runtime.Token;

public class ClassSymbol extends ScopingSymbol implements Scope, Type {
	
	ClassSymbol superClass;
    Map<String, Symbol> members = new LinkedHashMap<String, Symbol>();
    MethodSymbol constructor = null;
    boolean isAbstract = false;
    boolean isBuiltin = false;

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

	public void writeImport(PrintWriter pw) {
		//TODO global reference list
		if (access != Access.DEFAULT) pw.append(access.toString()).append(' ');
		if (isFinal) pw.append("final ");
		if (isStatic) pw.append("static ");
		if (isAbstract) pw.append("abstract ");
		pw.append("class ").append(name);
		if (superClass != null) {
			pw.append(" extends ").append(superClass.getName());
		}
		pw.append("{\n");
		for (Entry<String, Symbol> membEnt : members.entrySet()) {
			pw.append("  ");
			membEnt.getValue().writeImport(pw);
			pw.append(";\n");
		}
		pw.append("}\n");
    }

	public void writeImportType(PrintWriter pw) {
		pw.append(name);
	}
    
}
