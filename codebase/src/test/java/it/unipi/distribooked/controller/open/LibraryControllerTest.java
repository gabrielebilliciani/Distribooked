package it.unipi.distribooked.controller.open;

import it.unipi.distribooked.model.Library;
import it.unipi.distribooked.model.embedded.Address;
import it.unipi.distribooked.repository.mongo.LibraryRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



@SpringBootTest
@AutoConfigureMockMvc

class LibraryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LibraryRepository libraryRepository;

    @ServiceConnection(name = "mongo")
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));


    @ServiceConnection(name = "redis")
    private static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.4.2"))
            .withExposedPorts(6379);


    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    static {
        mongoDBContainer.start();
        redisContainer.start();
    }


    private Library library1, library2;

    @BeforeAll
    static void setup() {
        System.setProperty("spring.profiles.active", "test");
    }

    @BeforeEach
    void setUp() {

        ObjectId libraryId1 = new ObjectId("74b87f1a2d3b9c1234567890");
        ObjectId libraryId2 = new ObjectId("74b87f1a2d3b9c1234567891");

        Address address1 = new Address("Via Roma, 10", "Firenze", "FI", "50123", "Italy");
        Address address2 = new Address("Corso Vittorio Emanuele, 45", "Milano", "MI", "20121", "Italy");

        GeoJsonPoint location1 = new GeoJsonPoint(12.34, 56.78);
        GeoJsonPoint location2 = new GeoJsonPoint(77.594, 12.971);

        library1 = new Library(libraryId1, "Central Library", address1, "Centro", "048017", "Firenze", "048", "FI",
                            "Toscana", "051", location1, "+39 0551234567", "centrallibrary@example.com", "http//www.centrallibrary.com");
        library2 = new Library(libraryId2, "City Library", address2, "Centro", "20121", "Milano", "015", "MI",
                            "Lombardia", "003", location2, "+39 02 1234567", "citylibrary@example.com", "http//www.citylibrary.com");

        libraryRepository.saveAll(List.of(library1, library2));
    }

    @AfterEach
    void cleanUp() {
        libraryRepository.deleteAll();
    }

    @AfterAll
    static void tearDown() {
        mongoDBContainer.stop();
        redisContainer.stop();

        mongoDBContainer.close();
        redisContainer.close();
    }



    // DONE
    @Test
    void testGetLibraryById() throws Exception {
        String libraryId = library1.getId().toHexString();

        mockMvc.perform(get("/api/v1/libraries/" + libraryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(libraryId))
                .andDo(print());
    }
}