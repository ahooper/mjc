package ca.nevdull.mjc.compiler;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;

public class ReferenceType extends Type {
	ClassSymbol referredClass;
	Token nameToken;
	static ReferenceType nullReference = new ReferenceType(new CommonToken(Token.INVALID_TYPE,"null"));
										//TODO not certain what to use for Token type here
	
	/**
	 * @param nameToken
	 */
	public ReferenceType(Token nameToken) {
		super();
		this.nameToken = nameToken;
	}

	/**
	 * @param referredClass
	 */
	public void resolveTo(ClassSymbol referredClass) {
		this.referredClass = referredClass;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ReferenceType(" + nameToken.getText() + ")";
	}

}
