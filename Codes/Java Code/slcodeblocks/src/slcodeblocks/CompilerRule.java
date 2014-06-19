package slcodeblocks;

public interface CompilerRule {
	
	public boolean match(Long blockID);
	public Long apply(Long blockID) throws CompilerException;
	
}
