package ca.nevdull.mjc.compiler;

public class UnknownType extends Symbol implements Type {

	private static final long serialVersionUID = 142879997187116054L;
	
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
}
