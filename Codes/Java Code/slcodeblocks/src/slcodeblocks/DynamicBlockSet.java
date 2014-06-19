package slcodeblocks;

/**
 * DynamicBlockSets are a collection of BlockGenuses that are generated dynamically.  In other words, 
 * the specification BlockGenuses are done during runtime.  Each of the methods below return 
 * the XML String specifications.  DynamicBlockSets must be registered with the BlocksGenerator 
 * in order for each set to be properly loaded into the application.  
 */
public interface DynamicBlockSet {

    /**
     * Returns the String XML specification of BlockGenuses for this set.
     * @return the String XML specification of BlockGenuses for this set.
     */
    public String getGenuses();
    
    /**
     * Returns the String XML specification of BlockFamilies for this set.  
     * @return the String XML specification of BlockFamilies for this set. 
     */
    public String getFamilies();
    
    /**
     * Returns the String XML specification indicating the block drawer(s) that 
     * the blocks in this set belong to.
     * @return the String XML specification indicating the block drawer(s) that 
     * the blocks in this set belong to.
     */
    public String getBlockDrawerMembership();
    
}
