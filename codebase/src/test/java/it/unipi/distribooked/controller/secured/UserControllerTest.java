package it.unipi.distribooked.controller.secured;


import it.unipi.distribooked.model.*;
import it.unipi.distribooked.model.embedded.*;
import it.unipi.distribooked.model.enums.UserType;
import it.unipi.distribooked.repository.mongo.UserRepository;
import it.unipi.distribooked.repository.redis.LoanRepository;
import it.unipi.distribooked.repository.redis.RedisBookRepository;
import it.unipi.distribooked.repository.redis.ReservationRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private LoanRepository loanRepository;


    @Container
    @ServiceConnection(name = "mongo")
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");


    @Container
    @ServiceConnection(name = "redis")
    private static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.4.2"))
            .withExposedPorts(6379);
    @Autowired
    private RedisBookRepository redisBookRepository;


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


    private ObjectId userId;
    private EmbeddedLibrary library1, library2;
    private Book book1, book2, book3;


    @BeforeAll
    static void setup() {
        System.setProperty("spring.profiles.active", "test");
    }


    @BeforeEach
    void setUp() {

        userId = new ObjectId("64b87f1a2d3b9c1234567890");

        ObjectId bookId1 = new ObjectId("54b87f1a2d3b9c1234567890");
        ObjectId bookId2 = new ObjectId("54b87f1a2d3b9c1234567891");
        ObjectId bookId3 = new ObjectId("54b87f1a2d3b9c1234567892");

        ObjectId authorId1 = new ObjectId("84b87f1a2d3b9c1234567890");
        ObjectId authorId2 = new ObjectId("84b87f1a2d3b9c1234567891");
        ObjectId authorId3 = new ObjectId("84b87f1a2d3b9c1234567892");

        ObjectId libraryId1 = new ObjectId("74b87f1a2d3b9c1234567890");
        ObjectId libraryId2 = new ObjectId("74b87f1a2d3b9c1234567891");


        EmbeddedAuthor Eauthor1 = new EmbeddedAuthor(authorId1, "Joshua Bloch");
        EmbeddedAuthor Eauthor2 = new EmbeddedAuthor(authorId2, "Robert C. Martin");
        EmbeddedAuthor Eauthor3 = new EmbeddedAuthor(authorId3, "Martin Fowler");


        List<EmbeddedAuthor> authors1 = List.of(Eauthor1, Eauthor2);
        List<EmbeddedAuthor> authors2 = List.of(Eauthor1, Eauthor3);
        List<EmbeddedAuthor> authors3 = List.of(Eauthor2, Eauthor3);


        EmbeddedBookAuthor Ebook1 = new EmbeddedBookAuthor(bookId1, "Effective Java", "Programming Best Practices", List.of(Eauthor1, Eauthor2),
                List.of("Programming", "Tech"), "cover1.jpg");
        EmbeddedBookAuthor Ebook2 = new EmbeddedBookAuthor(bookId2, "Clean Code", "A handbook of agile software craftsmanship", List.of(Eauthor2),
                List.of("Software Engineering", "IoT"), "cover2.jpg");
        EmbeddedBookAuthor Ebook3 = new EmbeddedBookAuthor(bookId3, "Refactoring", "Improving the Design of Existing Code", List.of(Eauthor3),
                List.of("Software Engineering", "Refactoring"), "cover3.jpg");


        Author author1 = new Author(authorId1, "Joshua Bloch", "10-28-1961", "28-12-2020", "https://example.com/joshua-bloch.jpg",
                "American", List.of(Ebook1));
        Author author2 = new Author(authorId2, "Robert C. Martin", "10-28-1952", "28-12-2020", "https://example.com/robert-c-martin.jpg",
                "American", List.of(Ebook1, Ebook2));
        Author author3 = new Author(authorId3, "Martin Fowler", "10-28-1963", "28-12-2020", "https://example.com/martin-fowler.jpg",
                "British", List.of(Ebook3));


        Map<String, Object> location1 = new HashMap<>();
        location1.put("type", "Point");
        location1.put("coordinates", new double[]{80.152, 13.205});

        Map<String, Object> location2 = new HashMap<>();
        location2.put("type", "Point");
        location2.put("coordinates", new double[]{77.594, 12.971});


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


        EmbeddedBookSaved bookSaved1 = new EmbeddedBookSaved(bookId1, "Effective Java", List.of(Eauthor1, Eauthor2));
        EmbeddedBookSaved bookSaved2 = new EmbeddedBookSaved(bookId2, "Clean Code", List.of(Eauthor2));

        EmbeddedBookRead bookRead1 = new EmbeddedBookRead(bookId1, "Effective Java", List.of(author1, author2), libraryId1, LocalDateTime.parse("2024-12-23T15:30:00"));
        EmbeddedBookRead bookRead2 = new EmbeddedBookRead(bookId2, "Clean Code", List.of(author2), libraryId2, LocalDateTime.parse("2024-11-28T15:30:00"));
        EmbeddedBookRead bookRead3 = new EmbeddedBookRead(bookId3, "Refactoring", List.of(author3), libraryId1, LocalDateTime.parse("2024-10-10T15:30:00"));


        User user = new User(userId, "Testuser", "Test", "User", LocalDate.parse("1990-01-01"), "password", UserType.USER, "testmail@example.com",
                address1, List.of(bookRead1, bookRead2, bookRead3), 12.36, List.of(bookSaved1, bookSaved2));


        userRepository.save(user);

        redisBookRepository.addLibraryToBookAvailability(book1.getId().toHexString(), library1.getId().toHexString(),10);
        redisBookRepository.addLibraryToBookAvailability(book2.getId().toHexString(), library2.getId().toHexString(),8);
        redisBookRepository.addLibraryToBookAvailability(book3.getId().toHexString(), library1.getId().toHexString(),5);

        reservationRepository.reserveBook(userId.toHexString(), book1.getId().toHexString(), library1.getId().toHexString(), book1.getTitle(), library1.getLibraryName());
        reservationRepository.reserveBook(userId.toHexString(), book2.getId().toHexString(), library2.getId().toHexString(), book2.getTitle(), library2.getLibraryName());

        loanRepository.markAsLoan(library1.getId().toHexString(), userId.toHexString(), book1.getId().toHexString());

    }


    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();

        reservationRepository.cancelReservation(userId.toHexString(), book2.getId().toHexString(), library2.getId().toHexString());
        loanRepository.completeLoan(library1.getId().toHexString(), userId.toHexString(), book1.getId().toHexString());

        redisBookRepository.removeLibraryEntry(book1.getId().toHexString(), library1.getId().toHexString(),10);
        redisBookRepository.removeLibraryEntry(book2.getId().toHexString(), library2.getId().toHexString(),8);
        redisBookRepository.removeLibraryEntry(book3.getId().toHexString(), library1.getId().toHexString(),5);
    }



    @AfterAll
    static void tearDown() {
        mongoDBContainer.stop();
        redisContainer.stop();

        mongoDBContainer.close();
        redisContainer.close();
    }




    @Test
    void testGetReservedAndLoanedBooks() throws Exception {

        mockMvc.perform(get("/api/v1/users/reserved-loaned")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(jwt -> jwt.claim("user_id", userId.toHexString()))))
                .andExpect(status().isOk())
                .andDo(print());
    }




    @Test
    void testGetSavedBooks() throws Exception {
        mockMvc.perform(get("/api/v1/users/saved")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(jwt -> jwt.claim("user_id", userId.toHexString()))))
                .andExpect(status().isOk())
                .andDo(print());
    }




    @Test
    void testGetReadBooks() throws Exception {
        mockMvc.perform(get("/api/v1/users/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(jwt -> jwt.claim("user_id", userId.toHexString()))))
                .andExpect(status().isOk())
                .andDo(print());
    }




    @Test
    void testGetUserDetails() throws Exception {
        mockMvc.perform(get("/api/v1/users/details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(jwt -> jwt.claim("user_id", userId.toHexString()))))
                .andExpect(status().isOk())
                .andDo(print());
    }
}
