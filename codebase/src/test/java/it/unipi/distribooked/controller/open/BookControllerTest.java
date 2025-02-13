package it.unipi.distribooked.controller.open;

import it.unipi.distribooked.repository.redis.RedisBookRepository;
import it.unipi.distribooked.model.Author;
import it.unipi.distribooked.model.Book;
import it.unipi.distribooked.model.embedded.Address;
import it.unipi.distribooked.model.embedded.EmbeddedAuthor;
import it.unipi.distribooked.model.embedded.EmbeddedBookAuthor;
import it.unipi.distribooked.model.embedded.EmbeddedLibrary;
import it.unipi.distribooked.repository.mongo.AuthorRepository;
import it.unipi.distribooked.repository.mongo.BookRepository;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



@SpringBootTest
@AutoConfigureMockMvc

class BookControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private RedisBookRepository redisBookRepository;


    @Container
    @ServiceConnection(name = "mongo")
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");


    @Container
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


    private EmbeddedLibrary library1, library2;
    private Book book1, book2, book3, book4, book5, book6;



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

        ObjectId libraryId1 = new ObjectId("74b87f1a2d3b9c1234567890");
        ObjectId libraryId2 = new ObjectId("74b87f1a2d3b9c1234567891");

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


        Author author1 = new Author(authorId1, "Joshua Bloch", "10-28-1961", "28-12-2020", "https://example.com/joshua-bloch.jpg",
                "American", List.of(Ebook1));
        Author author2 = new Author(authorId2, "Robert C. Martin", "10-28-1952", "28-12-2020", "https://example.com/robert-c-martin.jpg",
                "American", List.of(Ebook1, Ebook4));
        Author author3 = new Author(authorId3, "Martin Fowler", "10-28-1963", "28-12-2020", "https://example.com/martin-fowler.jpg",
                "British", List.of(Ebook3));
        Author author4 = new Author(authorId4, "Kent Beck", "10-28-1961", "28-12-2020", "https://example.com/kent-beck.jpg",
                "American", List.of(Ebook2, Ebook6));
        Author author5 = new Author(authorId5, "Erich Gamma", "10-28-1961", "28-12-2020", "https://example.com/erich-gamma.jpg",
                "Swiss", List.of(Ebook2, Ebook4));
        Author author6 = new Author(authorId6, "John Vlissides", "10-28-1961", "28-12-2020", "https://example.com/john-vlissides.jpg",
                "American", List.of(Ebook3, Ebook5));
        Author author7 = new Author(authorId7, "Richard Helm", "10-28-1961", "28-12-2020", "https://example.com/richard-helm.jpg",
                "American", List.of(Ebook5, Ebook6));

        Map<String, Object> location1 = new HashMap<>();
        location1.put("type", "Point");
        location1.put("coordinates", new double[]{80.152, 13.205});

        Map<String, Object> location2 = new HashMap<>();
        location2.put("type", "Point");
        location2.put("coordinates", new double[]{77.594, 12.971});

        List<EmbeddedAuthor> authors1 = List.of(Eauthor1, Eauthor2);
        List<EmbeddedAuthor> authors2 = List.of(Eauthor4, Eauthor5);
        List<EmbeddedAuthor> authors3 = List.of(Eauthor6, Eauthor3);
        List<EmbeddedAuthor> authors4 = List.of(Eauthor2, Eauthor5);
        List<EmbeddedAuthor> authors5 = List.of(Eauthor7, Eauthor6);
        List<EmbeddedAuthor> authors6 = List.of(Eauthor4, Eauthor7);


        Address address1 = new Address("Via Roma, 10", "Firenze", "FI", "50123", "Italy");
        Address address2 = new Address("Corso Vittorio Emanuele, 45", "Milano", "MI", "20121", "Italy");


        library1 = new EmbeddedLibrary(libraryId1, "Central Library", Map.of("Position", location1), address1, 4);
        library2 = new EmbeddedLibrary(libraryId2, "Tech Library", Map.of("Position", location2), address2, 3);


        book1 = new Book(bookId1, "Effective Java", "Best practices for Java", "2008-05-08", "English",
                List.of("Programming", "Java"), "0134685997", "9780134685991", "Addison-Wesley", "https://example.com/effective-java.jpg",
                authors1, List.of(library1, library2), 100);

        book2 = new Book(bookId2, "Clean Code", "A Handbook of Agile Software Craftsmanship", "2008-08-01", "English",
                List.of("Software Engineering", "Best Practices"), "0132350882", "9780132350884", "Prentice Hall", "https://example.com/clean-code.jpg",
                authors2, List.of(library1, library2), 200);
        book3 = new Book(bookId3, "Refactoring", "Improving the Design of Existing Code", "1999-07-08", "English",
                List.of("Software Engineering", "Refactoring"), "0201485672", "9780201485677", "Addison-Wesley", "https://example.com/refactoring.jpg",
                authors3, List.of(library1, library2), 20);
        book4 = new Book(bookId4, "Design Patterns", "Elements of Reusable Object-Oriented Software", "1994-10-21", "English",
                List.of("Software Architecture", "Design Patterns"), "0201633612", "9780201633610", "Addison-Wesley", "https://example.com/design-patterns.jpg",
                authors4, List.of(library1, library2), 75);
        book5 = new Book(bookId5, "Extreme Programming Explained", "Embrace Change", "1999-10-20", "English",
                List.of("Software Engineering", "Extreme Programming"), "0201616416", "9780201616415", "Addison-Wesley", "https://example.com/extreme-programming.jpg",
                authors5, List.of(library1, library2), 120);
        book6 = new Book(bookId6, "Test Driven Development", "By Example", "2002-11-18", "English",
                List.of("Software Engineering", "TDD"), "0321146530", "9780321146533", "Addison-Wesley", "https://example.com/tdd.jpg",
                authors6, List.of(library1, library2), 150);


        redisBookRepository.addLibraryToBookAvailability(book1.getId().toHexString(), library1.getId().toHexString(), 10);
        redisBookRepository.addLibraryToBookAvailability(book1.getId().toHexString(), library2.getId().toHexString(), 2);
        redisBookRepository.addLibraryToBookAvailability(book2.getId().toHexString(), library1.getId().toHexString(), 15);
        redisBookRepository.addLibraryToBookAvailability(book2.getId().toHexString(), library2.getId().toHexString(), 8);
        redisBookRepository.addLibraryToBookAvailability(book3.getId().toHexString(), library1.getId().toHexString(), 25);
        redisBookRepository.addLibraryToBookAvailability(book3.getId().toHexString(), library2.getId().toHexString(), 4);
        redisBookRepository.addLibraryToBookAvailability(book4.getId().toHexString(), library1.getId().toHexString(), 36);
        redisBookRepository.addLibraryToBookAvailability(book4.getId().toHexString(), library2.getId().toHexString(), 16);
        redisBookRepository.addLibraryToBookAvailability(book5.getId().toHexString(), library1.getId().toHexString(), 8);
        redisBookRepository.addLibraryToBookAvailability(book5.getId().toHexString(), library2.getId().toHexString(), 3);
        redisBookRepository.addLibraryToBookAvailability(book6.getId().toHexString(), library1.getId().toHexString(), 17);
        redisBookRepository.addLibraryToBookAvailability(book6.getId().toHexString(), library2.getId().toHexString(), 13);

        authorRepository.saveAll(List.of(author1, author2, author3, author4, author5, author6, author7));
        bookRepository.saveAll(List.of(book1, book2, book3, book4, book5, book6));
    }

    @AfterEach
    void cleanUp() {

        bookRepository.deleteAll();
        authorRepository.deleteAll();

        redisBookRepository.removeLibraryEntry(book1.getId().toHexString(), library1.getId().toHexString(), 10);
        redisBookRepository.removeLibraryEntry(book1.getId().toHexString(), library2.getId().toHexString(), 2);
        redisBookRepository.removeLibraryEntry(book2.getId().toHexString(), library1.getId().toHexString(), 15);
        redisBookRepository.removeLibraryEntry(book2.getId().toHexString(), library2.getId().toHexString(), 8);
        redisBookRepository.removeLibraryEntry(book3.getId().toHexString(), library1.getId().toHexString(), 25);
        redisBookRepository.removeLibraryEntry(book3.getId().toHexString(), library2.getId().toHexString(), 4);
        redisBookRepository.removeLibraryEntry(book4.getId().toHexString(), library1.getId().toHexString(), 36);
        redisBookRepository.removeLibraryEntry(book4.getId().toHexString(), library2.getId().toHexString(), 16);
        redisBookRepository.removeLibraryEntry(book5.getId().toHexString(), library1.getId().toHexString(), 8);
        redisBookRepository.removeLibraryEntry(book5.getId().toHexString(), library2.getId().toHexString(), 3);
        redisBookRepository.removeLibraryEntry(book6.getId().toHexString(), library1.getId().toHexString(), 17);
        redisBookRepository.removeLibraryEntry(book6.getId().toHexString(), library2.getId().toHexString(), 13);

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
    void testGetBooks() throws Exception {

        mockMvc.perform(get("/api/v1/books")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isNotEmpty())
                .andDo(print());
    }



    // DONE
    @Test
    void testSearchBooks() throws Exception {
        mockMvc.perform(get("/api/v1/books/search")
                        .param("title", "Effective Java")
                        .param("author", "Joshua Bloch")
                        .param("page", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }




    // DONE
    @Test
    void testGetBookByIdWithLibraries() throws Exception {
        String Id = book5.getId().toHexString();

        mockMvc.perform(get("/api/v1/books/" + Id)
                        .param("latitude", "13.205")
                        .param("longitude", "80.152")
                        .param("radius", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.data.id").value(Id));

    }



    // DONE
    @Test
    void testFilterBooks() throws Exception {

        mockMvc.perform(get("/api/v1/books/filter")
                        .param("category", "Software Engineering")
                        .param("sortByPopularity", "false")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }






    @Test
    void testGetBookAvailability() throws Exception {

        String bookId = book6.getId().toHexString();
        String libraryId = library1.getId().toHexString();

        mockMvc.perform(get(("/api/v1/books/" + bookId + "/availability/" + libraryId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }
}
