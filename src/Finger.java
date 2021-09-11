
public class Finger {
	private String address;
    private int port;
    private long id;

    public Finger(String address, int port) {
        this.address = address;
        this.port = port;

        //SHA1Hasher sha1Hasher = new SHA1Hasher(this.address + ":" + this.port);
        //this.id = sha1Hasher.getLong();
    }

    public String getAddress() {
        return this.address;
    }

    public int getPort() {
        return this.port;
    }

    public void setAddress(String address) {
		this.address = address;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getId() {
        return this.id;
    }
}
