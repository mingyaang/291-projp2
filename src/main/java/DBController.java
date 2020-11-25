import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.bulk.UpdateRequest;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.BSON;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import utils.Utils;
import java.util.Scanner;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.PublicKey;
import java.util.*;
import java.util.logging.Logger;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import com.mongodb.client.result.*;

import javax.print.Doc;

public class DBController {
    Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
    MongoClient mongoClient;
    MongoDatabase db;
    MongoCollection<Document> postsCol;
    MongoCollection<Document> tagsCol;
    MongoCollection<Document> votesCol;
    Gson gson = new Gson();

    long timeForParsingTerms = 0;
    long timeForParsingData;
    long timeForCreatingIndicies;

    public DBController(int port) {
//        mongoLogger.setLevel(Level.SEVERE); // prevents log popup
        mongoClient = new MongoClient("localhost", port);
        db = mongoClient.getDatabase("291db");
        initCollections();

        long start = System.currentTimeMillis();

        System.out.println("Currently importing data into collections ...");
        System.out.println("Importing Posts...");
        initCollectionData(postsCol, "Posts.json");
        System.out.println("Importing Tags...");
        initCollectionData(tagsCol, "Tags.json");
        System.out.println("Importing Votes...");
        initCollectionData(votesCol, "Votes.json");
        timeForParsingData = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        System.out.println("Creating indices (This can take a while ~1 minute)");
        createIndexes();
        timeForCreatingIndicies = System.currentTimeMillis() - start;

        System.out.println("Parse data to mongo time: " + timeForParsingData + " ms");
        System.out.println("Parsing terms time: " + timeForParsingTerms + " ms");
        System.out.println("creating index time " + timeForCreatingIndicies + " ms");
    }

    // call this if you want to skip the setup time
    public DBController(int port, boolean noDropAndImportFlag) {
        mongoClient = new MongoClient("localhost", port);
        System.out.println("YOU ARE SKIPPING THE SETUP -- WARNING THIS IS ONLY ALLOWED IF YOU HAVE ALREADY SET UP THE DB AND DO NOT WISH TO REIMPORT DATA");
        db = mongoClient.getDatabase("291db");
        postsCol = db.getCollection("Posts");
        tagsCol = db.getCollection("Tags");
        votesCol = db.getCollection("Votes");
    }

    private void initCollections() {
        postsCol = db.getCollection("Posts");
        tagsCol = db.getCollection("Tags");
        votesCol = db.getCollection("Votes");

        postsCol.drop(); // drops then gets new collections
        tagsCol.drop();
        votesCol.drop();

        postsCol = db.getCollection("Posts");
        tagsCol = db.getCollection("Tags");
        votesCol = db.getCollection("Votes");
    }

    /**
     * Attempts to load into collection a Posts.json file
     */
    private void initCollectionData(MongoCollection<Document> col, String filename) {
        int count = 0;
        int batch = 100;

        List<InsertOneModel<Document>> docs = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            for(int i = 0; i < 3; i++)      // skip first 3 lines of posts
                br.readLine();

            String line;
            StringBuilder sb = new StringBuilder();
            String jsonObject;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                sb.append(line);

                if(line.compareTo("},") == 0) {
                    sb.deleteCharAt(sb.length() - 1); // remove last index
                    jsonObject = col == postsCol ? parsePostTerms(sb.toString()) : sb.toString();
                    docs.add(new InsertOneModel<>(Document.parse(jsonObject)));
                    sb.setLength(0);
                    count++;
                } else if (line.compareTo("}") == 0) {
                    jsonObject = col == postsCol ? parsePostTerms(sb.toString()) : sb.toString();
                    docs.add(new InsertOneModel<>(Document.parse(jsonObject)));
                    sb.setLength(0);
                    count++;
                    break;
                }

                if (count == batch) {
                    col.bulkWrite(docs, new BulkWriteOptions().ordered(false));
                    docs.clear();
                    count = 0;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("COULD FIND FILE : " + filename +
                    "\nAre you sure you have that filename in the same directory as this jar?");
        } catch (IOException e) {
            System.out.println("Encountered a fatal I/O error\n" +
                    "Please restart the app unless " + filename + " does not need to be loaded");
            e.printStackTrace();
        }

        if (count > 0) {
            postsCol.bulkWrite(docs, new BulkWriteOptions().ordered(false));
        }
    }

    /**
     * takes in some json representation and parses it to post object
     * then program adds new field terms into posts
     * @param s the string to convert to post
     * @return the new json string representaiton
     */
    private String parsePostTerms(String s) {
        long startTime = System.currentTimeMillis();

        Post p = gson.fromJson(s, Post.class);

        Set<String> terms = new HashSet<>();
        String[] words = new String[]{};

        if(p.Title != null) {
            words = p.Title.split("\\W+");
            for(String word : words) {
                if(word.length() >= 3)
                    terms.add(word.toLowerCase());
            }
        }

        if(p.Body != null) {
            words = p.Body.split("\\W+");
            for(String word : words) {
                if(word.length() >= 3)
                    terms.add(word.toLowerCase());
            }
        }

        if (p.Tags != null) {
            words = p.Tags.split("\\W+");
            for (String word : words) {
                if (!word.isEmpty())
                    terms.add(word.toLowerCase());
            }

        }

        p.Terms = new ArrayList<>(terms);

        timeForParsingTerms += System.currentTimeMillis() - startTime;
        return gson.toJson(p);
    }

    /**
     * creates indicies here
     */
    private void createIndexes() {
        postsCol.createIndex(Indexes.text("Terms"));
//        postsCol.createIndex(Indexes.compoundIndex(Indexes.text("Title"), Indexes.text("Body")));
    }

    public boolean postQuestion(String uid, String type, String tile, String body, String[] tags) {
        String formattedTags = "";
        for (int i = 0; i < tags.length; i++) {
            formattedTags += "<" + tags[i].trim() + ">";
            Bson filter = eq("TagName", tags[i].trim());
            Bson update = inc("Count", 1);
            Document updateResult = tagsCol.findOneAndUpdate(filter, update);
            if (updateResult == null) {
                Document tag = new Document("_id", new ObjectId());
                tag.append("Id", Utils.generateID(21)).append("TagName", tags[i].trim()).append("Count", 1);
                tagsCol.insertOne(tag);
            }
        }
        ArrayList<String> terms = getTerms(tile, body);
        String datePosted = new Date().toString();

        Document post = new Document("_id", new ObjectId());
        post.append("Id", Utils.generateID(21)).append("Tags", formattedTags)
                .append("PostTypeId", type).append("CreationDate", datePosted).append("OwnerUserId", uid)
                .append("Score", 0).append("ViewCount", 0).append("AnswerCount", 0).append("CommentCount", 0)
                .append("FavoriteCount", 0).append("ContentLicense", "CC BY-SA 2.5").append("Title", tile)
                .append("Body", body).append("Terms", terms).append("AcceptedAnswerId", null);
        postsCol.insertOne(post);
        return true;
    }

    public boolean postAnswer(String uid, String type, String answer, String quid){
        ArrayList<String> terms = getTerms(answer, "");
        String datePosted = new Date().toString();
        Document post = new Document("_id", new ObjectId());
        post.append("Id", Utils.generateID(21)).append("PostTypeId", type).append("CreationDate", datePosted)
            .append("OwnerUserId", uid).append("ParentId", quid).append("Score", 0).append("Body", answer)
            .append("CommentCount", 0).append("ContentLicense", "CC BY-SA 2.5").append("Terms", terms);
        postsCol.insertOne(post);
        return true;
    }

    public boolean vote(String uid, String type, String pid){
        String datePosted = new Date().toString();
        Document vote = new Document("_id", new ObjectId());
        if (uid != null){
            vote.append("Id", Utils.generateID(21)).append("VoteTypeId", type).append("CreationDate", datePosted)
                .append("PostId", pid).append("OwnerUserId", uid);
        }
        else {
            vote.append("Id", Utils.generateID(21)).append("VoteTypeId", type).append("CreationDate", datePosted)
                .append("PostId", pid);
        }
        votesCol.insertOne(vote);
        return true;
    }

    public FindIterable<Document> getPostsBy(String uid) {
        // q owned // a owned // q votes // a votes
        double[] res = new double[]{0, 0, 0, 0};

        Bson fil = and(eq("OwnerUserId", uid));
        return postsCol.find(fil);
    }

    public FindIterable<Document> getAnswersToQuestion(String parentId) {
        Bson fil = and(eq("PostTypeId", "2"), eq("ParentId", parentId));
        return postsCol.find(fil);
    }

    public FindIterable<Document> getAnswer(String id) {
        Bson fil = and(eq("Id", id));
        return postsCol.find(fil);
    }

    public FindIterable<Document> getVotesInPosts(Iterable<String> items) {
        Bson fil = in("PostId", items);
        return votesCol.find(fil);
    }

    public FindIterable<Document> getVotesInPost(String id) {
        Bson fil = and(eq("PostId", id));
        return votesCol.find(fil);
    }

    public FindIterable<Document> getPostById(String id) {
        Bson fil = eq("Id", id);
        return postsCol.find(fil).limit(1);
    }

    private ArrayList<String> getTerms(String title, String body) {
        Set<String> terms = new HashSet<>();
        String[] words = new String[]{};

        if(title != null) {
            words = title.split("\\W+");
            for(String word : words) {
                if(word.length() >= 3)
                    terms.add(word.toLowerCase());
            }
        }

        if(body != null) {
            words = body.split("\\W+");
            for(String word : words) {
                if(word.length() >= 3)
                    terms.add(word.toLowerCase());
            }
        }
        return new ArrayList<>(terms);
    }

    public List<Document> search(String[] keywords) {
        boolean allGT3 = true;
        String search = "";
        for (String key : keywords) {
            search += key + " ";
            if (key.length() < 3) {
                allGT3 = false;
            }
        }
        search.trim();

        List<Document> results = new ArrayList<>();
        if (allGT3) {
            Bson filter = and(eq("PostTypeId", "1"), text(search));
            postsCol.find(filter).forEach((Block<? super Document>) document -> results.add(document));
        } else {
            Bson filter = eq("PostTypeId", "1");
            postsCol.find(filter).forEach((Block<? super Document>) document -> {
                for (String key : keywords) {
                    String title = ((String) document.get("Title"));
                    String body = ((String) document.get("Body"));
                    String tags = ((String) document.get("Tags"));
                    boolean inTitle = title == null ? false : title.contains(key);
                    boolean inBody = body == null ? false : body.contains(key);
                    boolean inTags = tags == null ? false : tags.contains(key);
                    if (inTags || inBody || inTitle ) {
                        results.add(document);
                        break;
                    }
                }

            });
        }

        return results;

    }

    public void incrementViewCount(ObjectId id) {
        postsCol.updateOne(eq("_id", new ObjectId(String.valueOf(id))), inc("ViewCount", 1));
    }

    public void incrementScore(ObjectId id) {
        postsCol.updateOne(eq("_id", new ObjectId(String.valueOf(id))), inc("Score", 1));
    }

    public void incrementAnswers(ObjectId id) {
        postsCol.updateOne(eq("_id", new ObjectId(String.valueOf(id))), inc("AnswerCount", 1));
    }
}