package ca.nevdull.mjc.compiler;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class UnknownType extends Symbol implements Type {
	
	private static final UnknownType INSTANCE = new UnknownType();
	 
    private UnknownType() {
    	super("unknown", null);
    }
 
    public static UnknownType getInstance() {
        return INSTANCE;
    }
    
	public String toString() {
		return "unknown";
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
