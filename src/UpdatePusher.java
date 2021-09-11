import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class UpdatePusher implements Runnable {

    private Node node = null;
    private final static int delayInit = 30;
    private final static int delay = 5;

    public UpdatePusher(Node node) {
        this.node = node;
    }
    
    public void run() {
        try {
            Thread.sleep(UpdatePusher.delayInit * 1000);

            while (true) {
                this.testSuccessor();
                this.testPredecessor();

                Thread.sleep(UpdatePusher.delay * 1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void testSuccessor() {
        if (!this.node.getAddress().equals(this.node.getFirstSuccessor().getAddress()) || (this.node.getPort() != this.node.getFirstSuccessor().getPort())) {
            try {
                Socket socket = new Socket(this.node.getFirstSuccessor().getAddress(), this.node.getFirstSuccessor().getPort());

                PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                socketWriter.println(Util.PING_QUERY + ":" + this.node.getId());
                System.out.println("Sent: " + Util.PING_QUERY + ":" + this.node.getId());

                String serverResponse = socketReader.readLine();
                System.out.println("Received: " + serverResponse);

                if (!serverResponse.equals(Util.PING_RESPONSE)) {
                    this.node.acquire();
                    this.node.setFirstSuccessor(this.node.getSecondSuccessor());
                    this.node.getFingers().put(0, this.node.getSecondSuccessor());
                    this.node.release();
                }

                socketWriter.close();
                socketReader.close();
                socket.close();
            } catch (IOException e) {
                this.node.acquire();
                this.node.setFirstSuccessor(this.node.getSecondSuccessor());
                this.node.getFingers().put(0, this.node.getSecondSuccessor());
                this.node.release();
            }
        }
    }

    private void testPredecessor() {
        if (!this.node.getAddress().equals(this.node.getFirstPredecessor().getAddress()) || (this.node.getPort() != this.node.getFirstPredecessor().getPort())) {
            try {
                Socket socket = new Socket(this.node.getFirstPredecessor().getAddress(), this.node.getFirstPredecessor().getPort());

                PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                socketWriter.println(Util.PING_QUERY + ":" + this.node.getId());
                System.out.println("Sent: " + Util.PING_QUERY + ":" + this.node.getId());

                String serverResponse = socketReader.readLine();
                System.out.println("Received: " + serverResponse);

                if (!serverResponse.equals(Util.PING_RESPONSE)) {
                    this.node.acquire();
                    this.node.setFirstPredecessor(this.node.getSecondPredecessor());
                    this.node.release();
                }

                socketWriter.close();
                socketReader.close();
                socket.close();
            } catch (IOException e) {
                this.node.acquire();
                this.node.setFirstPredecessor(this.node.getSecondPredecessor());
                this.node.release();
            }
        }
    }
}
