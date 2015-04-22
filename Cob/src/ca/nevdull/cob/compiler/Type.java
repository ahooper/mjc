package ca.nevdull.cob.compiler;

// Representation of language types
// The specializations include primitive, class, array, and unknown

import java.io.PrintWriter;

public interface Type {

	// Write the type to the class import file
	public void writeImportType(PrintWriter pw);

	public String getNameString();

	public String getArrayString();
}
