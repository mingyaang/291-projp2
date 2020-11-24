import org.bson.Document;
import org.bson.types.ObjectId;
import utils.Utils;

import java.io.Console;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.sql.Date;
import java.util.*;

/**
 * A class that handles main UI operations and invokes functions
 */
public class Main {
    private final Post selectedPost = new Post();
    static Scanner scanner = new Scanner(System.in);
    final HashMap<String, Method> cmds;
    private static DBController dbController;
    private final static PrintStream out = new PrintStream(System.out);
    String curUserUid;

    public Main() throws NoSuchMethodException {
        cmds = new HashMap<String, Method>() {{ // initialize the commands into hashmap of methods
            put("p", Main.class.getMethod("postQuestion"));
            put("s", Main.class.getMethod("searchPost"));
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

    public void postQuestion() {
        System.out.print("Enter a title: ");
        String title = scanner.nextLine();
        System.out.print("Enter a body: ");
        String body = scanner.nextLine();
        System.out.print("Enter tags (separate by ,): ");
        String tags = scanner.nextLine();
        String[] sTags = {};
        if (tags.length() > 0) {
            sTags = tags.split(",");
        }

//        String genPid = Utils.generateID(4);
//        Post checkPost = dbController.getPost(genPid);
//        while (checkPost != null) {
//            genPid = Utils.generateID(4);
//            checkPost = dbController.getPost(genPid);
//        }

//        Date date = Utils.getSQLDate();
        Boolean status = dbController.postQuestion(curUserUid, "1", title, body, sTags);
        System.out.println("Thanks for posting your question!");
    }

    public void searchPost() {
        System.out.print("Search Keywords: ");
        String keywords = scanner.nextLine();
        String[] splitKeywords = keywords.split(",");
        if (splitKeywords.length == 0) {
            return;
        }

        List<Document> results = dbController.search(splitKeywords);

        Iterator iterator = results.iterator();
        System.out.println("Displaying first 5 results.");
        System.out.println("q to quit, > for next page, s to select a post");
        iterator = printSearch(iterator);
        Boolean done = false;
        while (true) {
            if (!iterator.hasNext() && !done) {
                System.out.println("End of search results");
                done = true;
            }
            String input = scanner.nextLine();
            if (input.toLowerCase().compareTo("q") == 0) {
                return;
            } else if (input.toLowerCase().compareTo(">") == 0 && !done) {
                if (done) {
                    System.out.println("End of search results");
                } else {
                    iterator = printSearch(iterator);
                }
            } else if (input.toLowerCase().compareTo("s") == 0) {
                System.out.println("Enter the id of the post you want to select");
                String post = scanner.nextLine();
                if (results.stream().anyMatch((searchResult -> {
                    if (((String)searchResult.get("Id")).toLowerCase().compareTo(post.toLowerCase()) == 0) {
                        selectedPost._id = (ObjectId) searchResult.get("_id");
                        selectedPost.Id = (String) searchResult.get("Id");
                        selectedPost.Title = (String) searchResult.get("Title");
                        selectedPost.Body = (String) searchResult.get("Body");
                        selectedPost.Tags = (String) searchResult.get("Tags");
                        selectedPost.PostTypeId = (String) searchResult.get("PostTypeId");
                        selectedPost.AcceptedAnswerId = (String) searchResult.get("AcceptedAnswerId");
                        selectedPost.CreationDate = (String) searchResult.get("CreationDate");
                        selectedPost.AnswerCount = (Integer) searchResult.get("AnswerCount");
                        selectedPost.CommentCount = (Integer) searchResult.get("CommentCount");
                        selectedPost.FavoriteCount = (Integer) searchResult.get("FavoriteCount");
                        selectedPost.OwnerUserId = (String) searchResult.get("OwnerUserId");
                        selectedPost.Score = (Integer) searchResult.get("Score");
                        selectedPost.ViewCount = (Integer) searchResult.get("ViewCount");
                        return true;
                    }
                    return false;
                }))) {
                    System.out.println(post + " Selected");
                    break;
                } else {
                    System.out.println("Selected post does not exist. Start search again.");
                }
            } else {
                System.out.println("Invalid input");
                System.out.println("q to quit, > for next page, s to select a post");
            }
        }
        dbController.incrementViewCount(selectedPost._id);
        viewSelectedPost();
    }

    private Iterator printSearch(Iterator iterator) {
        int counter = 0;
        System.out.println("Id ||  Title   || Creation Date || Score || Answer Count");
        while (iterator.hasNext()) {
            Document d = (Document) iterator.next();
            String id = (String) d.get("Id");
            String title = (String) d.get("Title");
            String date = (String) d.get("CreationDate");
            Integer score = (Integer) d.get("Score");
            Integer answercnt = (Integer) d.get("AnswerCount");
            System.out.println(id + " || " + title + " || " + date + " || " + score + " || " + answercnt);
            counter++;
            if (counter == 5) {
                break;
            }
        }
        return iterator;
    }

    private void viewSelectedPost() {
        System.out.println("Post ID: " + selectedPost.Id);
        System.out.println("PostTypeId: " + selectedPost.PostTypeId);
        System.out.println("Accepted Answer Id: " + selectedPost.AcceptedAnswerId);
        System.out.println("Creation Date: " + selectedPost.CreationDate);
        System.out.println("Title: " + selectedPost.Title);
        System.out.println("Body: " + selectedPost.Body);
        System.out.println("Owner: " + selectedPost.OwnerUserId);
        System.out.println("Tags: " + selectedPost.Tags);
        System.out.println("Score: " + selectedPost.Score);
        System.out.println("View Count: " + (selectedPost.ViewCount + 1));
        System.out.println("Answer Count: " + selectedPost.AnswerCount);
        System.out.println("Comment Count: " + selectedPost.CommentCount);
        System.out.println("Favorite Count: " + selectedPost.FavoriteCount);


    }
}
