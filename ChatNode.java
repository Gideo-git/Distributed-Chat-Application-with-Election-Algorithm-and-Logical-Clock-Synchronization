import java.rmi.RemoteException;
import java.util.Map;

public interface ChatNode extends java.rmi.Remote {
    void receiveMessage(String sender, String message, int timestamp) throws RemoteException;
    void pingNode() throws RemoteException;
    void electCoordinator(int newCoordinatorId) throws RemoteException;
    void startElection() throws RemoteException;
    void registerNode(int nodeId, String nodeName) throws RemoteException;
    Map<Integer, String> getRegisteredNodes() throws RemoteException;
    int getNodeId() throws RemoteException;
    void start() throws RemoteException;
}