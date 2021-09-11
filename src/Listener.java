import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Listener implements Runnable {
	private Node node;

    public Listener(Node node) {
        this.node = node;
    }

    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(this.node.getPort());

            while(true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new NodeThread(this.node, clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
