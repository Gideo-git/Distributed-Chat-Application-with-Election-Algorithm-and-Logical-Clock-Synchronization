// README.md
/*
# Distributed Chat System with Election Algorithm and Logical Clock Synchronization

This project implements a distributed chat system with the following features:
- RMI-based client-server communication
- Bully election algorithm for coordinator selection
- Lamport logical clocks for message ordering
- Basic fault tolerance for coordinator failure

## Components

1. `ChatNode.java` - Interface for chat node operations
2. `ChatMessage.java` - Class representing messages with logical timestamps
3. `NodeInfo.java` - Class to hold node status information
4. `ChatNodeImpl.java` - Implementation of the ChatNode interface
5. `ChatSystem.java` - Main class for the distributed chat system

## How to Compile and Run

1. Compile all files:
   ```
   javac *.java
   ```

2. Start nodes (open different terminals for each node):
   ```
   java ChatSystem <nodeID> <nodeName>
   ```
   For example:
   ```
   # Terminal 1
   java ChatSystem 1 Alice
   
   # Terminal 2
   java ChatSystem 2 Bob
   
   # Terminal 3
   java ChatSystem 3 Charlie
   ```

3. Use the available commands to interact with the system:
   - `help` - Show help message
   - `nodes` - List all nodes in the system
   - `info` - Show information about this node
   - `history` - Show message history
   - `log` - Show system log
   - `election` - Force an election process
   - `join <nodeID>` - Join a specific node
   - `exit` or `quit` - Exit the application
   - `<message>` - Send a chat message

## How It Works

### Node Discovery
When a node starts, it registers itself in the RMI registry and discovers other nodes.

### Bully Election Algorithm
The bully algorithm is used to elect a coordinator node:
1. When a node discovers there's no coordinator, it starts an election
2. It sends election messages to all nodes with higher IDs
3. If no node with a higher ID responds, it becomes the coordinator
4. The coordinator broadcasts its status to all nodes

### Lamport Logical Clocks
Logical timestamps are used to maintain message ordering:
1. Each node maintains a logical clock counter
2. When sending a message, the sender increments its counter and attaches the new value
3. When receiving a message, the receiver sets its counter to max(local, received) + 1
4. Messages are ordered based on their logical timestamps

### Message Broadcasting
The coordinator node is responsible for broadcasting messages:
1. When a node sends a message, it sends it to the coordinator
2. The coordinator updates the logical timestamp and broadcasts to all other nodes
3. If the coordinator fails, a new election process starts

### Fault Tolerance
The system handles node failures:
1. If a node cannot reach the coordinator, it starts an election
2. If a node detects the coordinator is down during message sending, it starts an election
3. Nodes are removed from the registry when they fail

## Future Enhancements
1. Implement a more efficient message broadcasting mechanism
2. Add persistent storage for messages
3. Improve fault tolerance with redundancy
4. Add security features like authentication and encryption
*/