package slcodeblocks;

import java.util.List;
import java.util.Map;

import slcodeblocks.BreedManager.BreedInfo;

import workspace.NetworkEvent;

/**
 * This class contains factory methods to create and interpret NetworkEvents.
 */
public class NetworkEventHandler {

    // The event types.
    private static final byte POSITION_REQUEST = 1;
    private static final byte POSITION_REPLY = 2;
	private static final byte AGENTS_MOVED = 3;
	private static final byte BREED_UPDATED = 4;
	private static final byte KEYS_UPDATED = 5;
	private static final byte BREED_STATUS = 6;
	private static final byte BREED_SUMMARY = 7;
	
	// Some constants.
	private static final Byte REPLY_YES = Byte.valueOf((byte) 1);
	private static final Byte REPLY_NO = Byte.valueOf((byte) 0);
	
	/** The network manager we respond to. */
	private final SLNetworkManager myManager;
	
	NetworkEventHandler(SLNetworkManager manager) { myManager = manager; }
	
	// Network Event Handler ===========================
	
	boolean processEvent(NetworkEvent event) {
	    switch (event.getEventType()) {
	    case AGENTS_MOVED:
	        BreedManager.receiveAgents((long[]) event.getData(0),
	                                   (long[]) event.getData(1),
	                                   (Long[]) event.getData(2),
	                                   (String[]) event.getData(3));
	        return true;
	        
	    case POSITION_REQUEST:
	        myManager.positionRequested(event.getSrcHostId(),
	                                    (Integer) event.getData(0));
	        return true;
	        
	    case POSITION_REPLY:
	        boolean answer = ((Byte) event.getData(0)).equals(REPLY_YES);
	        myManager.positionReplied(event.getSrcHostId(), 
	                                  answer,
	                                  (Integer) event.getData(1));
	        return true;
	        
	    case BREED_UPDATED:
	        BreedManager.updateBreed((Long) event.getData(0),
	                                 (String) event.getData(1),
	                                 (String) event.getData(2),
	                                 (String) event.getData(3));
	        
            // Pass on the message to all of our connected parties.
            WorkspaceController.getNetworkManager().sendEvent(event);
	        return true;
	        
	    case BREED_STATUS:
	        boolean add = ((Boolean) event.getData(0));
	        if (add) {
	            BreedManager.importBreed((Long) event.getData(1),
	                                     (String) event.getData(2),
	                                     (String) event.getData(3));
	        }
	        else {
	            BreedManager.deleteBreed((Long) event.getData(1),
	                                     (String) event.getData(2));
	        }
	        
	        // Pass on the message to all of our connected parties.
	        WorkspaceController.getNetworkManager().sendEvent(event);
	        return true;
	        
	    case BREED_SUMMARY:
	        Long host = (Long) event.getData(0);
	        SLNetworkManager mgr = WorkspaceController.getNetworkManager();
	        
	        // We don't want to import our own breeds!
	        if (host.longValue() == mgr.getServerHash())
	            return true;
	        
	        int len = (Integer) event.getData(1);
	        int count = 2;
	        for (int i = 0; i < len; i++) {
	            String name = (String) event.getData(count++);
	            String shape = (String) event.getData(count++);
	            BreedManager.importBreed(host, name, shape);
	        }
	        
	        // Pass on the message to all of our connected parties.
	        mgr.sendEvent(event);
	        return true;
	        
        default:
            return false;
	    }
	}
	
    // NetworkEvents constructors =========================
    
    /** Pass in the new agent heap data and destination host. */
    static NetworkEvent agentsMoved(long[] agentHeap) {
        Map<Long, BreedInfo> breedMap = BreedManager.getExportBreedMap();
        long[] slnums = new long[breedMap.size()];
        Long[] hosts = new Long[breedMap.size()];
        String[] names = new String[breedMap.size()];
        int i = 0;
        for (Long slnum : breedMap.keySet()) {
            slnums[i] = slnum;
            BreedInfo info = breedMap.get(slnum);
            hosts[i] = info.host;
            names[i] = info.name;
            i++;
        }
        return new NetworkEvent(AGENTS_MOVED, agentHeap, slnums, hosts, names);
    }
    
    /** Pass in the key heap for the host that has changed. */
    static NetworkEvent keysUpdated(byte[] keyHeap) {
        return new NetworkEvent(KEYS_UPDATED, keyHeap);
    }
    
    /** 
     * Request that the given host connect at the given position relative to
     * the RECEIVER. For example, if host 1 wants host 2 at NORTH, host 1 
     * would send an event (POSITION_REQUEST, SOUTH). 
     */
    static NetworkEvent positionRequest(int direction) {
        return new NetworkEvent(POSITION_REQUEST, direction);
    }
    
    /** Reply to a position request. */
    static NetworkEvent positionReply(boolean accepted, int direction) {
        return new NetworkEvent(POSITION_REPLY, 
            accepted ? REPLY_YES : REPLY_NO, direction);
    }
    
    /** Breed change. */
    static NetworkEvent breedUpdated(String oldName, String newName, String shape) {
        return new NetworkEvent(BREED_UPDATED, 
            WorkspaceController.getNetworkManager().getServerHash(), 
            oldName, newName, shape);
    }
    
    /** Breed addition. */
    static NetworkEvent breedAdded(String name, String shape) {
        return new NetworkEvent(BREED_STATUS, Boolean.TRUE, 
            WorkspaceController.getNetworkManager().getServerHash(), 
            name, shape);
    }
    
    /** Breed deletion. */
    static NetworkEvent breedDeleted(String name) {
        return new NetworkEvent(BREED_STATUS, Boolean.FALSE, 
            WorkspaceController.getNetworkManager().getServerHash(), 
            name);
    }
    
    /** Breed summary package. */
    static NetworkEvent breedSummary(Long id, List<String> names, List<String> shapes) {
        assert names.size() == shapes.size();
        int len = names.size();
        
        // The data looks like: id, len, name1, shape1, name2, shape2, ...
        Object[] data = new Object[len * 2 + 2];
        data[0] = id;
        data[1] = len;
        int counter = 2;
        for (int i = 0; i < len; i++) {
            data[counter++] = names.get(i);
            data[counter++] = shapes.get(i);
        }
        
        return new NetworkEvent(BREED_SUMMARY, data);
    }
}
