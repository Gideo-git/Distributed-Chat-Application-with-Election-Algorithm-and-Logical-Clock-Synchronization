// NodeInfo.java - Class to hold node status information
import java.io.Serializable;

public class NodeInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final int nodeID;
    private final String nodeName;
    private final boolean isCoordinator;
    private final int logicalClock;
    
    public NodeInfo(int nodeID, String nodeName, boolean isCoordinator, int logicalClock) {
        this.nodeID = nodeID;
        this.nodeName = nodeName;
        this.isCoordinator = isCoordinator;
        this.logicalClock = logicalClock;
    }
    
    public int getNodeID() {
        return nodeID;
    }
    
    public String getNodeName() {
        return nodeName;
    }
    
    public boolean isCoordinator() {
        return isCoordinator;
    }
    
    public int getLogicalClock() {
        return logicalClock;
    }
}