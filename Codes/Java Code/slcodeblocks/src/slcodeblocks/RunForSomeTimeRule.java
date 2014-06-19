package slcodeblocks;

import codeblocks.Block;
import codeblocks.BlockConnector;
import codeblocks.LinkRule;

/**
 * 
 * Rule to prohibit non-global numbers to be hooked up to the seconds socket of 
 * the run for some time block.
 * 
 * TODO: Should make this into a more generic exception rather than specific rule
 * 
 */

public class RunForSomeTimeRule implements LinkRule {

	public boolean canLink(Block block1, Block block2, BlockConnector socket1,
			BlockConnector socket2) {
		if(block1.getProperty(SLBlockProperties.STACK_TYPE) != null && block2.getProperty(SLBlockProperties.STACK_TYPE) != null){
			if((block1.getGenusName().equals(SLGenus.RUNFORSOMETIME) 
					&& block2.getProperty(SLBlockProperties.STACK_TYPE).equals(SLBlockProperties.STACK_BREED) 
					&& socket1.getKind().equals(SLBlockProperties.KIND_NUMBER))
					|| (block1.getProperty(SLBlockProperties.STACK_TYPE).equals(SLBlockProperties.STACK_BREED) 
							&& block2.getGenusName().equals(SLGenus.RUNFORSOMETIME) 
							&& socket2.getKind().equals(SLBlockProperties.KIND_NUMBER))){
				return false;
			}
		}
		return true;
	}

	public boolean isMandatory() {
		return true;
	}

}
