import java.io.*;
import java.net.*;
import java.util.*;

class Node extends Thread {
    private int nodeid;
    private int port;
    private ServerSocket serversocket;
    private Map<Integer, Integer> connections;
    private List<Long> receiveTimestamps = new ArrayList<>();
    private volatile boolean running = true;

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
        long sendTime = System.currentTimeMillis();
        System.out.println("Node " + nodeid + " is sending message to Node " + destid);
        try (Socket socket = new Socket("localhost", destinationport);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println("Message from node " + nodeid + ": " + message + " [sent:" + sendTime + "]");
        } catch (IOException e) {
            System.out.println("Failed to send message: " + e.getMessage());
        }
    }

    public void forwardMessage(List<Node> nodes, List<Integer> path, String message, int currentIndex) {
        if (currentIndex >= path.size() - 1) return;
        int nextNodeId = path.get(currentIndex + 1);
        sendMessage(nextNodeId, message);
        try {
            Thread.sleep(100); // simulate delay
        } catch (InterruptedException ignored) {}
        nodes.get(nextNodeId - 1).forwardMessage(nodes, path, message, currentIndex + 1);
    }

    public void terminate() {
        running = false;
        try {
            serversocket.close();
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public void run() {
        System.out.println("Node " + nodeid + " is listening on port " + port);
        while (running) {
            try (Socket socket = serversocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String message = in.readLine();
                long receiveTime = System.currentTimeMillis();
                receiveTimestamps.add(receiveTime);
                System.out.println("Node " + nodeid + " received at " + receiveTime + ": " + message);
            } catch (IOException e) {
                if (running) {
                    System.out.println("Error in node " + nodeid + ": " + e.getMessage());
                }
            }
        }
        System.out.println("Node " + nodeid + " shutting down.");
    }

    public void printPerformanceStats() {
        if (receiveTimestamps.size() < 2) {
            System.out.println("Not enough messages to compute performance stats.");
            return;
        }
        long totalTime = receiveTimestamps.get(receiveTimestamps.size() - 1) - receiveTimestamps.get(0);
        double throughput = (double) receiveTimestamps.size() / (totalTime / 1000.0);
        List<Long> jitters = new ArrayList<>();
        for (int i = 1; i < receiveTimestamps.size(); i++) {
            jitters.add(receiveTimestamps.get(i) - receiveTimestamps.get(i - 1));
        }
        double avgJitter = jitters.stream().mapToLong(j -> j).average().orElse(0.0);
        System.out.println("Node " + nodeid + " - Avg Jitter: " + avgJitter + " ms, Throughput: " + throughput + " msg/s");
    }

    public int getNodeId() {
        return nodeid;
    }

    public int getPort() {
        return port;
    }
}

class Graph {
    private final int V = 30;
    private int[][] distance;
    private int[][] next;

    public Graph() {
        distance = new int[V][V];
        next = new int[V][V];
        for (int i = 0; i < V; i++) {
            Arrays.fill(distance[i], Integer.MAX_VALUE / 2);
            Arrays.fill(next[i], -1);
            distance[i][i] = 0;
        }
    }

    public void addEdge(int u, int v) {
        distance[u - 1][v - 1] = 1;
        distance[v - 1][u - 1] = 1;
        next[u - 1][v - 1] = v - 1;
        next[v - 1][u - 1] = u - 1;
    }

    public void computeRoutes() {
        for (int k = 0; k < V; k++) {
            for (int i = 0; i < V; i++) {
                for (int j = 0; j < V; j++) {
                    if (distance[i][k] + distance[k][j] < distance[i][j]) {
                        distance[i][j] = distance[i][k] + distance[k][j];
                        next[i][j] = next[i][k];
                    }
                }
            }
        }
    }

    public List<Integer> getShortestPath(int u, int v) {
        u--; v--;
        List<Integer> path = new ArrayList<>();
        if (next[u][v] == -1) return path;
        path.add(u + 1);
        while (u != v) {
            u = next[u][v];
            path.add(u + 1);
        }
        return path;
    }

    public void printRoutingTable() {
        System.out.println("----- Routing Table (Next Hop for All Pairs) -----");
        for (int i = 0; i < V; i++) {
            System.out.print("From Node " + (i + 1) + ": ");
            for (int j = 0; j < V; j++) {
                if (i != j && next[i][j] != -1) {
                    System.out.print("To " + (j + 1) + " via " + (next[i][j] + 1) + " | ");
                }
            }
            System.out.println();
        }
    }

    public void printGraphEdges() {
        System.out.println("----- Graph Connections (Edges) -----");
        for (int i = 0; i < V; i++) {
            for (int j = i + 1; j < V; j++) {
                if (distance[i][j] == 1) {
                    System.out.println("Edge between Node " + (i + 1) + " and Node " + (j + 1));
                }
            }
        }
    }

    public void displayTopologyType() {
        int[] degree = new int[V];
        for (int i = 0; i < V; i++) {
            for (int j = 0; j < V; j++) {
                if (distance[i][j] == 1) {
                    degree[i]++;
                }
            }
        }

        int nodesWithOne = 0, nodesWithTwo = 0, nodesWithVMinus1 = 0;
        for (int d : degree) {
            if (d == 1) nodesWithOne++;
            else if (d == 2) nodesWithTwo++;
            else if (d == V - 1) nodesWithVMinus1++;
        }

        if (nodesWithOne == 2 && nodesWithTwo == V - 2) {
            System.out.println("Detected Topology: Bus Topology");
        } else if (nodesWithTwo == V) {
            System.out.println("Detected Topology: Ring Topology");
        } else if (nodesWithVMinus1 == 1 && Arrays.stream(degree).filter(d -> d == 1).count() == V - 1) {
            System.out.println("Detected Topology: Star Topology");
        } else if (Arrays.stream(degree).allMatch(d -> d == V - 1)) {
            System.out.println("Detected Topology: Mesh Topology");
        } else {
            System.out.println("Detected Topology: Custom or Hybrid Topology");
        }
    }
}

public class CNPROJECT_23BCE5078 {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        List<Node> nodes = new ArrayList<>();
        Graph graph = new Graph();

        System.out.println("Enter the baseport:");
        int baseport = scan.nextInt();
        scan.nextLine();

        for (int i = 1; i <= 30; i++) {
            try {
                nodes.add(new Node(i, baseport + i));
            } catch (IOException e) {
                System.out.println("Error creating Node " + i + ": " + e.getMessage());
                return;
            }
        }

        for (Node node : nodes) {
            node.start();
        }
        System.out.println("All 30 nodes created!!! Now specify connections:");

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
                graph.addEdge(src, dest);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter numbers!!");
            }
        }

        graph.computeRoutes();
        graph.displayTopologyType();

        System.out.println("Would you like to view the routing table and edges? (yes/no)");
        String showTables = scan.nextLine().trim();
        if (showTables.equalsIgnoreCase("yes")) {
            graph.printRoutingTable();
            graph.printGraphEdges();
        }

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
                List<Integer> path = graph.getShortestPath(src, dest);
                if (path.isEmpty()) {
                    System.out.println("No path exists between Node " + src + " and Node " + dest);
                    continue;
                }
                System.out.println("Shortest Path: " + path);
                nodes.get(src - 1).forwardMessage(nodes, path, message, 0);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter numbers!!");
            }
        }

        while (true) {
            System.out.println("Enter node id to print performance stats, -1 to skip, or 'shutdown' to stop the program:");
            String input = scan.next().trim();

            if (input.equalsIgnoreCase("shutdown")) {
                System.out.println("Shutting down all nodes...");
                for (Node node : nodes) {
                    node.terminate();
                }
                break;
            }

            try {
                int id = Integer.parseInt(input);
                if (id == -1) {
                    System.out.println("Skipping performance analysis and stopping program.");
                    break;
                }
                if (id < 1 || id > 30) {
                    System.out.println("Invalid node id");
                    continue;
                }
                nodes.get(id - 1).printPerformanceStats();
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a valid node id or 'shutdown'.");
            }
        }

        scan.close();
    }
}
