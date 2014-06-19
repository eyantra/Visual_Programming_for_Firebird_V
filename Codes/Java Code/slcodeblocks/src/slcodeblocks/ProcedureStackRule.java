package slcodeblocks;

import java.util.HashSet;
import java.util.Set;

import codeblocks.Block;
import codeblocks.BlockConnector;
import codeblocks.BlockStub;

/**
 * Checks that any commands in procedure stacks do not contain outdated
 * arguments (ie. arguments that were removed).
 */
public class ProcedureStackRule implements CompilerRule
{
	/**
	 * Applies to procedure declarations.
	 */
	public boolean match(Long blockID) {
		return Block.getBlock(blockID).isProcedureDeclBlock();
	}

	/**
	 * Checks that any arguments used in the connected stack are attached
	 * to the procedure block. If true, then returns the original blockID.
	 * Otherwise, this procedure throws a CompilerException.
	 */
	public Long apply(Long blockID) throws CompilerException {
		Block b = Block.getBlock(blockID);

		// Set of block ids for the arguments used in thie proc
		Set<Long> args = new HashSet<Long>();
		for (BlockConnector arg : b.getSockets()) {
			args.add(arg.getBlockID());
		}

		checkArgs(args, Block.getBlock(b.getAfterBlockID()));
		return blockID;
	}

	/**
	 * Loop through sockets and attached blocks, checking whether proc
	 * arguments are present in the declaration block. Returns true if
	 * all blocks are valid; false otherwise.
	 */
	private static boolean checkArgs(final Set<Long> args, Block b)
	throws CompilerException
	{
		if (b == null) return true;

		if (b instanceof BlockStub) {
			Block parent = ((BlockStub) b).getParent();
			if (parent != null){
				if (parent.isProcedureParamBlock()) {
					// Must be contained in our parameters; this cannot
					// be for another procedure
					return args.contains(parent.getBlockID());
				}
			}
		}

		for (BlockConnector conn : b.getSockets()) {
			if (!checkArgs(args, Block.getBlock(conn.getBlockID()))) {
				String arg = Block.getBlock(conn.getBlockID()).getBlockLabel() +
				" in socket of " + b.getBlockLabel() + " block";
				throw new CompilerException(CompilerException.Error.INVALID_ARG,
						b.getBlockID(), arg);
			}
		}

		return checkArgs(args, Block.getBlock(b.getAfterBlockID()));
	}
}
