
public class MainChord {

	public static void main(String[] args) {
		if (args.length == 1) {
            new Node("127.0.0.1", args[0]);
        }
		else if (args.length == 3) {
            new Node("127.0.0.1", args[0], args[1], args[2]);
        } 
		else {
            System.err.println("Incorrect usage! Please see README file.");
            System.exit(1);
        }
	}

}
