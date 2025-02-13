package it.unipi.distribooked.service;

import it.unipi.distribooked.dto.BookUtilizationDTO;
import it.unipi.distribooked.dto.BooksByAgeGroupDTO;
import it.unipi.distribooked.repository.mongo.BookRepository;
import it.unipi.distribooked.repository.mongo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    public List<BooksByAgeGroupDTO> getMostReadBooksByAgeGroup(String startDate, String endDate) {
        return userRepository.findMostReadBooksByAgeGroup(startDate, endDate);
    }

    /**
     * Retrieves the average age of active readers (those who borrowed at least one book in the last 365 days),
     * grouped by city.
     *
     * @return A map where the key is the city and the value is the average age of readers in that city.
     */
    public Map<String, Map<String, Object>> getAverageAgeOfReadersByCity() {
        return userRepository.findAverageAgeOfReadersByCity();
    }

    public Map<String, List<BookUtilizationDTO>> getBooksUtilization() {
        return bookRepository.findBooksUtilization();
    }
}
