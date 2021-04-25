package core.utils;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.json.JSONObject;

import java.util.HashMap;

public class Mongodb {
    private final MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private HashMap<String, MongoCollection<Document>> collectionHashMap;

    public Mongodb(JSONObject mongoConfig) {
        MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder();
        JSONObject optionsConfig = mongoConfig.getJSONObject("options");
        JSONObject connectConfig = mongoConfig.getJSONObject("connect");
        JSONObject authConfig = mongoConfig.getJSONObject("auth");

        if (optionsConfig.has("socketTimeout")) {
            optionsBuilder.maxConnectionIdleTime(optionsConfig.getInt("socketTimeout"));
        }
        if (optionsConfig.has("maxConnectionIdleTime")) {
            optionsBuilder.maxConnectionIdleTime(optionsConfig.getInt("maxConnectionIdleTime"));
        }
        if (optionsConfig.has("maxConnectionLifeTime")) {
            optionsBuilder.maxConnectionIdleTime(optionsConfig.getInt("maxConnectionLifeTime"));
        }
        if (optionsConfig.has("minConnectionsPerHost")) {
            optionsBuilder.maxConnectionIdleTime(optionsConfig.getInt("minConnectionsPerHost"));
        }
        if (optionsConfig.has("connectionsPerHost")) {
            optionsBuilder.maxConnectionIdleTime(optionsConfig.getInt("connectionsPerHost"));
        }
        MongoClientOptions options = optionsBuilder.build();
        ServerAddress address = new ServerAddress(connectConfig.getString("ip"), connectConfig.getInt("port"));
        MongoCredential mongoCredential = MongoCredential.createCredential(authConfig.getString("username"), authConfig.getString("db"), authConfig.getString("password").toCharArray());
        mongoClient = new MongoClient(address, mongoCredential, options);
    }

    public long getMaxBlock(String collectionName) {
        MongoCollection<Document> collection = getCollection(collectionName);
        long number = 0;
        BasicDBObject dbObject = new BasicDBObject();
        dbObject.put("number", -1);
        MongoCursor<Document> mongoCursor = collection.find().sort(dbObject).limit(1).iterator();
        if (mongoCursor.hasNext()) {
            Document doc = mongoCursor.next();
            number = doc.getLong("number");
        }
        return number;
    }


    public MongoCollection<Document> getCollection(String collectionName) {
        return this.collectionHashMap.get(collectionName);
    }


}
