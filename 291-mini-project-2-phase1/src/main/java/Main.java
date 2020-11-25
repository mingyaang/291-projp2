import java.io.PrintStream;
import java.util.Scanner;

/**
 * A class that handles main UI operations and invokes functions
 */
public class Main {
    static Scanner scanner = new Scanner(System.in);
    private static DBController dbController;
    private final static PrintStream out = new PrintStream(System.out);

    public static void main(String[] args) throws NoSuchMethodException {
        if (args == null || args.length != 1) {
            out.println("Invalid cmd line arguments on start");
            return;
        }

        // This creates the db
        dbController = new DBController(Integer.valueOf(args[0]));
        out.println("Your db has been created !");
    }
}