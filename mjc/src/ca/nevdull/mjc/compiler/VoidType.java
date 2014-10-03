package ca.nevdull.mjc.compiler;

public class VoidType extends Type {
	// void is not really a type, but we pretend
	private static final VoidType INSTANCE = new VoidType();
	 
    private VoidType() {}
 
    public static VoidType getInstance() {
        return INSTANCE;
    }

    public String toString() {
    	return "void";
    }
}
