package ca.nevdull.cob.compiler;

// Representation of unknown types, just a single, common instance

import java.io.PrintWriter;
import java.util.HashMap;

import org.antlr.v4.runtime.Token;

public class UnknownType extends ClassSymbol {
	static HashMap<String,UnknownType> cache = new HashMap<String,UnknownType>(); 
	
	private UnknownType(Token id) {
		super(id, null, null);
		setType(this);
		this.base = null;
	}
	
	static UnknownType make(Token id) {
		UnknownType unk = cache.get(id);
		if (unk == null) unk = new UnknownType(id);
		return unk;
	}

	public Symbol findMember(String name) {
		return null;  // no members known
		//LATER  Returning everything unknown can cause a cascade of errors. Instead
		// we could install the name as an UnknownType member.
	}

    public String toString() {
    	return "unknown?"+getName();
    }

 	public void writeImport(PrintWriter pw) {
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

}
