package ca.nevdull.mjc.compiler;

import java.io.PrintWriter;

public interface Type {
    public String getName();

	public void writeImportType(PrintWriter pw);
}