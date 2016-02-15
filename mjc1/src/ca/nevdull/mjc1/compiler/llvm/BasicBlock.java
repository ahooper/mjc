package ca.nevdull.mjc1.compiler.llvm;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class BasicBlock {
	private String label;
	private Function function;

    public BasicBlock(Function function, String label) {
        this.function = function;
        this.label = label;
    }

    public BasicBlock(Function function) {
    	this(function, String.format("%%b%d", function.getCounter()));
    }
	
	public String getLabel() {
		return label;
	}
	
    public Function getFunction() {
        return function;
    }
    
    private List<Instruction> instructions = new ArrayList<Instruction>();

    public void add(Instruction instruction) {
        instructions.add(instruction);
        instruction.setBasicBlock(this);
    }

    public void insertBefore(Instruction before, Instruction instruction) {
        instructions.add(instructions.indexOf(before), instruction);
        instruction.setBasicBlock(this);
    }

    public void insertAfter(Instruction after, Instruction instruction) {
        instructions.add(instructions.indexOf(after) + 1, instruction);
        instruction.setBasicBlock(this);
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public Instruction first() {
        if (instructions.isEmpty()) {
            return null;
        }
        return instructions.get(0);
    }

    public Instruction last() {
        if (instructions.isEmpty()) {
            return null;
        }
        return instructions.get(instructions.size() - 1);
    }
    
    public void write(Writer writer) throws IOException {
        writer.write(getLabel());
        writer.write(":\n");
        for (Instruction instruction : instructions) {
        	writer.write("    ");
            writer.write(instruction.toString());
            List<Metadata> metadata = instruction.getMetadata();
            if (!metadata.isEmpty()) {
                for (Metadata md : metadata) {
                	writer.write(", ");
                	writer.write(md.toString());
                }
            }
            writer.write('\n');
        }
    }

}
