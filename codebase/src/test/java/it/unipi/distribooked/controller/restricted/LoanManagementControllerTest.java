package it.unipi.distribooked.controller.restricted;

import it.unipi.distribooked.model.Author;
import it.unipi.distribooked.model.Book;
import it.unipi.distribooked.model.User;
import it.unipi.distribooked.model.embedded.*;
import it.unipi.distribooked.model.enums.UserType;
import it.unipi.distribooked.repository.mongo.UserRepository;
import it.unipi.distribooked.repository.redis.*;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc

class LoanManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisBookRepository redisBookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private OverdueLoanRepository overdueLoanRepository;





    @Container
    @ServiceConnection(name = "mongo")
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");


    @Container
    @ServiceConnection(name = "redis")
    private static final GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:7.4.2")).withExposedPorts(6379);




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




    private ObjectId userId1, userId2, userId3, bookId1, bookId2, bookId3, bookId4, bookId5, bookId6, libraryId1, libraryId2;
    private Book book1, book2, book3, book4, book5, book6;
    private EmbeddedLibrary library1, library2;
    private User user1, user2;


    @BeforeAll
    static void setup() {
        System.setProperty("spring.profiles.active", "test");
    }

    @BeforeEach
    void setUp() {

        userId1 = new ObjectId("54b87f1a2d3b9c1234567890");
        userId2 = new ObjectId("54b87f1a2d3b9c1234567891");
        userId3 = new ObjectId("54b87f1a2d3b9c1234567892");

        bookId1 = new ObjectId("54b87f1a2d3b9c1234567890");
        bookId2 = new ObjectId("54b87f1a2d3b9c1234567891");
        bookId3 = new ObjectId("54b87f1a2d3b9c1234567892");
        bookId4 = new ObjectId("54b87f1a2d3b9c1234567893");
        bookId5 = new ObjectId("54b87f1a2d3b9c1234567894");
        bookId6 = new ObjectId("54b87f1a2d3b9c1234567895");


        libraryId1 = new ObjectId("74b87f1a2d3b9c1234567890");
        libraryId2 = new ObjectId("74b87f1a2d3b9c1234567891");


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


        List<EmbeddedAuthor> authors1 = List.of(Eauthor1, Eauthor2);
        List<EmbeddedAuthor> authors2 = List.of(Eauthor4, Eauthor5);
        List<EmbeddedAuthor> authors3 = List.of(Eauthor6, Eauthor3);
        List<EmbeddedAuthor> authors4 = List.of(Eauthor2, Eauthor5);
        List<EmbeddedAuthor> authors5 = List.of(Eauthor7, Eauthor6);
        List<EmbeddedAuthor> authors6 = List.of(Eauthor4, Eauthor7);


        EmbeddedBookSaved bookSaved1 = new EmbeddedBookSaved(bookId1, "Effective Java", List.of(Eauthor1, Eauthor2));
        EmbeddedBookSaved bookSaved2 = new EmbeddedBookSaved(bookId2, "Clean Code", List.of(Eauthor2));
        EmbeddedBookSaved bookSaved3 = new EmbeddedBookSaved(bookId3, "Refactoring", List.of(Eauthor3));


        EmbeddedBookRead bookRead1 = new EmbeddedBookRead(bookId1, "Effective Java", List.of(author1, author2), libraryId1, LocalDateTime.parse("2024-12-23T15:30:00"));
        EmbeddedBookRead bookRead2 = new EmbeddedBookRead(bookId2, "Clean Code", List.of(author2), libraryId2, LocalDateTime.parse("2024-11-28T15:30:00"));
        EmbeddedBookRead bookRead3 = new EmbeddedBookRead(bookId3, "Refactoring", List.of(author3), libraryId1, LocalDateTime.parse("2024-10-10T15:30:00"));


        Map<String, Object> location1 = new HashMap<>();
        location1.put("type", "Point");
        location1.put("coordinates", new double[]{80.152, 13.205});

        Map<String, Object> location2 = new HashMap<>();
        location2.put("type", "Point");
        location2.put("coordinates", new double[]{77.594, 12.971});


        Address address1 = new Address("Via Roma, 10", "Firenze", "FI", "50123", "Italy");
        Address address2 = new Address("Corso Vittorio Emanuele, 45", "Milano", "MI", "20121", "Italy");
        Address address3 = new Address("Via Garibaldi, 1", "Rome", "RM", "00100", "Italy");


        library1 = new EmbeddedLibrary(libraryId1, "Central Library", Map.of("Position", location1), address1, 4);
        library2 = new EmbeddedLibrary(libraryId2, "Tech Library", Map.of("Position", location2), address2, 3);


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


        user1 = new User(userId1, "johndoe", "John", "Doe", LocalDate.parse("1985-05-12"), "Johndoe01", UserType.USER,"johndoe@example.com",  address3, List.of(bookRead1, bookRead2),
                24.32, List.of(bookSaved3));
        user2 = new User(userId2, "janedoe", "Jane", "Doe", LocalDate.parse("1985-05-12"), "Janedoe01", UserType.USER,"janedoe@example.com", address2, List.of(bookRead3),
                10.32, List.of(bookSaved1, bookSaved2));



        userRepository.saveAll(List.of(user1, user2));

        redisBookRepository.addLibraryToBookAvailability(book1.getId().toHexString(), library1.getId().toString(), 10);
        redisBookRepository.addLibraryToBookAvailability(book1.getId().toHexString(), library2.getId().toString(), 2);
        redisBookRepository.addLibraryToBookAvailability(book2.getId().toHexString(), library1.getId().toString(), 15);
        redisBookRepository.addLibraryToBookAvailability(book2.getId().toHexString(), library2.getId().toString(), 8);
        redisBookRepository.addLibraryToBookAvailability(book3.getId().toHexString(), library1.getId().toString(), 25);
        redisBookRepository.addLibraryToBookAvailability(book3.getId().toHexString(), library2.getId().toString(), 4);
        redisBookRepository.addLibraryToBookAvailability(book4.getId().toHexString(), library1.getId().toString(), 36);
        redisBookRepository.addLibraryToBookAvailability(book4.getId().toHexString(), library2.getId().toString(), 16);
        redisBookRepository.addLibraryToBookAvailability(book5.getId().toHexString(), library1.getId().toString(), 8);
        redisBookRepository.addLibraryToBookAvailability(book5.getId().toHexString(), library2.getId().toString(), 3);
        redisBookRepository.addLibraryToBookAvailability(book6.getId().toHexString(), library1.getId().toString(), 17);
        redisBookRepository.addLibraryToBookAvailability(book6.getId().toHexString(), library2.getId().toString(), 13);


    }

    @AfterEach
    void tearDown() {

        userRepository.deleteAll();

        redisBookRepository.removeLibraryEntry(book1.getId().toHexString(), library1.getId().toString(), 10);
        redisBookRepository.removeLibraryEntry(book1.getId().toHexString(), library2.getId().toString(), 2);
        redisBookRepository.removeLibraryEntry(book2.getId().toHexString(), library1.getId().toString(), 15);
        redisBookRepository.removeLibraryEntry(book2.getId().toHexString(), library2.getId().toString(), 8);
        redisBookRepository.removeLibraryEntry(book3.getId().toHexString(), library1.getId().toString(), 25);
        redisBookRepository.removeLibraryEntry(book3.getId().toHexString(), library2.getId().toString(), 4);
        redisBookRepository.removeLibraryEntry(book4.getId().toHexString(), library1.getId().toString(), 36);
        redisBookRepository.removeLibraryEntry(book4.getId().toHexString(), library2.getId().toString(), 16);
        redisBookRepository.removeLibraryEntry(book5.getId().toHexString(), library1.getId().toString(), 8);
        redisBookRepository.removeLibraryEntry(book5.getId().toHexString(), library2.getId().toString(), 3);
        redisBookRepository.removeLibraryEntry(book6.getId().toHexString(), library1.getId().toString(), 17);
        redisBookRepository.removeLibraryEntry(book6.getId().toHexString(), library2.getId().toString(), 13);

    }

    @AfterAll
    static void tearDownAll() {
        mongoDBContainer.stop();
        redisContainer.stop();

        mongoDBContainer.close();
        redisContainer.close();
    }





    @Test
    void testMarkAsLoan() throws Exception {

        reservationRepository.reserveBook(userId1.toString(), book1.getId().toHexString(), book1.getBranches().get(0).getId().toHexString(), book1.getTitle(), "Central Library");

        String libraryId = book1.getBranches().get(0).getId().toString();
        String userId = userId1.toHexString();
        String bookId = book1.getId().toHexString();

        mockMvc.perform(post("/api/v1/admin/loans/" + libraryId + "/" + userId + "/" + bookId + "/mark-as-loan")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());


        loanRepository.completeLoan(book1.getBranches().get(0).getId().toHexString(), userId1.toString(), book1.getId().toHexString());
    }






    @Test
    void testCompleteLoan() throws Exception {

        reservationRepository.reserveBook(userId2.toString(), book2.getId().toHexString(), book2.getBranches().get(1).getId().toHexString(), book2.getTitle(), "Tech Library");
        loanRepository.markAsLoan(book2.getBranches().get(1).getId().toHexString(), userId2.toString(), book2.getId().toHexString());


        String libraryId = book2.getBranches().get(1).getId().toString();
        String userId = userId2.toHexString();
        String bookId = book2.getId().toHexString();

        mockMvc.perform(post("/api/v1/admin/loans/" + libraryId + "/" + userId + "/" + bookId + "/complete")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }







    @Test
    void testGetOverdueLoans() throws Exception {

        reservationRepository.reserveBook(userId1.toString(), book4.getId().toHexString(), book4.getBranches().get(0).getId().toHexString(),book4.getTitle(), "Central Library");
        reservationRepository.reserveBook(userId2.toString(), book5.getId().toHexString(), book5.getBranches().get(0).getId().toHexString(),book5.getTitle(), "Central Library");
        reservationRepository.reserveBook(userId3.toString(), book6.getId().toHexString(), book6.getBranches().get(0).getId().toHexString(),book6.getTitle(), "Central Library");

        loanRepository.markAsLoan(book4.getBranches().get(0).getId().toHexString(), userId1.toString(), book4.getId().toHexString());
        loanRepository.markAsLoan(book5.getBranches().get(0).getId().toHexString(), userId2.toString(), book5.getId().toHexString());
        loanRepository.markAsLoan(book6.getBranches().get(0).getId().toHexString(), userId3.toString(), book6.getId().toHexString());

        overdueLoanRepository.markLoanAsOverdue(userId1.toHexString(), book4.getId().toHexString(), book4.getBranches().get(0).getId().toHexString());
        overdueLoanRepository.markLoanAsOverdue(userId2.toHexString(), book5.getId().toHexString(), book5.getBranches().get(0).getId().toHexString());
        overdueLoanRepository.markLoanAsOverdue(userId3.toHexString(), book6.getId().toHexString(), book6.getBranches().get(0).getId().toHexString());


        String libraryId = libraryId1.toHexString();

        mockMvc.perform(get("/api/v1/admin/loans/" + libraryId + "/overdue")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());


        loanRepository.completeLoan(book4.getBranches().get(0).getId().toHexString(), userId1.toString(), book4.getId().toHexString());
        loanRepository.completeLoan(book5.getBranches().get(0).getId().toHexString(), userId2.toString(), book5.getId().toHexString());
        loanRepository.completeLoan(book6.getBranches().get(0).getId().toHexString(), userId3.toString(), book6.getId().toHexString());
    }







    @Test
    void testGetAllLoans() throws Exception {

        reservationRepository.reserveBook(userId1.toString(), book1.getId().toHexString(), book1.getBranches().get(0).getId().toHexString(),"Effective Java", "Central Library");
        reservationRepository.reserveBook(userId2.toString(), book2.getId().toHexString(), book2.getBranches().get(0).getId().toHexString(),book2.getTitle(), "Central Library");
        reservationRepository.reserveBook(userId2.toString(), book4.getId().toHexString(), book4.getBranches().get(0).getId().toHexString(),book4.getTitle(), "Central Library");
        reservationRepository.reserveBook(userId3.toString(), book6.getId().toHexString(), book6.getBranches().get(0).getId().toHexString(),book6.getTitle(), "Central Library");

        loanRepository.markAsLoan(book1.getBranches().get(0).getId().toHexString(), userId1.toString(), book1.getId().toHexString());
        loanRepository.markAsLoan(book2.getBranches().get(0).getId().toHexString(), userId2.toString(), book2.getId().toHexString());
        loanRepository.markAsLoan(book4.getBranches().get(0).getId().toHexString(), userId2.toString(), book4.getId().toHexString());
        loanRepository.markAsLoan(book6.getBranches().get(0).getId().toHexString(), userId3.toString(), book6.getId().toHexString());

        String libraryId = libraryId1.toHexString();

        mockMvc.perform(get("/api/v1/admin/loans/" + libraryId + "/loans")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());



        loanRepository.completeLoan(book1.getBranches().get(0).getId().toHexString(), userId1.toString(), book1.getId().toHexString());
        loanRepository.completeLoan(book2.getBranches().get(0).getId().toHexString(), userId2.toString(), book2.getId().toHexString());
        loanRepository.completeLoan(book4.getBranches().get(0).getId().toHexString(), userId2.toString(), book4.getId().toHexString());
        loanRepository.completeLoan(book6.getBranches().get(0).getId().toHexString(), userId3.toString(), book6.getId().toHexString());
    }





    @Test
    void testGetAllReservations() throws Exception {

        reservationRepository.reserveBook(userId1.toString(), book1.getId().toHexString(), book1.getBranches().get(0).getId().toHexString(),book1.getTitle(), "Central Library");
        reservationRepository.reserveBook(userId2.toString(), book2.getId().toHexString(), book2.getBranches().get(0).getId().toHexString(),book2.getTitle(), "Central Library");
        reservationRepository.reserveBook(userId2.toString(), book4.getId().toHexString(), book4.getBranches().get(0).getId().toHexString(),book4.getTitle(), "Central Library");
        reservationRepository.reserveBook(userId3.toString(), book6.getId().toHexString(), book6.getBranches().get(0).getId().toHexString(),book6.getTitle(), "Central Library");

        String libraryId = libraryId1.toHexString();

        mockMvc.perform(get("/api/v1/admin/loans/"+ libraryId +"/reservations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());



        reservationRepository.cancelReservation(userId1.toString(), book1.getId().toHexString(), book1.getBranches().get(0).getId().toHexString());
        reservationRepository.cancelReservation(userId2.toString(), book2.getId().toHexString(), book2.getBranches().get(0).getId().toHexString());
        reservationRepository.cancelReservation(userId2.toString(), book4.getId().toHexString(), book4.getBranches().get(0).getId().toHexString());
        reservationRepository.cancelReservation(userId3.toString(), book6.getId().toHexString(), book6.getBranches().get(0).getId().toHexString());
    }






    @Test
    void testGetUserReservedAndLoanedBooks() throws Exception {

        reservationRepository.reserveBook(userId1.toString(), book1.getId().toHexString(), book1.getBranches().get(0).getId().toHexString(),book1.getTitle(), "Central Library");
        reservationRepository.reserveBook(userId1.toString(), book2.getId().toHexString(), book2.getBranches().get(1).getId().toHexString(),book2.getTitle(), "Tech Library");
        reservationRepository.reserveBook(userId1.toString(), book3.getId().toHexString(), book3.getBranches().get(0).getId().toHexString(),book3.getTitle(), "Central Library");
        reservationRepository.reserveBook(userId1.toString(), book4.getId().toHexString(), book4.getBranches().get(1).getId().toHexString(),book4.getTitle(), "Tech Library");

        loanRepository.markAsLoan(book1.getBranches().get(0).getId().toHexString(), userId1.toString(), book1.getId().toHexString());
        loanRepository.markAsLoan(book2.getBranches().get(1).getId().toHexString(), userId1.toString(), book2.getId().toHexString());



        String uId = userId1.toHexString();

        mockMvc.perform(get("/api/v1/admin/loans/" + uId + "/reserved-loaned")
                        .param("userId", uId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());


        loanRepository.completeLoan(book1.getBranches().get(0).getId().toHexString(), userId1.toString(), book1.getId().toHexString());
        loanRepository.completeLoan(book2.getBranches().get(1).getId().toHexString(), userId1.toString(), book2.getId().toHexString());
        reservationRepository.cancelReservation(userId1.toString(), book3.getId().toHexString(), book3.getBranches().get(0).getId().toHexString());
        reservationRepository.cancelReservation(userId1.toString(), book4.getId().toHexString(), book4.getBranches().get(1).getId().toHexString());
    }





    @Test
    void testGetUserDetails() throws Exception {

        String uId = user1.getId().toHexString();

        mockMvc.perform(get("/api/v1/admin/loans/" + uId + "/details")
                        .param("userId", uId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());

    }
}
