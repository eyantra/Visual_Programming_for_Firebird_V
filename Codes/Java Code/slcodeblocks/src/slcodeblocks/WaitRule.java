package slcodeblocks;

import codeblocks.Block;
import codeblocks.BlockConnector;
import codeblocks.LinkRule;

/**
 * 
 * Rule that prohibits a wait (yield block) to be hooked up to an ask agent
 * block.
 * 
 * TODO: Should make this into a more generic exception rather than specific rule
 *  
 */

public class WaitRule implements LinkRule {

	public boolean canLink(Block block1, Block block2, BlockConnector socket1,
			BlockConnector socket2) {
		if((block1.getGenusName().equals(SLGenus.ASK_AGENT) && block2.getGenusName().equals(SLGenus.WAIT))
				|| (block1.getGenusName().equals(SLGenus.WAIT) && block2.getGenusName().equals(SLGenus.ASK_AGENT))){
			return false;
		}
		return true;
	}

	public boolean isMandatory() {
		return true;
	}
	

}
