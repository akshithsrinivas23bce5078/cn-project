import java.io.*;
import java.net.*;
import java.util.*;

class Node extends Thread {
    private int nodeid;
    private int port;
    private ServerSocket serversocket;
    private Map<Integer, Integer> connections;

    public Node(int nodeid, int port) throws IOException {
        this.nodeid = nodeid;
        this.port = port;
        this.serversocket = new ServerSocket(port);
        this.connections = new HashMap<>();
    }

    public void connect(int targetnodeid, int targetport) {
        connections.put(targetnodeid, targetport);
        System.out.println("Node " + nodeid + " connected to Node " + targetnodeid);
    }

    public void sendMessage(int destid, String message) {
        if (!connections.containsKey(destid)) {
            System.out.println("Node " + nodeid + " is not connected to Node " + destid);
            return;
        }
        int destinationport = connections.get(destid);
        System.out.println("Node " + nodeid + " is sending message to Node " + destid);
        try (Socket socket = new Socket("localhost", destinationport);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println("Message from node " + nodeid + ": " + message);
        } catch (IOException e) {
            System.out.println("Failed to send message: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        System.out.println("Node " + nodeid + " is listening on port " + port);
        while (true) {
            try (Socket socket = serversocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String message = in.readLine();
                System.out.println("Node " + nodeid + " received: " + message);
            } catch (IOException e) {
                System.out.println("Error in node " + nodeid + ": " + e.getMessage());
            }
        }
    }

    public int getNodeId() {
        return nodeid;
    }

    public int getPort() {
        return port;
    }
}

public class CNPROJECTREVIEW2_23BCE5078 {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        List<Node> nodes = new ArrayList<>();
        
        System.out.println("Enter the baseport:");
        int baseport = scan.nextInt();
        scan.nextLine(); // Consume the leftover newline

        // Creating nodes
        for (int i = 1; i <= 30; i++) {
            try {
                nodes.add(new Node(i, baseport + i));
            } catch (IOException e) {
                System.out.println("Error creating Node " + i + ": " + e.getMessage());
                return;
            }
        }

        // Start all nodes
        for (Node node : nodes) {
            node.start();
        }
        System.out.println("All 30 nodes created!!! Now specify connections:");

        // Connection setup
        while (true) {
            System.out.println("Enter connection (source-destination), or type 'over' to end:");
            String input = scan.nextLine().trim();
            if (input.equalsIgnoreCase("over")) break;
            String[] parts = input.split("-");
            if (parts.length != 2) {
                System.out.println("Invalid format. Use source-destination");
                continue;
            }
            try {
                int src = Integer.parseInt(parts[0].trim());
                int dest = Integer.parseInt(parts[1].trim());
                if (src < 1 || src > 30 || dest < 1 || dest > 30) {
                    System.out.println("Invalid node id. Must be between 1 to 30");
                    continue;
                }
                nodes.get(src - 1).connect(dest, nodes.get(dest - 1).getPort());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter numbers!!");
            }
        }

        // Message sending
        while (true) {
            System.out.println("Enter message (source-destination-message), or type 'exit' to end:");
            String input = scan.nextLine().trim();
            if (input.equalsIgnoreCase("exit")) break;
            String[] parts = input.split("-", 3);
            if (parts.length != 3) {
                System.out.println("Invalid format. Use source-destination-message");
                continue;
            }
            try {
                int src = Integer.parseInt(parts[0].trim());
                int dest = Integer.parseInt(parts[1].trim());
                String message = parts[2].trim();
                if (src < 1 || src > 30 || dest < 1 || dest > 30) {
                    System.out.println("Invalid node id. Must be between 1 to 30");
                    continue;
                }
                nodes.get(src - 1).sendMessage(dest, message);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter numbers!!");
            }
        }
        scan.close();
    }
}