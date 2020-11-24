import com.google.gson.Gson;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import org.bson.Document;

import org.bson.Document;
import org.bson.types.ObjectId;
import utils.Utils;

import java.io.Console;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.sql.Date;
import java.text.DecimalFormat;
import java.util.*;
import java.util.*;

/**
 * A class that handles main UI operations and invokes functions
 */
public class Main {
    private Post selectedPost = new Post();
    static Scanner scanner = new Scanner(System.in);
    final HashMap<String, Method> cmds;
    private static DBController dbController;
    private final static PrintStream out = new PrintStream(System.out);
    String curUserUid;
    private static final Gson gson = new Gson();

    public Main() throws NoSuchMethodException {
        cmds = new HashMap<String, Method>() {{ // initialize the commands into hashmap of methods
            put("p", Main.class.getMethod("postQuestion"));
            put("s", Main.class.getMethod("searchPost"));
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
        if(selectedPost == null)
            out.println("You haven't selected a post!");
        else if(selectedPost.PostTypeId == null)
            out.println("You haven't selected a post or this post has no type :(");
        else if(selectedPost.PostTypeId.compareTo("1") != 0)
            out.println("This selected post is not a question!");

        FindIterable<Document> ansForCurPost = dbController.getAnswer(selectedPost.AcceptedAnswerId);

        ansForCurPost.forEach((Block<? super Document>) a -> {
            out.println("*-*-* ACCEPTED ANSWER *-*-*");
            displayBasicAnswerInfo(a);
        });

        ansForCurPost = dbController.getAnswersToQuestion(selectedPost.Id);

        ansForCurPost.forEach((Block<? super Document>) post -> {
            if(post.getString("Id").compareTo(selectedPost.AcceptedAnswerId) != 0) {
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
                selectedPost = gson.fromJson(post.toJson(), Post.class);
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

        if(selectedPost == null || selectedPost.Id == null)
            out.println("You have no currently selected post :)");
        else
            displayFullSelectedInfo();
    }

    public void displayFullSelectedInfo() {
        out.println(" --- * --- YOUR SELECTED POST --- * ---");
        out.println("Id: " + selectedPost.Id);
        String postType;
        if(selectedPost.PostTypeId != null && selectedPost.PostTypeId.compareTo("1") == 0)
            postType = "Question";
        else if(selectedPost.PostTypeId != null && selectedPost.PostTypeId.compareTo("2") == 0)
            postType = "Answer";
        else
            postType = "Special/Unknown Post Type";

        out.println("PostType: " + postType);
        if(postType.compareTo("Question") == 0 && selectedPost.Title != null)
            out.println("Title: " + selectedPost.Title);

        if(selectedPost.Body != null) {
            out.print("Body: ");
            int prev = 0;
            for (int i = 80; i < selectedPost.Body.length(); i += 80) {
                out.println(selectedPost.Body.substring(prev, i));
                prev = i;
            }
            out.println(selectedPost.Body.substring(prev, selectedPost.Body.length()));
        }

        if(selectedPost.Score != null) {
            out.println("Score: " + selectedPost.Score);
        }

        if(selectedPost.CreationDate != null) {
            out.println("Creation Date: " + selectedPost.CreationDate);
        }
        System.out.println(" -- * ---- ---***--- * ---***--- ---- * --");
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
