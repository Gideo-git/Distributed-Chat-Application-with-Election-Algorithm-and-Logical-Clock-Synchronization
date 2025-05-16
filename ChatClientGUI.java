import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ChatClientGUI extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton, nodesButton, electionButton;
    private ChatNode nodeStub;
    private int nodeId;
    private String nodeName;
    private int lastMessageCount = 0;

    public ChatClientGUI(int nodeId, String nodeName) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        setTitle("Distributed Chat - Node " + nodeId);
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initComponents();
        connectToNode();
        startMessagePoller();
        setVisible(true);
    }

    private void initComponents() {
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        inputField = new JTextField();
        sendButton = new JButton("Send");
        nodesButton = new JButton("Show Nodes");
        electionButton = new JButton("Start Election");

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(panel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.add(nodesButton);
        controlPanel.add(electionButton);
        bottomPanel.add(controlPanel, BorderLayout.SOUTH);

        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        nodesButton.addActionListener(e -> showNodes());
        electionButton.addActionListener(e -> startElection());
    }

    private void connectToNode() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            nodeStub = (ChatNode) registry.lookup("ChatNode_" + nodeId);
            chatArea.append("[SYSTEM] Connected to node: " + nodeName + "\n");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error connecting to node: " + e.getMessage());
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) return;
        try {
            nodeStub.receiveMessage(nodeName, message, 0); // 0 timestamp will be corrected by node
            inputField.setText("");
        } catch (Exception e) {
            chatArea.append("[ERROR] Failed to send message: " + e.getMessage() + "\n");
        }
    }

    private void showNodes() {
        try {
            Map<Integer, String> nodes = nodeStub.getRegisteredNodes();
            StringBuilder sb = new StringBuilder("[SYSTEM] Registered nodes:\n");
            for (Map.Entry<Integer, String> entry : nodes.entrySet()) {
                sb.append("- Node ").append(entry.getKey()).append(" (" + entry.getValue() + ")\n");
            }
            chatArea.append(sb.toString());
        } catch (Exception e) {
            chatArea.append("[ERROR] Could not fetch nodes: " + e.getMessage() + "\n");
        }
    }

    private void startElection() {
        try {
            nodeStub.startElection();
            chatArea.append("[SYSTEM] Election initiated.\n");
        } catch (Exception e) {
            chatArea.append("[ERROR] Failed to start election: " + e.getMessage() + "\n");
        }
    }

    private void startMessagePoller() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> refreshMessages());
            }
        }, 1000, 2000);
    }

    private void refreshMessages() {
        try {
            // Simulate pulling messages by re-displaying current state of node console (since no direct message history)
            // Ideally, you'd have a shared message log or history API to poll.
            // Here we just prompt to visually reflect that a pull would happen.
            chatArea.append(""); // Placeholder for refreshing logic
        } catch (Exception e) {
            chatArea.append("[ERROR] Failed to refresh messages: " + e.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java ChatClientGUI <nodeId> <nodeName>");
            System.exit(0);
        }
        int id = Integer.parseInt(args[0]);
        String name = args[1];
        SwingUtilities.invokeLater(() -> new ChatClientGUI(id, name));
    }
}
