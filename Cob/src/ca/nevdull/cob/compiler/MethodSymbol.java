package ca.nevdull.cob.compiler;

// Representation of a class method

import java.io.PrintWriter;
import java.util.Map.Entry;

import org.antlr.v4.runtime.Token;

public class MethodSymbol extends ScopingSymbol {
	
	boolean isNative;
	private MethodSymbol hides;
	private MethodSymbol overrides;
	private MethodSymbol overloads;

	public MethodSymbol(Token id, Scope enclosingScope, Type type) {
		super(id, enclosingScope, type);
		// TODO Auto-generated constructor stub
	}

    public boolean isNative() {
		return isNative;
	}

	public void setNative(boolean isNative) {
		this.isNative = isNative;
	}

	public String toString() {
    	return "method "+getName();//+":"+members.values();//+":"+getType();
    }

 	public void writeImport(PrintWriter pw) {
 		super.writeImport(pw);
		pw.append("(");
		String sep = "";
		for (Entry<String, Symbol> paramEnt : getMembers().entrySet()) {
			if (sep.length()==0 && paramEnt.getKey().equals("this")) continue; //TODO a cleaner way?
			pw.append(sep);  sep = ",";
			paramEnt.getValue().writeImport(pw);
		}
		pw.append(")");
    }

	public void setHides(MethodSymbol hides) {
		this.hides = hides;
	}

	public MethodSymbol getHides() {
		return hides;
	}

	public void setOverrides(MethodSymbol overrides) {
		this.overrides = overrides;
	}

	public MethodSymbol getOverrides() {
		return overrides;
	}

	public MethodSymbol getOverloads() {
		return overloads;
	}

	public void setOverloads(MethodSymbol overloads) {
		this.overloads = overloads;
	}
}
