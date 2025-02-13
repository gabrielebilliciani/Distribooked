package it.unipi.distribooked.controller.open;

import it.unipi.distribooked.model.Author;
import it.unipi.distribooked.model.embedded.EmbeddedAuthor;
import it.unipi.distribooked.model.embedded.EmbeddedBookAuthor;
import it.unipi.distribooked.repository.mongo.AuthorRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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

@SpringBootTest()
@AutoConfigureMockMvc

class AuthorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @ServiceConnection(name = "mongo")
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

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



    @Autowired
    private AuthorRepository authorRepository;

    private Author author1, author2, author3, author4, author5, author6, author7;

    @BeforeAll
    static void setup() {
        System.setProperty("spring.profiles.active", "test");
    }

    @BeforeEach
    void setUp() {

        ObjectId bookId1 = new ObjectId("54b87f1a2d3b9c1234567890");
        ObjectId bookId2 = new ObjectId("54b87f1a2d3b9c1234567891");
        ObjectId bookId3 = new ObjectId("54b87f1a2d3b9c1234567892");
        ObjectId bookId4 = new ObjectId("54b87f1a2d3b9c1234567893");
        ObjectId bookId5 = new ObjectId("54b87f1a2d3b9c1234567894");
        ObjectId bookId6 = new ObjectId("54b87f1a2d3b9c1234567895");


        ObjectId authorId1 = new ObjectId("64b87f1a2d3b9c1234567890");
        ObjectId authorId2 = new ObjectId("64b87f1a2d3b9c1234567891");
        ObjectId authorId3 = new ObjectId("64b87f1a2d3b9c1234567892");
        ObjectId authorId4 = new ObjectId("64b87f1a2d3b9c1234567893");
        ObjectId authorId5 = new ObjectId("64b87f1a2d3b9c1234567894");
        ObjectId authorId6 = new ObjectId("64b87f1a2d3b9c1234567895");
        ObjectId authorId7 = new ObjectId("64b87f1a2d3b9c1234567896");


        EmbeddedAuthor Eauthor1 = new EmbeddedAuthor(authorId1, "Joshua Bloch");
        EmbeddedAuthor Eauthor2 = new EmbeddedAuthor(authorId2, "Robert C. Martin");
        EmbeddedAuthor Eauthor3 = new EmbeddedAuthor(authorId3, "Martin Fowler");
        EmbeddedAuthor Eauthor4 = new EmbeddedAuthor(authorId4, "Kent Beck");
        EmbeddedAuthor Eauthor5 = new EmbeddedAuthor(authorId5, "Erich Gamma");
        EmbeddedAuthor Eauthor6 = new EmbeddedAuthor(authorId6, "John Vlissides");
        EmbeddedAuthor Eauthor7 = new EmbeddedAuthor(authorId7, "Richard Helm");

        EmbeddedBookAuthor Ebook1 = new EmbeddedBookAuthor(bookId1, "Effective Java", "Programming Best Practices", List.of(Eauthor1, Eauthor2),
                List.of("Programming", "Tech"), "cover1.jpg");
        EmbeddedBookAuthor Ebook2 = new EmbeddedBookAuthor(bookId2, "Clean Code", "A handbook of agile software craftsmanship", List.of(Eauthor4, Eauthor5),
                List.of("Software Engineering", "IoT"), "cover2.jpg");
        EmbeddedBookAuthor Ebook3 = new EmbeddedBookAuthor(bookId3, "Refactoring", "Improving the Design of Existing Code", List.of(Eauthor6, Eauthor3),
                List.of("Software Engineering", "Refactoring"), "cover3.jpg");
        EmbeddedBookAuthor Ebook4 = new EmbeddedBookAuthor(bookId4, "Design Patterns", "Elements of Reusable Object-Oriented Software", List.of(Eauthor2, Eauthor5),
                List.of("Software Architecture", "Design Patterns"), "cover4.jpg");
        EmbeddedBookAuthor Ebook5 = new EmbeddedBookAuthor(bookId5, "Extreme Programming Explained", "Embrace Change", List.of(Eauthor7, Eauthor6),
                List.of("Software Engineering", "Extreme Programming"), "cover5.jpg");
        EmbeddedBookAuthor Ebook6 = new EmbeddedBookAuthor(bookId6, "Test Driven Development", "By Example", List.of(Eauthor4, Eauthor7),
                List.of("Software Engineering", "TDD"), "cover6.jpg");

        author1 = new Author(authorId1, "Joshua Bloch", "10-02-1961", null, "joshua_bloch.jpg",
                        "Joshua Bloch is a software engineer and a technology author", List.of(Ebook1));
        author2 = new Author(authorId2, "Robert C. Martin", "05-12-1952", "12-10-2010", "robert_c_martin.jpg",
                        "Robert Cecil Martin is a software engineer and author", List.of(Ebook1, Ebook4));
        author3 = new Author(authorId3, "Martin Fowler", "18-12-1963", null, "martin_fowler.jpg",
                        "Martin Fowler is a software developer and author", List.of(Ebook3));
        author4 = new Author(authorId4, "Kent Beck", "31-03-1961", null, "kent_beck.jpg",
                        "Kent Beck is an American software engineer and the creator of Extreme Programming", List.of(Ebook2, Ebook6));
        author5 = new Author(authorId5, "Erich Gamma", "13-03-1961", null, "erich_gamma.jpg",
                        "Erich Gamma is a Swiss computer scientist and co-author of the book Design Patterns", List.of(Ebook2, Ebook4));
        author6 = new Author(authorId6, "John Vlissides", "02-08-1961", "24-11-2005", "john_vlissides.jpg",
                        "John Vlissides was a computer scientist known for his work on design patterns", List.of(Ebook3, Ebook5));
        author7 = new Author(authorId7, "Richard Helm", "01-07-1961", "12-09-1994", "richard_helm.jpg",
                        "Richard Helm was a computer scientist known for his work on design patterns", List.of(Ebook5, Ebook6));


        authorRepository.saveAll(List.of(author1, author2, author3, author4, author5, author6, author7));
    }

    @AfterEach
    void cleanUp() {
        authorRepository.deleteAll();
    }

    @AfterAll
    static void tearDown() {
        mongoDBContainer.stop();
        redisContainer.stop();

        mongoDBContainer.close();
        redisContainer.close();
    }



    @Test
    void testSearchAuthorsByName() throws Exception {

        String authorName = author3.getFullName();

        mockMvc.perform(get("/api/v1/authors/search")
                        .param("fullName", authorName)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.data.authors").isArray())
                .andExpect(jsonPath("$.data.authors[0].fullName").value(authorName));
    }


    @Test
    void testGetAuthorDetails() throws Exception {

        String authorID = author1.getId().toHexString();

        mockMvc.perform(get("/api/v1/authors/" + authorID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.data.author.id").value(authorID));
    }



    @Test
    void testGetAuthorBooks() throws Exception {

        String authorID = author2.getId().toHexString();

        mockMvc.perform(get("/api/v1/authors/" + authorID + "/books")
                        .param("page", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.data.books").isArray());

    }
}
