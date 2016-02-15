package ca.nevdull.mjc1.compiler.llvm;

import java.util.ArrayList;
import java.util.List;

public class Instruction extends Operand {
	protected String resultName;
	protected BasicBlock basicBlock;
	protected List<Metadata> metadata;

	public Instruction(Type type) {
		super(type);
	}

	public void setBasicBlock(BasicBlock basicBlock) {
		this.basicBlock = basicBlock;
		this.resultName = String.format("%%r%d", basicBlock.getFunction().getCounter());
	}

	public String getResultName() {
		if (resultName != null) return resultName;
		return String.format("%%%X", System.identityHashCode(this));
	}

	@Override
	public String asOperand() {
		return getResultName();
	}

	public List<Metadata> getMetadata() {
		return metadata;
	}
    
    public Instruction addMetadata(Metadata md) {
        if (metadata == null) {
            metadata = new ArrayList<>();
        }
        metadata.add(md);
        return this;
    }
}
