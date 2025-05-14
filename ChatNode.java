// ChatNode.java - Interface for chat node operations
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ChatNode extends Remote {
    // Basic chat operations
    void receiveMessage(ChatMessage message) throws RemoteException;
    
    // Election algorithm operations
    void startElection() throws RemoteException;
    void electionMessage(int senderID) throws RemoteException;
    void coordinatorMessage(int coordinatorID) throws RemoteException;
    
    // Node management
    int getNodeID() throws RemoteException;
    boolean isAlive() throws RemoteException;
    
    // Get node status information
    NodeInfo getNodeInfo() throws RemoteException;
}