package slcodeblocks;

import java.util.*;

import codeblocks.Block;
import codeblocks.BlockConnector;

/**
 * This class keeps track of dependencies between procedures and run blocks.
 * The dependency manager depends on RunBlockManager to inform it when 
 * blocks have stopped or started running. 
 */
public class DependencyManager
{
    /** Map from procedure -> run block that calls procedure. */
    private final static Map<Long, Set<Long>> ourProcToBlockMap = new HashMap<Long, Set<Long>>();
    /** Map from procedure -> procedure stack that calls procedure */
    private final static Map<Long, Set<Long>> ourProcToCallerProcMap = new HashMap<Long, Set<Long>>();
    /** Map from run/proc block -> called procedure */
    private final static Map<Long, Set<Long>> ourBlockToProcMap = new HashMap<Long, Set<Long>>();
    
    /** Just an empty set of longs. */
    private final static Set<Long> EMPTY_DEPS = Collections.emptySet();
    
    /**
     * Returns the list of top blocks (stack, runtime, proc) that this given
     * top block needs compiled in order to run. Includes all collision blocks.
     */ 
    public static Set<Long> getDependencies(Long blockID) {
        Set<Long> set = new HashSet<Long>();
        set.add(blockID);
        getDependencies(blockID, set);
        for (Long id : RunBlockManager.getCollisionBlocks()) {
            set.add(id);
        }
        return set;
    }
    
    /** Recurse through the stack structure. */
    private static void getDependencies(Long blockID, Set<Long> set) {
        Block b = Block.getBlock(blockID);
        if (b == null) return;
        for (BlockConnector conn : b.getSockets()) {
            if (conn.hasBlock() && conn.getBlockID() != Block.NULL)
                getDependencies(conn.getBlockID(), set);
        }
        if (b.hasAfterConnector() && b.getAfterBlockID() != Block.NULL)
            getDependencies(b.getAfterBlockID(), set);
        if (SLBlockProperties.isProcedureCall(b)) {
            Block parent = SLBlockProperties.getParent(b);
            if (parent != null) {
                getCalledProcs(parent.getBlockID(), set);
            }
        }
    }
    
    /** Returns the list of procedures that the given proc CALLs. */
    private static void getCalledProcs(Long proc, Set<Long> procs) {
        if (!procs.add(proc) || !ourBlockToProcMap.containsKey(proc)) return;
        for (Long p : ourBlockToProcMap.get(proc)) 
            getCalledProcs(p, procs);
    }
    
    /**
     * Returns the list of run blocks that are currently dependent on 
     * the given procedure. Meaning, currently running and dependent.
     */
    public static Set<Long> getDependentRunBlocks(Long proc) {
        if (!Block.getBlock(proc).isProcedureDeclBlock()) return EMPTY_DEPS;
        Set<Long> r = new HashSet<Long>();
        getDependentRunBlocks(proc, new ArrayList<Long>(), r);
        return r;
    }
    
    /**
     * Pass in an empty list to prevent infinite recursions. This procedure
     * only returns the blocks that are currently running. 
     */
    private static void getDependentRunBlocks(Long proc, List<Long> tmp, 
                                              Set<Long> runBlocks) 
    {
        if (tmp.contains(proc)) return;
        tmp.add(proc);
        
        // Add any run blocks.
        if (ourProcToBlockMap.containsKey(proc)) {
            Set<Long> blocks = ourProcToBlockMap.get(proc);
            for (Long b : blocks) {
                if (RunBlockManager.isBlockRunning(b))
                    runBlocks.add(b);
            }
        }
        
        // Recurse on any proc blocks.
        Set<Long> procs = ourProcToCallerProcMap.get(proc);
        if (procs != null) {
            for (Long id : procs)
                getDependentRunBlocks(id, tmp, runBlocks);
        }
    }
    
    /**
     * Returns true if any running blocks are dependent on the given proc.
     */
    public static boolean isInUse(Long proc) {
        if (SLBlockProperties.isCollision(proc))
            return RunBlockManager.isAnyBlockRunning();
        else
            return isInUse(proc, new ArrayList<Long>());
    }
    
    /**
     * Helper method to ensure no infinite loops.
     */
    private static boolean isInUse(Long proc, List<Long> tmp) {
        if (tmp.contains(proc)) return false;
        tmp.add(proc);
        
        // Add any run blocks.
        if (ourProcToBlockMap.containsKey(proc)) {
            Set<Long> blocks = ourProcToBlockMap.get(proc);
            for (Long b : blocks) {
                if (RunBlockManager.isBlockRunning(b))
                    return true;
            }
        }
        
        // Recurse on any proc blocks.
        Set<Long> procs = ourProcToCallerProcMap.get(proc);
        if (procs != null) {
            for (Long id : procs)
                if (isInUse(id, tmp)) return true;
        }
        
        return false;
    }
    
    /** 
     * Update the dependencies for the whole project.
     */
    public static void updateDependencies() {
        List<Long> tmp = new ArrayList<Long>();
        for (Block b : Block.getAllBlocks()) {
            if (b.isProcedureDeclBlock() ||
                SLBlockProperties.isForeverRunBlock(b.getBlockID())) 
            {
                updateDependencies(b.getBlockID(), b.getBlockID(), tmp);
                tmp.clear();
            }
        }
    }
    
    /**
     * Updates the dependencies for the given block stack. 
     */
    static void updateDependencies(Long topBlockID, Long blockID) 
    {
        updateDependencies(topBlockID, blockID, new ArrayList<Long>());
    }

    /**
     * Visited contains the visited top blocks. This helper method ensures
     * that we don't get into a loop when computing dependencies.
     */
    private static void updateDependencies(
        Long topBlockID, Long blockID, List<Long> visited) 
    {
        if (topBlockID == null || blockID == null ||
            topBlockID.equals(Block.NULL) || blockID.equals(Block.NULL))
        {
            return;
        }

        Block b = Block.getBlock(blockID);
        // if this block doesn't actually exist, we have a problem, but return anyway
        // TODO: this is a patch, not a fix - why are any blocks null at all??
        if (b == null) {
        	System.out.println("Block "+blockID+" doesn't exist!");
        	return;
        }
        
        // If this is the first iteration, clear the existing dependencies
        // and get the appropriate top block.
        boolean isTopProc = Block.getBlock(topBlockID).isProcedureDeclBlock();
        if (topBlockID.equals(blockID)) {
            // We don't want to revisit this block, if we've already seen it.
            if (visited.contains(blockID)) return;
            visited.add(blockID);
            removeAllDeps(blockID);
        }
        
        // If this is a proc caller, add the called proc
        else if (SLBlockProperties.isProcedureCall(b)) {
            Block parent = SLBlockProperties.getParent(b);
            if (parent != null) {
                Long parentID = parent.getBlockID();
                if (isTopProc)
                    addProcDep(topBlockID, parentID);
                else
                    addDep(topBlockID, parentID);
            }
        }
    
        // Iterate through the rest of the set
        for (BlockConnector conn : b.getSockets()) {
            updateDependencies(topBlockID, conn.getBlockID(), visited);
        }

        // Next block.
        if (b.hasAfterConnector())
            updateDependencies(topBlockID, b.getAfterBlockID(), visited);
    }

    /**
     * Add a dependency. Nothing happens if these IDs are equal.
     */
    private static void addDep(Long topBlock, Long procDecl) {
        Set<Long> s = ourProcToBlockMap.get(procDecl);
        if (s == null) {
            s = new HashSet<Long>();
            ourProcToBlockMap.put(procDecl, s);
        }
        s.add(topBlock);
    
        s = ourBlockToProcMap.get(topBlock);
        if (s == null) {
            s = new HashSet<Long>();
            ourBlockToProcMap.put(topBlock, s);
        }
        s.add(procDecl);
    }

    /**
     * Add a dependency when both blocks are procedures.
     */
    private static void addProcDep(Long caller, Long callee) {
        if (caller.equals(callee)) return;
        Set<Long> s = ourProcToCallerProcMap.get(callee);
        if (s == null) {
            s = new HashSet<Long>();
            ourProcToCallerProcMap.put(callee, s);
        }
        s.add(caller);
    
        s = ourBlockToProcMap.get(caller);
        if (s == null) {
            s = new HashSet<Long>();
            ourBlockToProcMap.put(caller, s);
        }
        s.add(callee);
    }

    /**
    * Remove all dependencies that the given block HAS.
    */
    private static void removeAllDeps(Long blockID) {    
        Set<Long> s = ourBlockToProcMap.remove(blockID);
        if (s != null) {
            for (Long dep : s) {
                if (ourProcToBlockMap.containsKey(dep))
                    ourProcToBlockMap.get(dep).remove(blockID);
                if (ourProcToCallerProcMap.containsKey(dep))
                    ourProcToCallerProcMap.get(dep).remove(blockID);
            }
        }
    }
}
