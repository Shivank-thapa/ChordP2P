import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import hashing.SHA1;

public class Node {
	private String address;
	private int port;
	private String existingNodeAddress = null;
	private int existingNodePort;
	private Finger secondPredecessor;
	private Finger firstPredecessor;
	private Finger firstSuccessor;
	private Finger secondSuccessor;
	private Map<Integer, Finger> fingers = new HashMap<>();
	private long id;
	private String hex;
	private Semaphore semaphore = new Semaphore(1);

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getExistingNodeAddress() {
		return existingNodeAddress;
	}

	public void setExistingNodeAddress(String existingNodeAddress) {
		this.existingNodeAddress = existingNodeAddress;
	}

	public int getExistingNodePort() {
		return existingNodePort;
	}

	public void setExistingNodePort(int existingNodePort) {
		this.existingNodePort = existingNodePort;
	}

	public Finger getSecondPredecessor() {
		return secondPredecessor;
	}

	public void setSecondPredecessor(Finger secondPredecessor) {
		this.secondPredecessor = secondPredecessor;
	}

	public Finger getFirstPredecessor() {
		return firstPredecessor;
	}

	public void setFirstPredecessor(Finger firstPredecessor) {
		this.firstPredecessor = firstPredecessor;
	}

	public Finger getFirstSuccessor() {
		return firstSuccessor;
	}

	public void setFirstSuccessor(Finger firstSuccessor) {
		this.firstSuccessor = firstSuccessor;
	}

	public Finger getSecondSuccessor() {
		return secondSuccessor;
	}

	public void setSecondSuccessor(Finger secondSuccessor) {
		this.secondSuccessor = secondSuccessor;
	}

	public Map<Integer, Finger> getFingers() {
		return fingers;
	}

	public void setFingers(Map<Integer, Finger> fingers) {
		this.fingers = fingers;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getHex() {
		return hex;
	}

	public void setHex(String hex) {
		this.hex = hex;
	}

	public Semaphore getSemaphore() {
		return semaphore;
	}

	public void setSemaphore(Semaphore semaphore) {
		this.semaphore = semaphore;
	}

	public void acquire() {
		try {
			this.semaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	

    public void release() {
        this.semaphore.release();
    }

	public Node(String address, String port) {
		this.address = address;
		this.port = Integer.valueOf(port);

		SHA1 hasher = new SHA1(this.address + ":" + this.port);
        this.id = hasher.getLong();
        this.hex = hasher.getHex();

		System.out.println("Creating a new Chord ring");
		System.out.println("Listening on port " + this.port);

		this.initializeFingers();
		this.initializeSuccessors();

		new Thread(new Listener(this)).start();
		new Thread(new Stabilizer(this)).start();
		new Thread(new UpdatePusher(this)).start();
	}

	public Node(String address, String port, String existingNodeAddress, String existingNodePort) {
		this.address = address;
		this.port = Integer.valueOf(port);

		this.existingNodeAddress = existingNodeAddress;
		this.existingNodePort = Integer.valueOf(existingNodePort);

        SHA1 hasher = new SHA1(this.address + ":" + this.port);
        this.id = hasher.getLong();
        this.hex = hasher.getHex();

		System.out.println("Joining the Chord ring");
		System.out.println("You are listening on port " + this.port);
		System.out.println("Connected to existing node " + this.existingNodeAddress + ":" + this.existingNodePort);

		this.initializeFingers();
		this.initializeSuccessors();

		new Thread(new Listener(this)).start();
		new Thread(new Stabilizer(this)).start();
		new Thread(new UpdatePusher(this)).start();
	}

	private void initializeFingers() {
		if (this.existingNodeAddress == null) {
			for (int i = 0; i < 32; i++) {
				this.fingers.put(i, new Finger(this.address, this.port));
			}
		} else {
			try {
				Socket socket = new Socket(this.existingNodeAddress, this.existingNodePort);

				PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true);
				BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				BigInteger bigQuery = BigInteger.valueOf(2L);
				BigInteger bigSelfId = BigInteger.valueOf(this.id);

				for (int i = 0; i < 32; i++) {
					BigInteger bigResult = bigQuery.pow(i);
					bigResult = bigResult.add(bigSelfId);

					socketWriter.println(Util.FIND_NODE + ":" + bigResult.longValue());
					System.out.println("Sent: " + Util.FIND_NODE + ":" + bigResult.longValue());

					String response = socketReader.readLine();

					String[] responseList = response.split(":", 2);
					String[] addressList = responseList[1].split(":");

					this.fingers.put(i, new Finger(addressList[0], Integer.valueOf(addressList[1])));

					System.out.println("Response from server: " + response);
				}

				socketWriter.close();
				socketReader.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void initializeSuccessors() {
		this.firstSuccessor = this.fingers.get(0);
		this.secondSuccessor = this.fingers.get(1);
		this.firstPredecessor = new Finger(this.address, this.port);
		this.secondPredecessor = new Finger(this.address, this.port);

		if (!this.address.equals(this.firstSuccessor.getAddress()) || (this.port != this.firstSuccessor.getPort())) {
			try {
				Socket socket = new Socket(this.firstSuccessor.getAddress(), this.firstSuccessor.getPort());

				PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true);

				socketWriter.println(Util.NEW_PRED + ":" + this.getAddress() + ":" + this.getPort());
				System.out.println("Sent: " + Util.NEW_PRED + ":" + this.getAddress() + ":" + this.getPort() + " to "
						+ this.firstSuccessor.getAddress() + ":" + this.firstSuccessor.getPort());

				socketWriter.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
