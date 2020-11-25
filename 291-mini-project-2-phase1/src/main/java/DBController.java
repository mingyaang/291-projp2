import com.google.gson.Gson;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.InsertOneModel;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private String regexPtn = "[\\s*.;<>+\\-_)(?,}{\"\\[\\]\\|]+";

    public DBController(int port) {
        mongoLogger.setLevel(Level.SEVERE); // prevents log popup
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
            for (int i = 0; i < 3; i++)      // skip first 3 lines of posts
                br.readLine();

            String line;
            StringBuilder sb = new StringBuilder();
            String jsonObject;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                sb.append(line);

                if (line.compareTo("},") == 0) {
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
     *
     * @param s the string to convert to post
     * @return the new json string representaiton
     */
    private String parsePostTerms(String s) {
        long startTime = System.currentTimeMillis();

        Post p = gson.fromJson(s, Post.class);

        Set<String> terms = new HashSet<>();
        String[] words = new String[]{};

        if (p.Title != null) {
            words = p.Title.split(regexPtn);
            for (String word : words) {
                if (word.length() >= 3)
                    terms.add(word.toLowerCase());
            }
        }

        if (p.Body != null) {
            words = p.Body.split(regexPtn);
            for (String word : words) {
                if (word.length() >= 3)
                    terms.add(word.toLowerCase());
            }
        }

        if (p.Tags != null) {
            words = p.Tags.split(regexPtn);
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
}