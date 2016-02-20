package ca.nevdull.mjc1.compiler.llvm;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class Function {
    private String prefix;
    private String suffix;
    private String name;
    private FunctionType type;
    private String[] parameterNames;
    private List<BasicBlock> basicBlockList = new ArrayList<BasicBlock>();
    
    private int counter = 0;

    public Function(String prefix, String suffix, String name, FunctionType type, String ... parameterNames) {
        this.name = name;
        this.type = type;
        if (parameterNames == null || parameterNames.length == 0 && type.getParameterTypes().length > 0) {
            parameterNames = new String[type.getParameterTypes().length];
            for (int i = 0; i < parameterNames.length; i++) {
                parameterNames[i] = "p" + i;
            }
        }
        this.parameterNames = parameterNames;
     }
    
    public String getName() {
		return name;
	}

	public FunctionType getType() {
		return type;
	}

	public String[] getParameterNames() {
		return parameterNames;
	}

	public int nextCounter() {
		return ++counter;
	}

	public void write(Writer writer) throws IOException {
        Type returnType = type.getReturnType();
        Type[] parameterTypes = type.getParameterTypes();
        writer.write("define ");
        if (prefix != null) {
            writer.write(prefix);
            writer.write(' ');
        }
        writer.write(returnType.toString());
        writer.write(" @\"");
        writer.write(name);
        writer.write("\"(");
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                writer.write(", ");
            }
            writer.write(parameterTypes[i].toString());
            writer.write(" %");
            writer.write(parameterNames[i]);
        }
        if (type.isVarArgs()) {
            writer.write(", ...");
        }
        writer.write(")");
        if (suffix != null) {
            writer.write(' ');
            writer.write(suffix);
        }
        writer.write(" {\n");
        for (BasicBlock bb : basicBlockList) {
            bb.write(writer);
        }
        writer.write("}\n");
    }
}
