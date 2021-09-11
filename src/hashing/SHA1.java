package hashing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

public class SHA1 {
	private String address;
    private byte[] hashedBytes = new byte[8];

    public SHA1() {}

    public SHA1(String address) {
        this.address = address;
        this.hash();
    }

    public void hash() {
        MessageDigest md;

        try {
            md = MessageDigest.getInstance("SHA-1");

            byte[] addressBytes = md.digest(this.address.getBytes());

            for (int i = 0; i < 4; i++) {
                this.hashedBytes[i + 4] = (byte) (addressBytes[i] ^ addressBytes[i + 4] ^ addressBytes[i + 8] ^ addressBytes[i + 12] ^ addressBytes[i + 16]);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public String getHex() {
        return DatatypeConverter.printHexBinary(Arrays.copyOfRange(this.hashedBytes, 4, 8));
    }

    public long getLong() {
        return java.nio.ByteBuffer.wrap(this.hashedBytes).getLong();
    }

    public void setAddress(String address) {
        this.address = address;
        this.hash();
    }
}
