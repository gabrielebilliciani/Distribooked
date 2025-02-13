package it.unipi.distribooked.mapper;

import it.unipi.distribooked.dto.BookDTO;
import it.unipi.distribooked.model.Book;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BookMapperTest {

//    @Autowired
//    private BookMapper bookMapper;
//
//    @Test
//    public void testMapping() {
//        // Arrange
//        Book book = new Book();
//        book.setId(new ObjectId("64b87f1a2d3b9c1234567890"));
//        book.setTitle("Sample Book");
//
//        // Act
//        BookDTO dto = bookMapper.toBookDTO(book);
//
//        // Assert
//        assertNotNull(dto);
//        assertEquals("64b87f1a2d3b9c1234567890", dto.getId());
//        assertEquals("Sample Book", dto.getTitle());
//    }
}
