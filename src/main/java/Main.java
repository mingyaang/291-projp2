import com.google.gson.Gson;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import org.bson.Document;

import java.io.Console;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.sql.Date;
import java.text.DecimalFormat;
import java.util.*;

/**
 * A class that handles main UI operations and invokes functions
 */
public class Main {
    static Scanner scanner = new Scanner(System.in);
    final HashMap<String, Method> cmds;
    private static DBController dbController;
    private final static PrintStream out = new PrintStream(System.out);
    String curUserUid;
    Post curSelectedPost;
    private static final Gson gson = new Gson();

    public Main() throws NoSuchMethodException {
        cmds = new HashMap<String, Method>() {{ // initialize the commands into hashmap of methods
//            put("p", Main.class.getMethod("postQuestion"));
//            put("s", Main.class.getMethod("searchPost"));
//            put("a", Main.class.getMethod("answerPost"));
            put("h", Main.class.getMethod("help"));
            put("l", Main.class.getMethod("listAnswers"));
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

    public void giveReport() {
        if(curUserUid == null || curUserUid.isEmpty())
            return;

        // q owned // a owned // q votes // a votes
        double[] res = new double[]{0, 0, 0, 0};
        Set<String> userPosts = new HashSet<>();

        dbController.getPostsBy(curUserUid).forEach((Block<? super Document>) post -> {
            String postType = (String) post.get("PostTypeId");
            Integer score = (Integer) post.get("Score");
            userPosts.add(post.getString("Id"));
            if(postType != null && postType.compareTo("1") == 0) {
                res[0]++;
                res[2] += score == null ? 0 : score;
            } else if (postType != null && postType.compareTo("2") == 0) {
                res[1]++;
                res[3] += score == null ? 0 : score;
            }
        });

        DecimalFormat decimalFormat = new DecimalFormat("#0.00");
        double averageQScore = res[0] == 0 ? 0 : res[2]/res[0];
        double averageAScore = res[1] == 0 ? 0 : res[3]/res[1];

        out.println(" --------- REPORT ON USER : " + curUserUid + " ---------");
        out.println("Number Question Owned : " + decimalFormat.format(res[0]));
        out.println("Average Score on Qs   : " + decimalFormat.format(averageQScore));
        out.println("Number Answers Owned  : " + decimalFormat.format(res[1]));
        out.println("Average Score on As   : " + decimalFormat.format(averageAScore));

        int[] voteCnt = new int[]{0};
        dbController.getVotesInPosts(userPosts).forEach((Block<? super Document>) vote -> {
            voteCnt[0]++;
        });

        out.println("Votes registered by user : " + voteCnt[0]);
        out.println(" ----------- * ----------- * ----------- ");
    }

    public void listAnswers() {
        if(curSelectedPost == null)
            out.println("You haven't selected a post!");
        else if(curSelectedPost.PostTypeId == null)
            out.println("Something went wrong, cannot verify this post is of type question");
        else if(curSelectedPost.PostTypeId.compareTo("1") != 0)
            out.println("This selected post is not a question!");

        FindIterable<Document> ansForCurPost = dbController.getAnswer(curSelectedPost.AcceptedAnswerId);

        ansForCurPost.forEach((Block<? super Document>) a -> {
            out.println("*-*-* ACCEPTED ANSWER *-*-*");
            displayBasicAnswerInfo(a);
        });

        ansForCurPost = dbController.getAnswersToQuestion(curSelectedPost.Id);

        ansForCurPost.forEach((Block<? super Document>) post -> {
            if(post.getString("Id").compareTo(curSelectedPost.AcceptedAnswerId) != 0) {
                out.println("----- ----- ----- ----- -----");
                displayBasicAnswerInfo(post);
            }
        });
        out.println("----- ----- ----- ----- -----");

        out.println("s to select a post, and any other entry to exit :)");
        String in = scanner.nextLine();
        if(in.compareTo("s") == 0)
            selectPost();
    }

    private void selectPost() {
        out.println("Enter your post id below!");
        String in = scanner.nextLine();

        FindIterable<Document> posts = dbController.getPostById(in);
        final boolean[] foundFlag = {false};
        posts.forEach((Block<? super Document>) post -> {
            if(post != null) {
                foundFlag[0] = true;
                String postJson = post.toJson();
                curSelectedPost = gson.fromJson(post.toJson(), Post.class);
            }
        });

        if(!foundFlag[0]) {
            out.println("Wasn't able to find post " + in + " enter a valid id!");
            out.println("Post was reset to earlier selection !");
            return;
        }

        displayFullSelectedInfo();
        out.println("Selected the post!~ Please input command as h to view again");
    }

    private void displayBasicAnswerInfo(Document post) {
        String id = post.getString("Id");
        out.println("Id: " + id);

        String body = post.getString("Body");
        if(body.length() > 80) {
            body = body.substring(0, 81);
            body += "...";
        }
        out.println("Body: " + body);
        Integer score = post.getInteger("Score");
        out.println("Score: " + score);
        String creationDate = post.getString("CreationDate");
        out.println("Creation Date: " + creationDate);
    }

    public void help() {
        out.println(StringConstants.HELP);

        if(curSelectedPost == null || curSelectedPost.Id == null)
            out.println("You have no currently selected post :)");
        else
            displayFullSelectedInfo();
    }

    public void displayFullSelectedInfo() {
        out.println(" --- * --- YOUR SELECTED POST --- * ---");
        out.println("Id: " + curSelectedPost.Id);
        String postType;
        if(curSelectedPost.PostTypeId != null && curSelectedPost.PostTypeId.compareTo("1") == 0)
            postType = "Question";
        else if(curSelectedPost.PostTypeId != null && curSelectedPost.PostTypeId.compareTo("2") == 0)
            postType = "Answer";
        else
            postType = "Special/Unknown Post Type";

        if(curSelectedPost.Body != null) {
            out.println("PostType: " + postType);
            out.print("Body: ");
            int prev = 0;
            for (int i = 80; i < curSelectedPost.Body.length(); i += 80) {
                out.println(curSelectedPost.Body.substring(prev, i));
                prev = i;
            }
            out.println(curSelectedPost.Body.substring(prev, curSelectedPost.Body.length()));
        }

        if(curSelectedPost.Score != null) {
            out.println("Score: " + curSelectedPost.Score);
        }

        if(curSelectedPost.CreationDate != null) {
            out.println("Creation Date: " + curSelectedPost.CreationDate);
        }
        System.out.println(" -- * -- ----***---- -- * --");
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
