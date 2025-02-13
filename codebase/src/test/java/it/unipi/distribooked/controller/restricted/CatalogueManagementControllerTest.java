package it.unipi.distribooked.controller.restricted;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.distribooked.model.Author;
import it.unipi.distribooked.model.Book;
import it.unipi.distribooked.model.embedded.Address;
import it.unipi.distribooked.model.embedded.EmbeddedAuthor;
import it.unipi.distribooked.model.embedded.EmbeddedBookAuthor;
import it.unipi.distribooked.model.embedded.EmbeddedLibrary;
import it.unipi.distribooked.repository.mongo.AuthorRepository;
import it.unipi.distribooked.repository.mongo.BookRepository;
import it.unipi.distribooked.repository.mongo.LibraryRepository;
import it.unipi.distribooked.repository.redis.RedisBookRepository;
import it.unipi.distribooked.dto.BookDTO;
import it.unipi.distribooked.dto.LibraryDTO;
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
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc

class CatalogueManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private RedisBookRepository redisBookRepository;

    @Autowired
    private LibraryRepository libraryRepository;

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

    private BookDTO bookDTO;
    private LibraryDTO library;
    private LibraryDTO completeLibrary;
    private Book book1, book2, book3, book4, book5, book6;
    private EmbeddedLibrary library1, library2, library3;



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
        ObjectId libraryId3 = new ObjectId("74b87f1a2d3b9c1234567892");
        ObjectId libraryId4 = new ObjectId("74b87f1a2d3b9c1234567893");

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
        Address address3 = new Address("Via Garibaldi, 1", "Rome", "RM", "00100", "Italy");

        Author author1 = new Author(authorId1, "Joshua Bloch", "10-28-1961", "28-12-2020", "https://example.com/joshua-bloch.jpg",
                "American", List.of(Ebook1));
        Author author2 = new Author(authorId2, "Robert C. Martin", "10-28-1952", "28-12-2020", "https://example.com/robert-c-martin.jpg",
                "American", List.of(Ebook1, Ebook2));


        library1 = new EmbeddedLibrary(libraryId1, "Central Library", Map.of("Position", location1), address1, 10);
        library2 = new EmbeddedLibrary(libraryId2, "Tech Library", Map.of("Position", location2), address2, 36);
        library3 = new EmbeddedLibrary(libraryId3, "Big Library", Map.of("Position", location2), address3, 20);

        completeLibrary = new LibraryDTO(libraryId4.toString(), "Big Library2", address3, "Downtown", "12345",
                "Province A", "Region B", "654321", 45.123456, 12.654321, "123-456-7890", "completeLibrary@example.com", "http://biglibrary.com");

        bookDTO = new BookDTO("64b87f1a2d3b9c1234567890", "Effective Java", "Programming Best Practices", "2008-05-08", "English",
                List.of("Programming", "Java"), "0134685997", "9780134685991", "Addison-Wesley",
                "https://example.com/effective-java.jpg", List.of(Eauthor1, Eauthor2), null, 120);

        book1 = new Book(bookId1, "Effective Java", "Best practices for Java", "2008-05-08", "English",
                List.of("Programming", "Java"), "0134685997", "9780134685991", "Addison-Wesley", "https://example.com/effective-java.jpg",
                authors1, List.of(library1, library2), 120);

        book2 = new Book(bookId2, "Clean Code", "A Handbook of Agile Software Craftsmanship", "2008-08-01", "English",
                List.of("Software Engineering", "Best Practices"), "0132350882", "9780132350884", "Prentice Hall", "https://example.com/clean-code.jpg",
                authors2, List.of(library1, library2), 120);
        book3 = new Book(bookId3, "Refactoring", "Improving the Design of Existing Code", "1999-07-08", "English",
                List.of("Software Engineering", "Refactoring"), "0201485672", "9780201485677", "Addison-Wesley", "https://example.com/refactoring.jpg",
                authors3, List.of(library1, library2), 120);
        book4 = new Book(bookId4, "Design Patterns", "Elements of Reusable Object-Oriented Software", "1994-10-21", "English",
                List.of("Software Architecture", "Design Patterns"), "0201633612", "9780201633610", "Addison-Wesley", "https://example.com/design-patterns.jpg",
                authors4, List.of(library1, library2), 120);
        book5 = new Book(bookId5, "Extreme Programming Explained", "Embrace Change", "1999-10-20", "English",
                List.of("Software Engineering", "Extreme Programming"), "0201616416", "9780201616415", "Addison-Wesley", "https://example.com/extreme-programming.jpg",
                authors5, List.of(library1, library2), 120);
        book6 = new Book(bookId6, "Test Driven Development", "By Example", "2002-11-18", "English",
                List.of("Software Engineering", "TDD"), "0321146530", "9780321146533", "Addison-Wesley", "https://example.com/tdd.jpg",
                authors6, List.of(library1, library2), 120);

        authorRepository.saveAll(List.of(author1, author2));

        bookRepository.saveAll(List.of(book1, book2, book3, book4, book5, book6));

        redisBookRepository.addLibraryToBookAvailability(book1.getId().toHexString(), library1.getId().toString(), 10);
        redisBookRepository.addLibraryToBookAvailability(book1.getId().toHexString(), library2.getId().toString(), 36);
        redisBookRepository.addLibraryToBookAvailability(book2.getId().toHexString(), library1.getId().toString(), 10);
        redisBookRepository.addLibraryToBookAvailability(book2.getId().toHexString(), library2.getId().toString(), 36);
        redisBookRepository.addLibraryToBookAvailability(book3.getId().toHexString(), library1.getId().toString(), 10);
        redisBookRepository.addLibraryToBookAvailability(book3.getId().toHexString(), library2.getId().toString(), 36);
        redisBookRepository.addLibraryToBookAvailability(book4.getId().toHexString(), library1.getId().toString(), 10);
        redisBookRepository.addLibraryToBookAvailability(book4.getId().toHexString(), library2.getId().toString(), 36);
        redisBookRepository.addLibraryToBookAvailability(book5.getId().toHexString(), library1.getId().toString(), 10);
        redisBookRepository.addLibraryToBookAvailability(book5.getId().toHexString(), library2.getId().toString(), 36);
        redisBookRepository.addLibraryToBookAvailability(book6.getId().toHexString(), library1.getId().toString(), 10);
        redisBookRepository.addLibraryToBookAvailability(book6.getId().toHexString(), library2.getId().toString(), 36);

    }


    @AfterEach
    void cleanUp() {
        bookRepository.deleteAll();
        authorRepository.deleteAll();

        redisBookRepository.removeLibraryEntry(book1.getId().toHexString(), library1.getId().toString(), 10);
        redisBookRepository.removeLibraryEntry(book1.getId().toHexString(), library2.getId().toString(), 36);
        redisBookRepository.removeLibraryEntry(book2.getId().toHexString(), library1.getId().toString(), 10);
        redisBookRepository.removeLibraryEntry(book2.getId().toHexString(), library2.getId().toString(), 36);
        redisBookRepository.removeLibraryEntry(book3.getId().toHexString(), library1.getId().toString(), 10);
        redisBookRepository.removeLibraryEntry(book3.getId().toHexString(), library2.getId().toString(), 36);
        redisBookRepository.removeLibraryEntry(book4.getId().toHexString(), library1.getId().toString(), 10);
        redisBookRepository.removeLibraryEntry(book4.getId().toHexString(), library2.getId().toString(), 36);
        redisBookRepository.removeLibraryEntry(book5.getId().toHexString(), library1.getId().toString(), 10);
        redisBookRepository.removeLibraryEntry(book5.getId().toHexString(), library2.getId().toString(), 36);
        redisBookRepository.removeLibraryEntry(book6.getId().toHexString(), library1.getId().toString(), 10);
        redisBookRepository.removeLibraryEntry(book6.getId().toHexString(), library2.getId().toString(), 36);
    }

    @AfterAll
    static void tearDown() {
        mongoDBContainer.stop();
        redisContainer.stop();

        mongoDBContainer.close();
        redisContainer.close();
    }


    @Test
    void testAddBook() throws Exception {

        String json = objectMapper.writeValueAsString(bookDTO);

        mockMvc.perform(post("/api/v1/admin/catalogue/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andDo(print());
    }


    @Test
    void testRemoveBookFromLibrary() throws Exception {
        String bookId = book1.getId().toHexString();
        String libraryId = library2.getId().toHexString();

        mockMvc.perform(delete("/api/v1/admin/catalogue/books/" + bookId + "/libraries/" + libraryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());

        // Necessaria riaggiunta per evitare errori di rimozione duplicata
        redisBookRepository.addLibraryToBookAvailability(bookId, libraryId, 36);
    }




    @Test
    void testIncrementAvailableCopies() throws Exception {
        String bookId = book5.getId().toHexString();
        String libraryId = library2.getId().toHexString();

        mockMvc.perform(patch("/api/v1/admin/catalogue/books/" + bookId + "/libraries/" + libraryId + "/increment")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());

        // Necessaria eliminazione e riaggiunta per evitare errori di rimozione duplicata
        redisBookRepository.removeLibraryEntry(bookId, libraryId, 37);
        redisBookRepository.addLibraryToBookAvailability(bookId, libraryId, 36);
    }




    @Test
    void testDecrementAvailableCopies() throws Exception {
        String bookId = book3.getId().toHexString();
        String libraryId = library1.getId().toHexString();

        mockMvc.perform(patch("/api/v1/admin/catalogue/books/" + bookId + "/libraries/" + libraryId + "/decrement")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());

        // Necessaria eliminazione e riaggiunta per evitare errori di rimozione duplicata
        redisBookRepository.removeLibraryEntry(bookId, libraryId, 9);
        redisBookRepository.addLibraryToBookAvailability(bookId, libraryId, 10);
    }




    @Test
    void testAddLibraryToBookAvailability() throws Exception {
        String bookId = book1.getId().toHexString();
        String libraryId = library3.getId().toHexString();
        int copies = 20;

        mockMvc.perform(post("/api/v1/admin/catalogue/books/" + bookId + "/libraries/" + libraryId)
                        .param("initialValue", String.valueOf(copies))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());

        // Necessaria rimozione per coerenza con il test
        redisBookRepository.removeLibraryEntry(bookId, libraryId, copies);
    }




    @Test
    void testAddLibrary() throws Exception {
        String json = objectMapper.writeValueAsString(completeLibrary);

        mockMvc.perform(post("/api/v1/admin/catalogue/libraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andDo(print());

        // Necessaria rimozione per coerenza con il test
        libraryRepository.deleteAll();

    }
}