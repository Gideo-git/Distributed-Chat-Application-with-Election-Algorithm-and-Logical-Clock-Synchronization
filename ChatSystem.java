
public class ChatSystem {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java ChatSystem <nodeId> <nodeName>");
            System.exit(1);
        }

        int nodeId = Integer.parseInt(args[0]);
        String nodeName = args[1];

        try {
            ChatNode node = new ChatNodeImpl(nodeId, nodeName);
            node.start();
        } catch (Exception e) {
            System.err.println("Chat system error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}