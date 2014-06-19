package slcodeblocks;

import codeblocks.Block;
import codeblocks.LinkRule;
import codeblocks.BlockConnector;

/**
 * 
 * <code>MonitorRule</code> specifies the Starlogo TNG specific rules for connecting monitors to blocks
 */
public class MonitorRule implements LinkRule{

    public boolean canLink(Block block1, Block block2, BlockConnector socket1, BlockConnector socket2) {
        if((block1.getProperty(SLBlockProperties.IS_MONITOR) != null && block2.getProperty(SLBlockProperties.IS_MONITORABLE) != null) || 
                block2.getProperty(SLBlockProperties.IS_MONITOR) != null && block1.getProperty(SLBlockProperties.IS_MONITORABLE) != null){
            //block1 or block2 is a monitor, while the other is monitorable: satifies monitor linking rule
            return true;
        }
        else if (block1.getProperty(SLBlockProperties.IS_MONITOR) == null &&
                block2.getProperty(SLBlockProperties.IS_MONITOR) == null){
        		//neither block is a monitor, so it's ok to link
        		return true;
        }
        return false;
    }

    public boolean isMandatory() {
    		return true;
    }
}
