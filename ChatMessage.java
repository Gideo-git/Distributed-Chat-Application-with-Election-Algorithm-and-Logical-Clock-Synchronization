// ChatMessage.java - Class representing messages with logical timestamps
import java.io.Serializable;

public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String content;
    private final String sender;
    private final int senderID;
    private int logicalTimestamp;
    
    public ChatMessage(String content, String sender, int senderID, int logicalTimestamp) {
        this.content = content;
        this.sender = sender;
        this.senderID = senderID;
        this.logicalTimestamp = logicalTimestamp;
    }
    
    public String getContent() {
        return content;
    }
    
    public String getSender() {
        return sender;
    }
    
    public int getSenderID() {
        return senderID;
    }
    
    public int getLogicalTimestamp() {
        return logicalTimestamp;
    }
    
    public void setLogicalTimestamp(int timestamp) {
        this.logicalTimestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "[" + logicalTimestamp + "] " + sender + ": " + content;
    }
}

