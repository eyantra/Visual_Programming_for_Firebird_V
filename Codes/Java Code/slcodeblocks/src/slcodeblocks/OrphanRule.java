package slcodeblocks;

import codeblocks.Block;
import codeblocks.BlockStub;

/**
 * Checks that the parent declarations exist in the workspace.
 */
public class OrphanRule implements CompilerRule
{
    /**
     * Applies to stubs.
     */
    public boolean match(Long blockID) {
        return Block.getBlock(blockID) instanceof BlockStub;
    }
    
    /**
     * Find the declaration block. Throws an exception if parent doesn't exist.
     */
    public Long apply(Long blockID) throws CompilerException {
        Block b = Block.getBlock(blockID);
        Block parent = SLBlockProperties.getParent(b);
        if (parent == null) 
            throw new CompilerException(CompilerException.Error.ORPHAN_STUB, blockID, b.getBlockLabel());
        
        return blockID;
    }
}
