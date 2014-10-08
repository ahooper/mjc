package ca.nevdull.mjc.compiler;

public class VoidType extends Symbol implements Type {
	// void is not really a type, but we pretend
	private static final VoidType INSTANCE = new VoidType();
	 
    private VoidType() {
    	super("void");
    }
 
    public static VoidType getInstance() {
        return INSTANCE;
    }

    public String toString() {
    	return "void";
    }
}
