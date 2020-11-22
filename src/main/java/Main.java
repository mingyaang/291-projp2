import java.io.Console;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

/**
 * A class that handles main UI operations and invokes functions
 */
public class Main {
    static Scanner scanner = new Scanner(System.in);
    final HashMap<String, Method> cmds;
    private static DBController dbController;
    private final static PrintStream out = new PrintStream(System.out);
    String curUserUid;

    public Main() throws NoSuchMethodException {
        cmds = new HashMap<String, Method>() {{ // initialize the commands into hashmap of methods
//            put("p", Main.class.getMethod("postQuestion"));
//            put("s", Main.class.getMethod("searchPost"));
//            put("a", Main.class.getMethod("answerPost"));
            put("h", Main.class.getMethod("help"));
//            put("l", Main.class.getMethod("listAnswers"));
//            put("v", Main.class.getMethod("vote"));
        }};
    }

    public static void main(String[] args) throws NoSuchMethodException {
        if(args == null || args.length != 1) {
            out.println("Invalid cmd line arguments on start");
            return;
        }

        // TODO SWITCH THIS BACK TO NORMAL CONTROLLER IF YOU NEED DB MADE
        dbController = new DBController(Integer.valueOf(args[0]), true);
        Main mainView = new Main();
        mainView.show();
    }

    public void show() {
        out.println(StringConstants.LOGO);
        out.println(StringConstants.INTRO);

        promptUid();

        help();
        while(true) {
            System.out.print("cmd: ");
            String cmd = scanner.nextLine();
            if(parseInput(cmd))
                break;
        }

        System.out.println(StringConstants.EXIT_MESSAGE);
    }

    /**
     * prompts user for uid and calls for report
     * @return
     */
    public void promptUid() {
        out.println("Would you like to provide a uid ? (y/n)");
        String res = scanner.nextLine();
        if(res != null && res.compareTo("y") == 0) {
            out.print("uid: ");
            while(true) {
                curUserUid = scanner.nextLine();
                try {
                    Long.parseLong(curUserUid);
                    break;
                } catch (NumberFormatException e) {
                    out.println("uids are numeric only!");
                }
            }

            giveReport();
        }
    }

    // TODO GIVE A REPORT WHEN USER LOGINS
    public void giveReport() {
        int numQOwned = 0;
        double avgScoreOnQs = 0;
        int numAOwned = 0;
        double avgScoreOnAs = 0;
        int votesForUser = 0;



    }

    //
    public void help() {
        out.println(StringConstants.HELP);
    }


    /**
     * return true if you want to exit the program false otherwise
     *
     * @param in
     * @return
     */
    public boolean parseInput(String in) {
        if(in.compareTo("exit") == 0)
            return true;

        // get method from map
        Method m = cmds.get(in);
        if (m == null) {
            out.println(StringConstants.INVALID_INPUT);
            return false;
        }

        Object res; // can get response but why lol

        try {   // invoke your method
            res = m.invoke(this);
        } catch (Exception e) {
            e.printStackTrace();
            out.println("Failure on invoking function");
            throw new RuntimeException("Failure on invoking method in cmds", e);
        }

        return false;
    }

}
