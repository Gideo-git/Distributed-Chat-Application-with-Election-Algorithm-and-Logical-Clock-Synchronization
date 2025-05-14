import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatNodeImpl extends UnicastRemoteObject implements ChatNode {
    private static final long serialVersionUID = 1L;
    
    private final int nodeID;
    private final String nodeName;
    private boolean isCoordinator;
    private int coordinatorID;
    private final AtomicInteger logicalClock;
    private final List<ChatMessage> messageHistory;
    private final ConcurrentHashMap<Integer, ChatNode> nodeRegistry;
    private boolean electionInProgress;
    private final List<String> systemLog;
    
    public ChatNodeImpl(int nodeID, String nodeName) throws RemoteException {
        super();
        this.nodeID = nodeID;
        this.nodeName = nodeName;
        this.isCoordinator = false;
        this.coordinatorID = -1;
        this.logicalClock = new AtomicInteger(0);
        this.messageHistory = new CopyOnWriteArrayList<>();
        this.nodeRegistry = new ConcurrentHashMap<>();
        this.electionInProgress = false;
        this.systemLog = new CopyOnWriteArrayList<>();
        
        logSystemEvent("Node initialized with ID " + nodeID + " and name " + nodeName);
    }
    
    @Override
    public void receiveMessage(ChatMessage message) throws RemoteException {
        // Implement Lamport's logical clock algorithm
        int receivedTimestamp = message.getLogicalTimestamp();
        int newTimestamp = Math.max(logicalClock.get(), receivedTimestamp) + 1;
        logicalClock.set(newTimestamp);
        
        message.setLogicalTimestamp(newTimestamp);
        messageHistory.add(message);
        
        System.out.println(message);
        
        // If this node is the coordinator, broadcast the message to all nodes
        if (isCoordinator) {
            broadcastMessage(message);
        }
    }
    
    @Override
    public void startElection() throws RemoteException {
        if (electionInProgress) {
            return;
        }
        
        electionInProgress = true;
        logSystemEvent("Starting election process");
        
        // Find nodes with higher IDs
        List<Integer> higherNodes = new ArrayList<>();
        for (Integer id : nodeRegistry.keySet()) {
            if (id > nodeID) {
                higherNodes.add(id);
            }
        }
        
        if (higherNodes.isEmpty()) {
            // This node has the highest ID, so it becomes the coordinator
            becomeCoordinator();
            return;
        }
        
        // Send election messages to higher-ID nodes
        boolean responseReceived = false;
        for (Integer higherID : higherNodes) {
            try {
                ChatNode node = nodeRegistry.get(higherID);
                if (node != null && node.isAlive()) {
                    node.electionMessage(nodeID);
                    responseReceived = true;
                }
            } catch (RemoteException e) {
                // Node is likely down, remove it from registry
                nodeRegistry.remove(higherID);
            }
        }
        
        if (!responseReceived) {
            // No higher nodes responded, become coordinator
            becomeCoordinator();
        }
    }
    
    @Override
    public void electionMessage(int senderID) throws RemoteException {
        logSystemEvent("Received election message from node " + senderID);
        
        // Reply to the sender to indicate that a node with higher ID exists
        try {
            ChatNode sender = nodeRegistry.get(senderID);
            if (sender != null && sender.isAlive()) {
                // This sends an "OK" message implicitly
                logSystemEvent("Sending OK message to node " + senderID);
            }
        } catch (RemoteException e) {
            nodeRegistry.remove(senderID);
        }
        
        // Start a new election
        if (!electionInProgress) {
            startElection();
        }
    }
    
    @Override
    public void coordinatorMessage(int coordinatorID) throws RemoteException {
        this.coordinatorID = coordinatorID;
        this.isCoordinator = (nodeID == coordinatorID);
        this.electionInProgress = false;
        
        logSystemEvent("Coordinator set to node " + coordinatorID);
    }
    
    private void becomeCoordinator() {
        isCoordinator = true;
        coordinatorID = nodeID;
        electionInProgress = false;
        
        logSystemEvent("This node is now the coordinator");
        
        // Inform all other nodes about the new coordinator
        for (Integer id : nodeRegistry.keySet()) {
            if (id != nodeID) {
                try {
                    ChatNode node = nodeRegistry.get(id);
                    if (node != null && node.isAlive()) {
                        node.coordinatorMessage(nodeID);
                    }
                } catch (RemoteException e) {
                    nodeRegistry.remove(id);
                }
            }
        }
    }
    
    public void broadcastMessage(ChatMessage message) {
        for (Integer id : nodeRegistry.keySet()) {
            try {
                ChatNode node = nodeRegistry.get(id);
                if (node != null && id != message.getSenderID() && node.isAlive()) {
                    node.receiveMessage(message);
                }
            } catch (RemoteException e) {
                nodeRegistry.remove(id);
                
                // If the coordinator fails, start a new election
                if (id == coordinatorID) {
                    try {
                        startElection();
                    } catch (RemoteException ex) {
                        logSystemEvent("Error starting election: " + ex.getMessage());
                    }
                }
            }
        }
    }
    
    public void registerNode(int id, ChatNode node) {
        nodeRegistry.put(id, node);
        logSystemEvent("Registered node with ID " + id);
        
        // If there's no coordinator yet, start an election
        if (coordinatorID == -1 && !electionInProgress) {
            try {
                startElection();
            } catch (RemoteException e) {
                logSystemEvent("Error starting election: " + e.getMessage());
            }
        }
    }
    
    public void sendMessage(String content) {
        int timestamp = logicalClock.incrementAndGet();
        ChatMessage message = new ChatMessage(content, nodeName, nodeID, timestamp);
        
        try {
            // If there's a coordinator, send the message to it
            if (coordinatorID != -1 && coordinatorID != nodeID) {
                ChatNode coordinator = nodeRegistry.get(coordinatorID);
                if (coordinator != null && coordinator.isAlive()) {
                    coordinator.receiveMessage(message);
                } else {
                    // Coordinator is down, start a new election
                    startElection();
                }
            } else if (isCoordinator) {
                // This node is the coordinator, handle the message directly
                receiveMessage(message);
            } else {
                logSystemEvent("Cannot send message: No coordinator available");
                
                // Start an election if no coordinator exists
                if (!electionInProgress) {
                    startElection();
                }
            }
        } catch (RemoteException e) {
            logSystemEvent("Error sending message: " + e.getMessage());
            
            try {
                // Coordinator might be down, start a new election
                startElection();
            } catch (RemoteException ex) {
                logSystemEvent("Error starting election: " + ex.getMessage());
            }
        }
    }
    
    @Override
    public int getNodeID() throws RemoteException {
        return nodeID;
    }
    
    @Override
    public boolean isAlive() throws RemoteException {
        return true;
    }
    
    @Override
    public NodeInfo getNodeInfo() throws RemoteException {
        return new NodeInfo(nodeID, nodeName, isCoordinator, logicalClock.get());
    }
    
    private void logSystemEvent(String event) {
        String logEntry = "[SYSTEM] " + event;
        systemLog.add(logEntry);
        System.out.println(logEntry);
    }
    
    public List<String> getSystemLog() {
        return Collections.unmodifiableList(systemLog);
    }
    
    public List<ChatMessage> getMessageHistory() {
        return Collections.unmodifiableList(messageHistory);
    }
}