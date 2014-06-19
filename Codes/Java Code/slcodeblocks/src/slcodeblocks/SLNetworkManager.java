package slcodeblocks;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import workspace.NetworkEvent;
import workspace.NetworkManager;

/**
 * As currently implemented, each host <i>connection</i> is identified by its
 * socket. Therefore, if 2 hosts are connected in more than 1 direction, they
 * will each have 2 entries for the other host. It is safe to assume that a 
 * connected host will be connected in <i>some</i> direction, so if we fail
 * a position request, that socket should be disconnected. 
 */
public class SLNetworkManager extends NetworkManager {
    
    // Directions - hardcoded in the VM.
    private static final int NUM_DIRECTIONS = 4;
    public static final int NORTH = 0;
    public static final int EAST  = 1;
    public static final int SOUTH = 2;
    public static final int WEST  = 3;
	
	private static final int BOUNCE  = -1;
	
	/** We always refer to ourselves as -1. */
	public static final int SELF_HOSTNUM = -1;
	
	/** If actions[i] != BOUNCE, then we pass the turtles to neighbors[i] */
	private final int[] myActions = new int[NUM_DIRECTIONS];
	
	/** The mapping from vm host (int) to network host (long). */
	private final Long[] myNetworkActions = new Long[NUM_DIRECTIONS];
	
	/** The SLBlockObserver we use to talk to the rest of the system. */
	private static SLBlockObserver mySlbo = null;
	
	/** The network listener we use to delegate events to us. */
	private final NetworkEventHandler myEventHandler;
	
	// The SLNetworkManager ==================================
	public SLNetworkManager() {
		super();
		myEventHandler = new NetworkEventHandler(this);
		resetActions();
	}
	
	public static void setObserver(SLBlockObserver slbo) {
	    mySlbo = slbo;
	}
	
	/**
	 * Call this method when loading a project. 
	 */
	void loadServerHash(long hash) {
	    // TODO: Move the loading code directly into this class, so we 
	    // don't need to expose these variables.
	    setServerHash(hash);
	}
	
	// Actions ===============================================
	
    /**
	 * Reset the actions. Call this method when initializing or changing
	 * servers.
	 */
	private void resetActions() {
        for(int i = 0; i < NUM_DIRECTIONS; i++) {
            myActions[i] = BOUNCE;
            myNetworkActions[i] = null;
        }
	}
	
	/** Set our neighbor at the given direction. */
	private void setAction(int dir, Long host) {
	    myActions[dir] = host == null ? BOUNCE : dir;
	    myNetworkActions[dir] = host;
	    updateVM();
	}
	   
    /** Updates the VM on our new neighbors. */
    private void updateVM() {
        mySlbo.setNetworkStatus(true, SELF_HOSTNUM, myActions);
    }
    
    /** Returns the opposite direction. */
    private int getOppositeDir(int dir) {
        return (dir + (NUM_DIRECTIONS / 2)) % NUM_DIRECTIONS;
    }
	
	// Public Connection/Disconnection Calls =============================

    /** Connect to the given host, at the given position. */
	public synchronized boolean connect(String host, int port, int dir) {
	    // Make sure we can accept a host at the given position.
	    if (dir >= NUM_DIRECTIONS || dir < 0) {
	        error("Invalid spaceland direction: " + dir);
	        return false;
	    }
	    if (myNetworkActions[dir] != null) {
	        error("Already connected to " + getHost(myNetworkActions[dir])
	            + " at that position");
	        return false;
	    }
	    
	    if (!establishConnection(host, port))
	        return false;
	    
	    Long hostId;
	    try {
	        InetAddress addr = InetAddress.getByName(host);
	        hostId = getHostIdentifier(addr, port);
	    }
	    catch (UnknownHostException uhe) {
	        // This shouldn't happen, since we've already connected. But
	        // if it does, we can only return false (no handle to host).
	        error("Cannot resolve host: " + host);
	        return false;
	    }

        // Request their new position. The request contains the direction that
	    // THEY see us in.
        if (sendEvent(hostId, NetworkEventHandler.positionRequest(getOppositeDir(dir))))
            return true;
        
        // Disconnect if we are unsuccessful.
        disconnect(hostId);
        return false;
	}
	
	/** Remove the host at the given direction. */
	public void disconnect(int dir) {
	    if (myNetworkActions[dir] == null) {
	        error("Not connected to any host in direction " + dir);
	        return;
	    }
	    
	    disconnect(myNetworkActions[dir]);
	}
	
	/** Clean up any connections we had to the host. */
	protected void disconnected(Long host) {
	    super.disconnected(host);
	    for (int i = 0; i < NUM_DIRECTIONS; i++) {
	        if (host.equals(myNetworkActions[i])) {
	            setAction(i, null);
	            return;
	        }
	    }
	}
	
    // VM makes these calls (through App) ================================

    /** Export the agent data to the given host. */
    public void exportTurtles(int host, long[] turtleHeap) {
        if (myNetworkActions[host] == null) {
            error("Unrecognized host: " + host);
            return;
        }
        
        sendEvent(myNetworkActions[host], 
                  NetworkEventHandler.agentsMoved(turtleHeap));
    }

    /** Export our keystroke data. */
    public void exportKeys(byte[] deltas) {
        sendEvent(NetworkEventHandler.keysUpdated(deltas));
    }
    
    /** Returns all neighboring hosts (any direction that is not BOUNCE). */
    public Iterable<Integer> getNeighbors() {
        List<Integer> list = new ArrayList<Integer>();
        for (int i : myActions) {
            if (i != BOUNCE && !list.contains(i)) list.add(i);
        }
        return list;
    }
    
	// Network Listener ==========================================
	
	@Override
    protected boolean eventReceived(NetworkEvent event) {
	    return super.eventReceived(event) || 
	           myEventHandler.processEvent(event);
    }
    
    /** Add a host at the given position. */
    void positionRequested(Long host, int dir) {
        System.out.println("Position requested: " + host + " in direction " + dir);
        if (myActions[dir] == BOUNCE) {
            String msg = getHost(host) + " requests to be connected at your ";
            switch (dir) {
            case NORTH: msg += "north"; break;
            case SOUTH: msg += "south"; break;
            case EAST: msg += "east"; break;
            case WEST: msg += "west"; break;
            default: // ignore
                System.err.println("Unrecognized dir in position req");
                return;
            }
            msg += ". Accept this connection?";
            
            if (JOptionPane.showConfirmDialog(null, msg, "Network Connection Request", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                setAction(dir, host);
                sendEvent(host, NetworkEventHandler.positionReply(true, getOppositeDir(dir)));
                BreedManager.sendBreedInfo(host);
                // TODO send some sort of connection notification to a listener
                return;
            }
        }
        
        // If it fails for any reason, we've rejected this request.
        sendEvent(host, NetworkEventHandler.positionReply(false, getOppositeDir(dir)));
    }
    
    /** Receive a host's reply to our position request. */
    void positionReplied(Long host, boolean accepted, int dir) {
        System.out.println("Position replied: " + host + (accepted ? "accepted" : "denied") + " request in dir " + dir);
        if (accepted) {
            setAction(dir, host);
            BreedManager.sendBreedInfo(host);
            // TODO Send some sort of notification to a listener.
        }
        else {
            // TODO Send some sort of notification to a listener.
            disconnect(host);
        }
    }
}
