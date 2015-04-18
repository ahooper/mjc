package ca.nevdull.cob.compiler;

import java.io.PrintWriter;
import java.util.Map.Entry;

import org.antlr.v4.runtime.Token;

public class MethodSymbol extends ScopingSymbol {

	public MethodSymbol(Token id, Scope enclosingScope, Type type) {
		super(id, enclosingScope, type);
		// TODO Auto-generated constructor stub
	}

    public String toString() {
    	return "method "+getName();//+":"+members.values();//+":"+getType();
    }

 	public void writeImport(PrintWriter pw) {
 		super.writeImport(pw);
		pw.append("(");
		String sep = "";
		for (Entry<String, Symbol> paramEnt : getMembers().entrySet()) {
			pw.append(sep);  sep = ",";
			paramEnt.getValue().writeImport(pw);
		}
		pw.append(")");
    }
}
