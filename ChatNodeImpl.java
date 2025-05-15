import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.util.*;
import java.util.concurrent.*;

public class ChatNodeImpl implements ChatNode {
    private int nodeId;
    private String nodeName;
    private int logicalClock = 0;
    private int coordinatorId;
    private Map<Integer, String> registeredNodes = new ConcurrentHashMap<>();
    private Map<Integer, ChatNode> nodeStubs = new ConcurrentHashMap<>();
    private Registry registry;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private int messageCounter = 0;

    public ChatNodeImpl(int nodeId, String nodeName) throws RemoteException {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.coordinatorId = nodeId; // Initially assume self as coordinator
        this.registeredNodes.put(nodeId, nodeName);
        
        System.out.println("[SYSTEM] Node initialized with ID " + nodeId + " and name " + nodeName);
    }

    @Override
    public void start() throws RemoteException {
        try {
            // Try to create a new registry
            try {
                registry = LocateRegistry.createRegistry(1099);
                System.out.println("Created new RMI registry on port 1099");
            } catch (RemoteException e) {
                // Registry already exists
                registry = LocateRegistry.getRegistry(1099);
                System.out.println("Using existing RMI registry on port 1099");
            }

            // Register this node
            ChatNode stub = (ChatNode) UnicastRemoteObject.exportObject(this, 0);
            registry.rebind("ChatNode_" + nodeId, stub);
            System.out.println("Node registered as: ChatNode_" + nodeId);

            // Discover existing nodes
            discoverNodes();

            // Start health check for coordinator
            scheduler.scheduleAtFixedRate(this::checkCoordinator, 5, 5, TimeUnit.SECONDS);

            // Start user input processing
            startUserInputProcessing();

        } catch (Exception e) {
            System.err.println("Node startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void discoverNodes() {
        try {
            String[] nodes = registry.list();
            for (String node : nodes) {
                if (node.startsWith("ChatNode_") && !node.equals("ChatNode_" + nodeId)) {
                    try {
                        ChatNode remoteNode = (ChatNode) registry.lookup(node);
                        int remoteId = remoteNode.getNodeId();
                        
                        if (remoteId != nodeId) {
                            Map<Integer, String> remoteNodes = remoteNode.getRegisteredNodes();
                            for (Map.Entry<Integer, String> entry : remoteNodes.entrySet()) {
                                int id = entry.getKey();
                                String name = entry.getValue();
                                if (id != nodeId) {
                                    registeredNodes.put(id, name);
                                    System.out.println("[SYSTEM] Registered node with ID " + id + " (" + name + ")");
                                    
                                    // Get stub for this node for future communication
                                    if (!nodeStubs.containsKey(id)) {
                                        ChatNode stub = (ChatNode) registry.lookup("ChatNode_" + id);
                                        nodeStubs.put(id, stub);
                                    }
                                }
                            }
                            
                            // Register with the remote node
                            remoteNode.registerNode(nodeId, nodeName);
                        }
                    } catch (Exception e) {
                        System.err.println("Error connecting to node " + node + ": " + e.getMessage());
                    }
                }
            }
            
            // Start election if other nodes were found
            if (registeredNodes.size() > 1) {
                startElection();
            }
        } catch (Exception e) {
            System.err.println("Error discovering nodes: " + e.getMessage());
        }
    }

    @Override
    public void registerNode(int nodeId, String nodeName) throws RemoteException {
        if (!registeredNodes.containsKey(nodeId)) {
            registeredNodes.put(nodeId, nodeName);
            System.out.println("[SYSTEM] Registered node with ID " + nodeId + " (" + nodeName + ")");
            
            // Get stub for this node for future communication
            try {
                ChatNode stub = (ChatNode) registry.lookup("ChatNode_" + nodeId);
                nodeStubs.put(nodeId, stub);
            } catch (NotBoundException e) {
                System.err.println("Error getting stub for node " + nodeId + ": " + e.getMessage());
            }
        }
    }

    @Override
    public Map<Integer, String> getRegisteredNodes() throws RemoteException {
        return new HashMap<>(registeredNodes);
    }

    @Override
    public int getNodeId() throws RemoteException {
        return nodeId;
    }

    @Override
    public void receiveMessage(String sender, String message, int timestamp) throws RemoteException {
        // Update logical clock
        logicalClock = Math.max(logicalClock, timestamp) + 1;
        
        // Print received message with logical timestamp
        messageCounter++;
        System.out.println("[" + messageCounter + "] " + sender + ": " + message);
    }

    @Override
    public void pingNode() throws RemoteException {
        // Simple method to check if node is alive
    }

    private void checkCoordinator() {
        if (coordinatorId != nodeId) {
            try {
                ChatNode coordinator = nodeStubs.get(coordinatorId);
                if (coordinator != null) {
                    coordinator.pingNode();
                } else {
                    // Try to get the coordinator stub
                    try {
                        ChatNode stub = (ChatNode) registry.lookup("ChatNode_" + coordinatorId);
                        nodeStubs.put(coordinatorId, stub);
                        stub.pingNode();
                    } catch (Exception e) {
                        System.out.println("[SYSTEM] Coordinator not responding, starting election");
                        try {
                            startElection();
                        } catch (RemoteException re) {
                            System.err.println("Error starting election: " + re.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("[SYSTEM] Coordinator not responding, starting election");
                try {
                    startElection();
                } catch (RemoteException re) {
                    System.err.println("Error starting election: " + re.getMessage());
                }
            }
        }
    }

    @Override
    public void startElection() throws RemoteException {
        System.out.println("[SYSTEM] Starting election process");
        
        boolean higherNodeExists = false;
        
        // Check if there are nodes with higher IDs
        for (int id : registeredNodes.keySet()) {
            if (id > nodeId) {
                higherNodeExists = true;
                try {
                    ChatNode node = nodeStubs.get(id);
                    if (node == null) {
                        // Try to get the node stub
                        try {
                            node = (ChatNode) registry.lookup("ChatNode_" + id);
                            nodeStubs.put(id, node);
                        } catch (Exception e) {
                            System.err.println("Error connecting to node " + id + ": " + e.getMessage());
                            continue;
                        }
                    }
                    
                    // Send election message to higher node
                    node.startElection();
                } catch (Exception e) {
                    // Node might be down, remove it
                    System.err.println("Error connecting to node " + id + ": " + e.getMessage());
                    nodeStubs.remove(id);
                    registeredNodes.remove(id);
                }
            }
        }
        
        // If no higher node exists or responds, this node becomes coordinator
        if (!higherNodeExists) {
            becomeCoordinator();
        }
    }

    private void becomeCoordinator() {
        System.out.println("[SYSTEM] This node is now the coordinator");
        coordinatorId = nodeId;
        
        // Notify all other nodes about the new coordinator
        for (int id : registeredNodes.keySet()) {
            if (id != nodeId) {
                try {
                    ChatNode node = nodeStubs.get(id);
                    if (node == null) {
                        try {
                            node = (ChatNode) registry.lookup("ChatNode_" + id);
                            nodeStubs.put(id, node);
                        } catch (Exception e) {
                            System.err.println("Error connecting to node " + id + ": " + e.getMessage());
                            continue;
                        }
                    }
                    node.electCoordinator(nodeId);
                } catch (Exception e) {
                    // Node might be down, remove it
                    System.err.println("Error notifying node " + id + " about new coordinator: " + e.getMessage());
                    nodeStubs.remove(id);
                    registeredNodes.remove(id);
                }
            }
        }
    }

    @Override
    public void electCoordinator(int newCoordinatorId) throws RemoteException {
        this.coordinatorId = newCoordinatorId;
        System.out.println("[SYSTEM] Coordinator set to node " + newCoordinatorId);
    }

    private void startUserInputProcessing() {
        System.out.println("Chat node started. Type 'help' for commands.");
        
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine();
            
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting chat system...");
                System.exit(0);
            } else if (input.equalsIgnoreCase("help")) {
                System.out.println("Available commands:");
                System.out.println("  help - Display this help message");
                System.out.println("  exit - Exit the chat system");
                System.out.println("  nodes - List all registered nodes");
                System.out.println("  election - Start a new coordinator election");
                System.out.println("  Any other text will be sent as a chat message");
            } else if (input.equalsIgnoreCase("nodes")) {
                System.out.println("Registered nodes:");
                for (Map.Entry<Integer, String> entry : registeredNodes.entrySet()) {
                    System.out.println("  Node " + entry.getKey() + " (" + entry.getValue() + ")" + 
                                      (entry.getKey() == coordinatorId ? " (coordinator)" : ""));
                }
            } else if (input.equalsIgnoreCase("election")) {
                try {
                    startElection();
                } catch (Exception e) {
                    System.err.println("Error starting election: " + e.getMessage());
                }
            } else {
                // Increment logical clock for new message
                logicalClock++;
                
                // Process as chat message
                broadcastMessage(input, logicalClock);
            }
        }
    }

    private void broadcastMessage(String message, int timestamp) {
        // Send message to all nodes including self to maintain message ordering
        for (int id : registeredNodes.keySet()) {
            try {
                ChatNode node = id == nodeId ? this : nodeStubs.get(id);
                
                if (node == null && id != nodeId) {
                    // Try to get the node stub
                    try {
                        node = (ChatNode) registry.lookup("ChatNode_" + id);
                        nodeStubs.put(id, node);
                    } catch (Exception e) {
                        System.err.println("Error connecting to node " + id + ": " + e.getMessage());
                        continue;
                    }
                }
                
                node.receiveMessage(nodeName, message, timestamp);
            } catch (Exception e) {
                System.err.println("Error sending message to node " + id + ": " + e.getMessage());
                
                // If the error is with the coordinator, start an election
                if (id == coordinatorId && id != nodeId) {
                    System.out.println("[SYSTEM] Coordinator not responding, starting election");
                    try {
                        startElection();
                    } catch (RemoteException ex) {
                        System.err.println("Error starting election: " + ex.getMessage());
                    }
                }
                
                // Remove failed node
                nodeStubs.remove(id);
                registeredNodes.remove(id);
            }
        }
    }
}