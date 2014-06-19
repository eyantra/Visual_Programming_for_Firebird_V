package slcodeblocks;

import codeblocks.Block;

import static slcodeblocks.SLCommand.*;
import static slcodeblocks.SLBlockProperties.isCmd;

/**
 * Checks that output blocks only appear in a procedure stack.
 */
public class OutputStackRule implements CompilerRule
{
    /**
     * Applies to output blocks.
     */
    public boolean match(Long blockID) {
        return isCmd(CMD_OUTPUT, Block.getBlock(blockID));
    }
    
    /**
     * Find the top level block. Throws a compiler exception if top-level
     * is not a procedure block.
     */
    public Long apply(Long blockID) throws CompilerException {
        Block b = Block.getBlock(blockID);
        while (b.getBeforeBlockID() != Block.NULL) {
            // Don't allow output blocks in certain stacks.
            b = Block.getBlock(b.getBeforeBlockID());
            if (isCmd(CMD_ASK_AGENTS, b) || isCmd(CMD_ASK_TURTLE, b)) {
                throw new CompilerException(
                    CompilerException.Error.OUTPUT_STACK, blockID, "ask-agent");
            }
            if (isCmd(CMD_HATCH_DO, b)) {
                throw new CompilerException(
                    CompilerException.Error.OUTPUT_STACK, blockID, "hatch-do");
            }
        }
        
        if (!b.isProcedureDeclBlock()) {
            throw new CompilerException(
                CompilerException.Error.OUTPUT_STACK, blockID, "");
        }
        
        return blockID;
    }
}
