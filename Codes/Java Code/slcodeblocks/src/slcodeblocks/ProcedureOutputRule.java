package slcodeblocks;

import codeblocks.Block;
import codeblocks.BlockConnector;

/**
 * Checks that a procedure contains only 1 type of output block, and that
 * if any branch contains an output, all branches contain an output.
 */
public class ProcedureOutputRule implements CompilerRule
{    
    /** The terminator return type. */
    private final static String VOID = "void";
    
    /**
     * Applies to procedure declarations.
     */
    public boolean match(Long blockID) {
        return Block.getBlock(blockID).isProcedureDeclBlock();
    }
    
    /**
     * <pre>Checks the following:
     *   1. If a branch contains an Output, all branches contain an Output.
     *   2. All outputs in a proc return the same type.
     */
    public Long apply(Long blockID) throws CompilerException {
        check(null, Block.getBlock(blockID));
        return blockID;
    }
    
    /**
     * Traverses the stack, setting the output status and checking that it
     * is valid at each block. Returns the last block in the stack (using
     * getAfterBlockID only).
     */
    private static String check(String outputType, Block b) 
        throws CompilerException 
    {
        // This is an output. Check that output types match.
        if (SLBlockProperties.isCmd(SLCommand.CMD_OUTPUT, b)) {
            if (outputType == VOID) {
                throw new CompilerException(CompilerException.Error.CUSTOM,
                    b.getBlockID(),
                    "Void procedures cannot return values");
            }
            if (outputType != null && 
                !outputType.equals(b.getSocketAt(0).getKind())) 
            {
                throw new CompilerException(CompilerException.Error.CUSTOM,
                    b.getBlockID(), 
                    "All outputs in a procedure must return the same type");
            }
            return b.getSocketAt(0).getKind();
        }
        
        // This is a TERMINATOR (like 'exit proc'). Make sure there is no 
        // output type.
        if (!b.hasAfterConnector()) {
            if (outputType != null && !outputType.equals(VOID)) {
                throw new CompilerException(CompilerException.Error.CUSTOM,
                    b.getBlockID(),
                    "All branches of this procedure must output a value");
            }
            return VOID;
        }
        
        // Check the block's sockets first.
        for (BlockConnector conn : b.getSockets()) {
            if (conn.getKind().equals(SLBlockProperties.KIND_CMD) &&
                conn.getBlockID() != Block.NULL) {
                String type = check(outputType, Block.getBlock(conn.getBlockID()));
                if (outputType == null) 
                    outputType = type;
                else if (outputType == VOID && type == null);   // OK
                else if (!outputType.equals(type)) {
                    throw new CompilerException(CompilerException.Error.CUSTOM,
                        b.getBlockID(),
                        "All outputs in a procedure must return the same type");
                }
            }
        }
        
        // This is NOT an output, but it is the end of the mini-stack. 
        if (b.getAfterBlockID() == Block.NULL) {
            if (outputType != null && !outputType.equals(VOID)) {
                throw new CompilerException(CompilerException.Error.CUSTOM,
                    b.getBlockID(), 
                    "All branches of this procedure must output a value");
            }
            return outputType;
        }
        
        // Call the next block.
        return check(outputType, Block.getBlock(b.getAfterBlockID()));
    }
}
