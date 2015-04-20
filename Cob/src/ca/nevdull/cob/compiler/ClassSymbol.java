package ca.nevdull.cob.compiler;

// Representation of a named class type

import java.io.PrintWriter;
import java.util.Map.Entry;

import org.antlr.v4.runtime.Token;

public class ClassSymbol extends ScopingSymbol implements Type {
	
	ClassSymbol base;

	public ClassSymbol(Token id, Scope enclosingScope, ClassSymbol base) {
		super(id, enclosingScope, null);
		setType(this);
		this.base = base;
	}
	
	public Symbol findMember(String name) {
		Symbol s = members.get(name);
		if (s != null) return s;
		if (base == null) return null;  // not found
		return base.findMember(name);
	}

    public String toString() {
    	return "class "+getName()+members.values();
    }

 	public void writeImport(PrintWriter pw) {
		//TODO global reference list
		pw.append("class ").append(name);
		if (base != null) {
			pw.append(" : ").append(base.getName());
		} else {
			pw.append(" : ").append("null");
		}
		pw.append("{\n");
		for (Entry<String, Symbol> membEnt : getMembers().entrySet()) {
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
