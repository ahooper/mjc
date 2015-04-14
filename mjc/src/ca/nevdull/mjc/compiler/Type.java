package ca.nevdull.mjc.compiler;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface Type {
    public String getName();

    public void writeImportTypeContent(DataOutput out)
            throws IOException;

    public Type readImportTypeContent(DataInput in)
            throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException;
}