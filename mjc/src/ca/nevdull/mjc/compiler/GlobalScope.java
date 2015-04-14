package ca.nevdull.mjc.compiler;

/***
 * Excerpted from "The Definitive ANTLR 4 Reference",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
***/
public class GlobalScope extends BaseScope {

	public GlobalScope() {
    	super(null);
    }
    
    public String getScopeName() {
    	return "globals";
    }
}
