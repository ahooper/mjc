package ca.nevdull.cob.compiler;

// Representation of a named class type

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.antlr.v4.runtime.Token;

public class ClassSymbol extends ScopingSymbol implements Type {
	
	ClassSymbol base;
	private boolean isAutoImport;
	LinkedHashMap<String,Symbol> expanded_members = null;

	public ClassSymbol(Token id, Scope enclosingScope, ClassSymbol base) {
		super(id, enclosingScope, null);
		setType(this);
		this.base = base;
	}
	
	public ClassSymbol getBase() {
		return base;
	}

	public void setBase(ClassSymbol base) {
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

	@Override
	public String getNameString() {
		return getName();
	}

	@Override
	public String getArrayString() {
		return "";
	}

	public boolean getAutoImport() {
		return this.isAutoImport;	
	}

	public void setAutoImport(boolean autoImport) {
		this.isAutoImport = autoImport;	
	}
	
	public void computeExpandedMembers() {
		if (expanded_members != null) return;  // previously computed
		if (base != null) {
			base.computeExpandedMembers();
			expanded_members = new LinkedHashMap<String,Symbol>(base.expanded_members);
		} else {
			expanded_members = new LinkedHashMap<String,Symbol>();
		}
		for (Entry<String, Symbol> ent : members.entrySet()) {
			expanded_members.put(ent.getKey(), ent.getValue());
		}
	}

	public Map<String, Symbol> getExpandedMembers() {
		computeExpandedMembers();
		return expanded_members;
	}
}
