// ChatSystem.java - Main class for the distributed chat system
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ChatSystem {
    private static final int RMI_PORT = 1099;
    private static final String RMI_NAME_PREFIX = "ChatNode_";
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java ChatSystem <nodeID> <nodeName>");
            System.exit(1);
        }
        
        try {
            int nodeID = Integer.parseInt(args[0]);
            String nodeName = args[1];
            
            // Create and export the chat node
            ChatNodeImpl node = new ChatNodeImpl(nodeID, nodeName);
            
            // Create or get the RMI registry
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(RMI_PORT);
                System.out.println("Created new RMI registry on port " + RMI_PORT);
            } catch (RemoteException e) {
                registry = LocateRegistry.getRegistry(RMI_PORT);
                System.out.println("Using existing RMI registry on port " + RMI_PORT);
            }
            
            // Bind this node to the registry
            String nodeBind = RMI_NAME_PREFIX + nodeID;
            registry.rebind(nodeBind, node);
            System.out.println("Node registered as: " + nodeBind);
            
            // Discover other nodes in the registry
            discoverAndRegisterNodes(registry, node);
            
            // User interface
            System.out.println("Chat node started. Type 'help' for commands.");
            Scanner scanner = new Scanner(System.in);
            boolean running = true;
            
            while (running) {
                String input = scanner.nextLine().trim();
                
                if (input.equals("quit") || input.equals("exit")) {
                    running = false;
                } else if (input.equals("help")) {
                    showHelp();
                } else if (input.equals("nodes")) {
                    listRegisteredNodes(registry);
                } else if (input.equals("info")) {
                    showNodeInfo(node);
                } else if (input.equals("history")) {
                    showMessageHistory(node);
                } else if (input.equals("log")) {
                    showSystemLog(node);
                } else if (input.equals("election")) {
                    node.startElection();
                    System.out.println("Election process started");
                } else if (input.startsWith("join ")) {
                    String[] parts = input.split(" ", 2);
                    if (parts.length > 1) {
                        int targetID = Integer.parseInt(parts[1]);
                        joinNode(registry, node, targetID);
                    }
                } else {
                    // Treat as a chat message
                    node.sendMessage(input);
                }
            }
            
            scanner.close();
            System.out.println("Chat node shutting down");
            
        } catch (Exception e) {
            System.err.println("Chat system error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void discoverAndRegisterNodes(Registry registry, ChatNodeImpl localNode) {
        try {
            String[] boundNames = registry.list();
            for (String name : boundNames) {
                if (name.startsWith(RMI_NAME_PREFIX)) {
                    try {
                        int nodeID = Integer.parseInt(name.substring(RMI_NAME_PREFIX.length()));
                        if (nodeID != localNode.getNodeID()) {
                            ChatNode remoteNode = (ChatNode) registry.lookup(name);
                            localNode.registerNode(nodeID, remoteNode);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore entries with invalid format
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error discovering nodes: " + e.getMessage());
        }
    }
    
    private static void joinNode(Registry registry, ChatNodeImpl localNode, int targetID) {
        try {
            String targetName = RMI_NAME_PREFIX + targetID;
            ChatNode remoteNode = (ChatNode) registry.lookup(targetName);
            
            if (remoteNode != null) {
                localNode.registerNode(targetID, remoteNode);
                System.out.println("Joined node with ID " + targetID);
            }
        } catch (Exception e) {
            System.err.println("Error joining node: " + e.getMessage());
        }
    }
    
    private static void listRegisteredNodes(Registry registry) {
        try {
            System.out.println("\nRegistered Nodes:");
            String[] boundNames = registry.list();
            
            Map<Integer, String> nodes = new HashMap<>();
            for (String name : boundNames) {
                if (name.startsWith(RMI_NAME_PREFIX)) {
                    try {
                        int nodeID = Integer.parseInt(name.substring(RMI_NAME_PREFIX.length()));
                        nodes.put(nodeID, name);
                    } catch (NumberFormatException e) {
                        // Ignore entries with invalid format
                    }
                }
            }
            
            if (nodes.isEmpty()) {
                System.out.println("No nodes found in registry");
            } else {
                for (Map.Entry<Integer, String> entry : nodes.entrySet()) {
                    try {
                        ChatNode node = (ChatNode) registry.lookup(entry.getValue());
                        NodeInfo info = node.getNodeInfo();
                        System.out.println("Node " + info.getNodeID() + " (" + info.getNodeName() + 
                                          ") - Coordinator: " + info.isCoordinator() +
                                          ", Logical Clock: " + info.getLogicalClock());
                    } catch (Exception e) {
                        System.out.println("Node " + entry.getKey() + " - Connection error");
                    }
                }
            }
            System.out.println();
        } catch (Exception e) {
            System.err.println("Error listing nodes: " + e.getMessage());
        }
    }
    
    private static void showNodeInfo(ChatNodeImpl node) {
        try {
            NodeInfo info = node.getNodeInfo();
            System.out.println("\nNode Information:");
            System.out.println("ID: " + info.getNodeID());
            System.out.println("Name: " + info.getNodeName());
            System.out.println("Is Coordinator: " + info.isCoordinator());
            System.out.println("Logical Clock: " + info.getLogicalClock());
            System.out.println();
        } catch (Exception e) {
            System.err.println("Error showing node info: " + e.getMessage());
        }
    }
    
    private static void showMessageHistory(ChatNodeImpl node) {
        System.out.println("\nMessage History:");
        for (ChatMessage message : node.getMessageHistory()) {
            System.out.println(message);
        }
        System.out.println();
    }
    
    private static void showSystemLog(ChatNodeImpl node) {
        System.out.println("\nSystem Log:");
        for (String logEntry : node.getSystemLog()) {
            System.out.println(logEntry);
        }
        System.out.println();
    }
    
    private static void showHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  help           - Show this help message");
        System.out.println("  nodes          - List all nodes in the system");
        System.out.println("  info           - Show information about this node");
        System.out.println("  history        - Show message history");
        System.out.println("  log            - Show system log");
        System.out.println("  election       - Force an election process");
        System.out.println("  join <nodeID>  - Join a specific node");
        System.out.println("  exit/quit      - Exit the application");
        System.out.println("  <message>      - Send a chat message");
        System.out.println();
    }
}