import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import hashing.SHA1;

public class NodeThread implements Runnable {
	private Node node;
    private Socket socket = null;

    public NodeThread(Node node, Socket sock) {
        this.node = node;
        this.socket = sock;
    }
    
    public void run() {
        System.out.println("Connection succesfully established on port " + this.socket.getLocalPort());

        try {
            PrintWriter socketWriter = new PrintWriter(this.socket.getOutputStream(), true);
            BufferedReader socketReader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

            String query;
            while ((query = socketReader.readLine()) != null) {
                String[] queryContents = query.split(":", 2);
                String command = queryContents[0];
                String content = queryContents[1];

                System.out.println("Received: " + command + " " + content);

                switch (command) {
                    case Util.FIND_VAL: {
                        String response = this.findValue(content);
                        System.out.println("Sent: " + response);

                        socketWriter.println(response);

                        break;
                    }
                    case Util.FIND_NODE: {
                        String response = this.findNode(content);
                        System.out.println("Sent: " + response);

                        socketWriter.println(response);

                        break;
                    }
                    case Util.NEW_PRED: {
                        String[] contentList = content.split(":");
                        String address = contentList[0];
                        int port = Integer.valueOf(contentList[1]);

                        this.node.acquire();

                        this.node.setSecondPredecessor(this.node.getFirstPredecessor());

                        this.node.setFirstPredecessor(new Finger(address, port));

                        this.node.release();

                        break;
                    }
                    case Util.REQUEST_PRED: {
                        String response = this.node.getFirstPredecessor().getAddress() + ":" + this.node.getFirstPredecessor().getPort();
                        System.out.println("Sent: " + response);

                        socketWriter.println(response);

                        break;
                    }
                    case Util.PING_QUERY: {
                        String response = Util.PING_RESPONSE;
                        System.out.println("Sent: " + response);

                        socketWriter.println(response);

                        break;
                    }
                }
            }

            socketWriter.close();
            socketReader.close();
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Connection terminated on port " + this.socket.getLocalPort());
    }
    
    private boolean checkQueryCurrentNode(long queryId) {
        boolean response = false;

        if (this.node.getId() > this.node.getFirstPredecessor().getId()) {
            if ((queryId > this.node.getFirstPredecessor().getId()) && (queryId <= this.node.getId())) {
                response = true;
            }
        } else {
            if ((queryId > this.node.getFirstPredecessor().getId()) || (queryId <= this.node.getId())) {
                response = true;
            }
        }

        return response;
    }
    
    private boolean checkQueryNextNode(long queryId) {
        boolean response = false;

        if (this.node.getId() < this.node.getFirstSuccessor().getId()) {
            if ((queryId > this.node.getId()) && (queryId <= this.node.getFirstSuccessor().getId())) {
                response = true;
            }
        } else { 
            if ((queryId > this.node.getId()) || (queryId <= this.node.getFirstSuccessor().getId())) {
                response = true;
            }
        }

        return response;
    }
    
    private String findNode(String query) {
        long queryId = Long.valueOf(query);

        if (queryId >= Util.RING_SIZE) {
            queryId -= Util.RING_SIZE;
        }

        String response = "Not found.";

        if (this.checkQueryCurrentNode(queryId)) {
            response = Util.NODE_FOUND + ":" +  this.node.getAddress() + ":" + this.node.getPort();
        } else if(this.checkQueryNextNode(queryId)) {
            response = Util.NODE_FOUND + ":" +  this.node.getFirstSuccessor().getAddress() + ":" + this.node.getFirstSuccessor().getPort();
        } else { 
            long minimumDistance = Util.RING_SIZE;
            Finger closestPredecessor = null;

            this.node.acquire();

            for (Finger finger : this.node.getFingers().values()) {
                long distance;

                if (queryId >= finger.getId()) {
                    distance = queryId - finger.getId();
                } else {
                    distance = queryId + Util.RING_SIZE - finger.getId();
                }

                if (distance < minimumDistance) {
                    minimumDistance = distance;
                    closestPredecessor = finger;
                }
            }

            System.out.println("queryid: " + queryId + " minimum distance: " + minimumDistance + " on " + closestPredecessor.getAddress() + ":" + closestPredecessor.getPort());

            try {
                Socket socket = new Socket(closestPredecessor.getAddress(), closestPredecessor.getPort());

                PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                socketWriter.println(Util.FIND_NODE + ":" + queryId);
                System.out.println("Sent: " + Util.FIND_NODE + ":" + queryId);

                String serverResponse = socketReader.readLine();
                System.out.println("Response from node " + closestPredecessor.getAddress() + ", port " + closestPredecessor.getPort() + ", position " + " (" + closestPredecessor.getId() + "):");

                response = serverResponse;

                socketWriter.close();
                socketReader.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.node.release();
        }

        return response;
    }
    
    
    
    private String findValue(String query) {
        SHA1 queryHasher = new SHA1(query);
        long queryId = queryHasher.getLong();

        if (queryId >= Util.RING_SIZE) {
            queryId -= Util.RING_SIZE;
        }

        String response = "Not found.";

        if (this.checkQueryCurrentNode(queryId)) {
            response = "VALUE_FOUND: Node " + this.node.getAddress() + ":" + this.node.getPort();
        } else if (this.checkQueryNextNode(queryId)) {
            response = "VALUE_FOUND: Node " + this.node.getFirstSuccessor().getAddress() + ":" + this.node.getFirstSuccessor().getPort();
        } else { 
            long minimumDistance = Util.RING_SIZE;
            Finger closestPredecessor = null;

            this.node.acquire();

            
            for (Finger finger : this.node.getFingers().values()) {
                long distance;

                if (queryId >= finger.getId()) {
                    distance = queryId - finger.getId();
                } else {
                    distance = queryId + Util.RING_SIZE - finger.getId();
                }

                if (distance < minimumDistance) {
                    minimumDistance = distance;
                    closestPredecessor = finger;
                }
            }

            System.out.println("queryid: " + queryId + " minimum distance: " + minimumDistance + " on " + closestPredecessor.getAddress() + ":" + closestPredecessor.getPort());

            try {
                Socket socket = new Socket(closestPredecessor.getAddress(), closestPredecessor.getPort());

                PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                socketWriter.println(Util.FIND_VAL+ ":" + query);
                System.out.println("Sent: " + Util.FIND_VAL + ":" + query);

                String serverResponse = socketReader.readLine();
                System.out.println("Response from node " + closestPredecessor.getAddress() + ", port " + closestPredecessor.getPort() + ", position " + " (" + closestPredecessor.getId() + "):");

                response = serverResponse;

                socketWriter.close();
                socketReader.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.node.release();
        }

        return response;
    }

}
