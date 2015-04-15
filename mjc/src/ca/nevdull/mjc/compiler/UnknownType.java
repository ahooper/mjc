package ca.nevdull.mjc.compiler;

import java.io.PrintWriter;

public class UnknownType extends Symbol implements Type {
	
	private static final UnknownType INSTANCE = new UnknownType();
	 
    private UnknownType() {
    	super("Unknown", null);
    }
 
    public static UnknownType getInstance() {
        return INSTANCE;
    }
    
	public String toString() {
		return "Unknown";
	}

	public void writeImportType(PrintWriter pw) {
		pw.append(name);
	}
}
