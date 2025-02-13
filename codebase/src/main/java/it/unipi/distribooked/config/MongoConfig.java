package it.unipi.distribooked.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import it.unipi.distribooked.model.Book;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;


@Configuration
@EnableMongoRepositories(basePackages = "it.unipi.distribooked.repository.mongo")
public class MongoConfig {

    // Reads the MongoDB URI from the application.properties file
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    // Defines a MongoClient bean to establish a connection to MongoDB
    @Bean
    @Profile({"local", "cluster"})
    public MongoClient mongoClient() {
        return MongoClients.create(mongoUri); // Creates a MongoClient using the provided URI
    }

    // Defines a MongoTemplate bean to interact with the MongoDB database
    @Bean
    @Profile({"local", "cluster"})
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), "library"); // Specifies the database name
    }

//    @EventListener(ContextRefreshedEvent.class)
//    public void initIndices() {
//        MongoTemplate mongoTemplate = mongoTemplate();
//        IndexOperations indexOps = mongoTemplate.indexOps(Book.class);
//
//        // Create geospatial index for library locations
//        indexOps.ensureIndex(new GeospatialIndex("branches.location")
//                .typed(GeoSpatialIndexType.GEO_2DSPHERE));
//
//        // Create any other indices defined by annotations
//        IndexResolver indexResolver = IndexResolver.create(mongoTemplate.getConverter().getMappingContext());
//        indexResolver.resolveIndexFor(Book.class).forEach(indexOps::ensureIndex);
//    }
}
