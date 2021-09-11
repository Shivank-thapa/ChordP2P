import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;

public class Stabilizer extends Thread {
	  private Node node;
	    private int delay = 10;

	    public Stabilizer(Node node) {
	        this.node = node;
	    }
	    
	    public void run() {
	        try {
	            Thread.sleep(this.delay * 1000);

	            Socket socket = null;
	            PrintWriter socketWriter = null;
	            BufferedReader socketReader = null;

	            while (true) {
	                if (!this.node.getAddress().equals(this.node.getFirstSuccessor().getAddress()) || (this.node.getPort() != this.node.getFirstSuccessor().getPort())) {
	                    
	                    socket = new Socket(this.node.getFirstSuccessor().getAddress(), this.node.getFirstSuccessor().getPort());

	                    socketWriter = new PrintWriter(socket.getOutputStream(), true);
	                    socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

	                    socketWriter.println(Util.REQUEST_PRED + ":" + this.node.getId() + " requesting " + this.node.getFirstSuccessor().getId());
	                    System.out.println("Sent: " + Util.REQUEST_PRED + ":" + this.node.getId() + " requesting " + this.node.getFirstSuccessor().getId());

	                    String serverResponse = socketReader.readLine();
	                    System.out.println("Received: " + serverResponse);

	                    String[] predecessorFragments = serverResponse.split(":");
	                    String predecessorAddress = predecessorFragments[0];
	                    int predecessorPort = Integer.valueOf(predecessorFragments[1]);

	                    if (!this.node.getAddress().equals(predecessorAddress) || (this.node.getPort() != predecessorPort)) {
	                        this.node.acquire();

	                        Finger newSuccessor = new Finger(predecessorAddress, predecessorPort);

	                        this.node.getFingers().put(1, this.node.getFingers().get(0));
	                        this.node.getFingers().put(0, newSuccessor);

	                        this.node.setSecondSuccessor(this.node.getFirstSuccessor());
	                        this.node.setFirstSuccessor(newSuccessor);

	                        this.node.release();

	                        socketWriter.close();
	                        socketReader.close();
	                        socket.close();

	                        socket = new Socket(newSuccessor.getAddress(), newSuccessor.getPort());

	                        socketWriter = new PrintWriter(socket.getOutputStream(), true);
	                        socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

	                        socketWriter.println(Util.NEW_PRED + ":" + this.node.getAddress() + ":" + this.node.getPort());
	                        System.out.println("Sent: " + Util.NEW_PRED + ":" + this.node.getAddress() + ":" + this.node.getPort());
	                    }

	                    BigInteger bigQuery = BigInteger.valueOf(2L);
	                    BigInteger bigSelfId = BigInteger.valueOf(this.node.getId());

	                    this.node.acquire();

	                    for (int i = 0; i < 32; i++) {
	                        BigInteger bigResult = bigQuery.pow(i);
	                        bigResult = bigResult.add(bigSelfId);

	                        socketWriter.println(Util.FIND_NODE + ":" + bigResult.longValue());
	                        System.out.println("Sent: " + Util.FIND_NODE + ":" + bigResult.longValue());

	                        serverResponse = socketReader.readLine();

	                        String[] serverResponseFragments = serverResponse.split(":", 2);
	                        String[] addressFragments = serverResponseFragments[1].split(":");

	                        this.node.getFingers().put(i, new Finger(addressFragments[0], Integer.valueOf(addressFragments[1])));
	                        this.node.setFirstSuccessor(this.node.getFingers().get(0));
	                        this.node.setSecondSuccessor(this.node.getFingers().get(1));

	                        System.out.println("Received: " + serverResponse);
	                    }

	                    this.node.release();

	                    socketWriter.close();
	                    socketReader.close();
	                    socket.close();
	                } else if (!this.node.getAddress().equals(this.node.getFirstPredecessor().getAddress()) || (this.node.getPort() != this.node.getFirstPredecessor().getPort())) {
	               
	                    socket = new Socket(this.node.getFirstPredecessor().getAddress(), this.node.getFirstPredecessor().getPort());

	                    socketWriter = new PrintWriter(socket.getOutputStream(), true);
	                    socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

	                    BigInteger bigQuery = BigInteger.valueOf(2L);
	                    BigInteger bigSelfId = BigInteger.valueOf(this.node.getId());

	                    this.node.acquire();

	                    for (int i = 0; i < 32; i++) {
	                        BigInteger bigResult = bigQuery.pow(i);
	                        bigResult = bigResult.add(bigSelfId);

	                        socketWriter.println(Util.FIND_NODE + ":" + bigResult.longValue());
	                        System.out.println("Sent: " + Util.FIND_NODE + ":" + bigResult.longValue());

	                        String response = socketReader.readLine();

	                        String[] responseList = response.split(":", 2);
	                        String[] addressList = responseList[1].split(":");

	                        this.node.getFingers().put(i, new Finger(addressList[0], Integer.valueOf(addressList[1])));
	                        this.node.setFirstSuccessor(this.node.getFingers().get(0));
	                        this.node.setSecondSuccessor(this.node.getFingers().get(1));

	                        System.out.println("Received: " + response);
	                    }

	                    this.node.release();

	                    socketWriter.close();
	                    socketReader.close();
	                    socket.close();
	                }

	                Thread.sleep(this.delay * 1000);
	            }
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        } catch (UnknownHostException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }

}
