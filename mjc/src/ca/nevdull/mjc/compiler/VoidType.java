package ca.nevdull.mjc.compiler;

import java.io.PrintWriter;

public class VoidType extends Symbol implements Type {

	// void is not really a type, but we pretend
	private static final VoidType INSTANCE = new VoidType();
	 
    private VoidType() {
    	super("void", null);
    }
 
    public static VoidType getInstance() {
        return INSTANCE;
    }

    public String toString() {
    	return "void";
    }

	public void writeImportType(PrintWriter pw) {
		pw.append(name);
	}
}
