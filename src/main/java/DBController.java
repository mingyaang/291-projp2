import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DBController {
    Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
    MongoClient mongoClient;
    MongoDatabase db;
    MongoCollection<Document> postsCol;
    MongoCollection<Document> tagsCol;
    MongoCollection<Document> votesCol;

    public DBController(int port) {
//        mongoLogger.setLevel(Level.SEVERE); // prevents log popup

        mongoClient = new MongoClient("localhost", port);
        db = mongoClient.getDatabase("291db");
        initCollections();

        Document doc = new Document("name", "mongo").append("SAMPLENEW", "TEST");
        postsCol.insertOne(doc);
        tagsCol.insertOne(doc);
        votesCol.insertOne(doc);

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

}
