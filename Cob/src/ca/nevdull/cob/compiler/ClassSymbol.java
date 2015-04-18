package ca.nevdull.cob.compiler;

import java.io.PrintWriter;
import java.util.Map.Entry;

import org.antlr.v4.runtime.Token;

public class ClassSymbol extends ScopingSymbol implements Type {
	
	ClassSymbol parent;

	public ClassSymbol(Token id, Scope enclosingScope, ClassSymbol parent) {
		super(id, enclosingScope, null);
		setType(this);
	}

    public String toString() {
    	return "class "+getName()+members.values();
    }

 	public void writeImport(PrintWriter pw) {
		//TODO global reference list
		pw.append("class ").append(name);
		if (parent != null) {
			pw.append(" : ").append(parent.getName());
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
