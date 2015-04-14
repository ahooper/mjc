package ca.nevdull.mjc.compiler;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

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

    public void writeImportTypeContent(DataOutput out)
            throws IOException {
    }

    public Type readImportTypeContent(DataInput in)
            throws IOException {
    	// only one instance
    	return getInstance();  // discards current instance
    }
}
