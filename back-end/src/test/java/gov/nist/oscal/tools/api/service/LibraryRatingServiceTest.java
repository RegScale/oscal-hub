package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryItemRating;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.RatingResponse;
import gov.nist.oscal.tools.api.repository.LibraryItemRatingRepository;
import gov.nist.oscal.tools.api.repository.LibraryItemRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LibraryRatingService
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LibraryRatingServiceTest {

    @Mock
    private LibraryItemRatingRepository ratingRepository;

    @Mock
    private LibraryItemRepository libraryItemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LibraryRatingService ratingService;

    private User testUser;
    private LibraryItem testItem;
    private LibraryItemRating testRating;
    private static final String TEST_ITEM_ID = "test-item-uuid-123";
    private static final String TEST_USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(TEST_USERNAME);

        // Create test library item
        testItem = new LibraryItem();
        testItem.setId(1L);
        testItem.setItemId(TEST_ITEM_ID);
        testItem.setTitle("Test Catalog");
        testItem.setOscalType("catalog");
        testItem.setCreatedBy(testUser);

        // Create test rating
        testRating = new LibraryItemRating(testItem, testUser, 4);
        testRating.setId(1L);
        testRating.setCreatedAt(LocalDateTime.now());
        testRating.setUpdatedAt(LocalDateTime.now());

        // Set up common mocks
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(libraryItemRepository.findByItemId(TEST_ITEM_ID)).thenReturn(Optional.of(testItem));
    }

    // ==================== rateItem Tests ====================

    @Test
    void testRateItem_newRating_createsRating() {
        // Arrange
        when(ratingRepository.findByItemIdAndUser(TEST_ITEM_ID, testUser)).thenReturn(Optional.empty());
        when(ratingRepository.save(any(LibraryItemRating.class))).thenAnswer(invocation -> {
            LibraryItemRating rating = invocation.getArgument(0);
            rating.setId(1L);
            return rating;
        });
        when(ratingRepository.getAverageRatingByItemId(TEST_ITEM_ID)).thenReturn(4.0);
        when(ratingRepository.countByItemId(TEST_ITEM_ID)).thenReturn(1L);

        // Act
        RatingResponse response = ratingService.rateItem(TEST_ITEM_ID, 4, TEST_USERNAME);

        // Assert
        assertNotNull(response);
        assertEquals(4.0, response.getAverageRating());
        assertEquals(1L, response.getTotalRatings());
        assertEquals(4, response.getUserRating());

        // Verify new rating was saved
        ArgumentCaptor<LibraryItemRating> ratingCaptor = ArgumentCaptor.forClass(LibraryItemRating.class);
        verify(ratingRepository).save(ratingCaptor.capture());
        LibraryItemRating savedRating = ratingCaptor.getValue();
        assertEquals(4, savedRating.getRating());
        assertEquals(testItem, savedRating.getLibraryItem());
        assertEquals(testUser, savedRating.getUser());
    }

    @Test
    void testRateItem_existingRating_updatesRating() {
        // Arrange
        when(ratingRepository.findByItemIdAndUser(TEST_ITEM_ID, testUser)).thenReturn(Optional.of(testRating));
        when(ratingRepository.save(any(LibraryItemRating.class))).thenReturn(testRating);
        when(ratingRepository.getAverageRatingByItemId(TEST_ITEM_ID)).thenReturn(5.0);
        when(ratingRepository.countByItemId(TEST_ITEM_ID)).thenReturn(1L);

        // Act
        RatingResponse response = ratingService.rateItem(TEST_ITEM_ID, 5, TEST_USERNAME);

        // Assert
        assertNotNull(response);
        assertEquals(5.0, response.getAverageRating());

        // Verify existing rating was updated
        ArgumentCaptor<LibraryItemRating> ratingCaptor = ArgumentCaptor.forClass(LibraryItemRating.class);
        verify(ratingRepository).save(ratingCaptor.capture());
        LibraryItemRating savedRating = ratingCaptor.getValue();
        assertEquals(5, savedRating.getRating());
    }

    @Test
    void testRateItem_allRatingValues_acceptsValidRatings() {
        // Test all valid rating values (1-5)
        for (int rating = 1; rating <= 5; rating++) {
            // Arrange
            when(ratingRepository.findByItemIdAndUser(TEST_ITEM_ID, testUser)).thenReturn(Optional.empty());
            when(ratingRepository.save(any(LibraryItemRating.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(ratingRepository.getAverageRatingByItemId(TEST_ITEM_ID)).thenReturn((double) rating);
            when(ratingRepository.countByItemId(TEST_ITEM_ID)).thenReturn(1L);

            // Act
            RatingResponse response = ratingService.rateItem(TEST_ITEM_ID, rating, TEST_USERNAME);

            // Assert
            assertNotNull(response);
            assertEquals((double) rating, response.getAverageRating());
        }
    }

    @Test
    void testRateItem_itemNotFound_throwsException() {
        // Arrange
        when(libraryItemRepository.findByItemId("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            ratingService.rateItem("nonexistent", 4, TEST_USERNAME));
        assertTrue(exception.getMessage().contains("Library item not found"));
    }

    @Test
    void testRateItem_userNotFound_throwsException() {
        // Arrange
        when(userRepository.findByUsername("unknownuser")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            ratingService.rateItem(TEST_ITEM_ID, 4, "unknownuser"));
        assertTrue(exception.getMessage().contains("User not found"));
    }

    // ==================== getRatingStats Tests ====================

    @Test
    void testGetRatingStats_withUserRating_returnsUserRating() {
        // Arrange
        when(ratingRepository.getAverageRatingByItemId(TEST_ITEM_ID)).thenReturn(4.5);
        when(ratingRepository.countByItemId(TEST_ITEM_ID)).thenReturn(10L);
        when(ratingRepository.findByItemIdAndUser(TEST_ITEM_ID, testUser)).thenReturn(Optional.of(testRating));

        // Act
        RatingResponse response = ratingService.getRatingStats(TEST_ITEM_ID, TEST_USERNAME);

        // Assert
        assertNotNull(response);
        assertEquals(4.5, response.getAverageRating());
        assertEquals(10L, response.getTotalRatings());
        assertEquals(4, response.getUserRating());
    }

    @Test
    void testGetRatingStats_withoutUserRating_returnsNullUserRating() {
        // Arrange
        when(ratingRepository.getAverageRatingByItemId(TEST_ITEM_ID)).thenReturn(3.5);
        when(ratingRepository.countByItemId(TEST_ITEM_ID)).thenReturn(5L);
        when(ratingRepository.findByItemIdAndUser(TEST_ITEM_ID, testUser)).thenReturn(Optional.empty());

        // Act
        RatingResponse response = ratingService.getRatingStats(TEST_ITEM_ID, TEST_USERNAME);

        // Assert
        assertNotNull(response);
        assertEquals(3.5, response.getAverageRating());
        assertEquals(5L, response.getTotalRatings());
        assertNull(response.getUserRating());
    }

    @Test
    void testGetRatingStats_noRatings_returnsZeros() {
        // Arrange
        when(ratingRepository.getAverageRatingByItemId(TEST_ITEM_ID)).thenReturn(null);
        when(ratingRepository.countByItemId(TEST_ITEM_ID)).thenReturn(null);

        // Act
        RatingResponse response = ratingService.getRatingStats(TEST_ITEM_ID, null);

        // Assert
        assertNotNull(response);
        assertEquals(0.0, response.getAverageRating());
        assertEquals(0L, response.getTotalRatings());
        assertNull(response.getUserRating());
    }

    @Test
    void testGetRatingStats_anonymousUser_returnsStatsWithoutUserRating() {
        // Arrange
        when(ratingRepository.getAverageRatingByItemId(TEST_ITEM_ID)).thenReturn(4.2);
        when(ratingRepository.countByItemId(TEST_ITEM_ID)).thenReturn(15L);

        // Act
        RatingResponse response = ratingService.getRatingStats(TEST_ITEM_ID, null);

        // Assert
        assertNotNull(response);
        assertEquals(4.2, response.getAverageRating());
        assertEquals(15L, response.getTotalRatings());
        assertNull(response.getUserRating());
    }

    // ==================== deleteRating Tests ====================

    @Test
    void testDeleteRating_existingRating_deletesSuccessfully() {
        // Arrange
        when(ratingRepository.findByItemIdAndUser(TEST_ITEM_ID, testUser)).thenReturn(Optional.of(testRating));

        // Act
        ratingService.deleteRating(TEST_ITEM_ID, TEST_USERNAME);

        // Assert
        verify(ratingRepository).delete(testRating);
    }

    @Test
    void testDeleteRating_noExistingRating_doesNotThrow() {
        // Arrange
        when(ratingRepository.findByItemIdAndUser(TEST_ITEM_ID, testUser)).thenReturn(Optional.empty());

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> ratingService.deleteRating(TEST_ITEM_ID, TEST_USERNAME));
        verify(ratingRepository, never()).delete(any());
    }

    @Test
    void testDeleteRating_userNotFound_throwsException() {
        // Arrange
        when(userRepository.findByUsername("unknownuser")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            ratingService.deleteRating(TEST_ITEM_ID, "unknownuser"));
        assertTrue(exception.getMessage().contains("User not found"));
    }

    // ==================== getBatchRatingStats Tests ====================

    @Test
    void testGetBatchRatingStats_multipleItems_returnsBatchStats() {
        // Arrange
        List<String> itemIds = Arrays.asList("item1", "item2", "item3");
        List<Object[]> stats = Arrays.asList(
            new Object[]{"item1", 4.5, 10L},
            new Object[]{"item2", 3.8, 5L}
            // item3 has no ratings
        );
        when(ratingRepository.getRatingStatsByItemIds(itemIds)).thenReturn(stats);

        // Act
        Map<String, RatingResponse> result = ratingService.getBatchRatingStats(itemIds);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());

        // Item1 - has ratings
        assertEquals(4.5, result.get("item1").getAverageRating());
        assertEquals(10L, result.get("item1").getTotalRatings());

        // Item2 - has ratings
        assertEquals(3.8, result.get("item2").getAverageRating());
        assertEquals(5L, result.get("item2").getTotalRatings());

        // Item3 - no ratings (default values)
        assertEquals(0.0, result.get("item3").getAverageRating());
        assertEquals(0L, result.get("item3").getTotalRatings());
    }

    @Test
    void testGetBatchRatingStats_emptyList_returnsEmptyMap() {
        // Arrange
        List<String> itemIds = Collections.emptyList();

        // Act
        Map<String, RatingResponse> result = ratingService.getBatchRatingStats(itemIds);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(ratingRepository, never()).getRatingStatsByItemIds(any());
    }

    @Test
    void testGetBatchRatingStats_nullList_returnsEmptyMap() {
        // Act
        Map<String, RatingResponse> result = ratingService.getBatchRatingStats(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetBatchRatingStats_singleItem_returnsSingleStat() {
        // Arrange
        List<String> itemIds = Collections.singletonList("item1");
        List<Object[]> stats = Collections.singletonList(new Object[]{"item1", 5.0, 20L});
        when(ratingRepository.getRatingStatsByItemIds(itemIds)).thenReturn(stats);

        // Act
        Map<String, RatingResponse> result = ratingService.getBatchRatingStats(itemIds);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5.0, result.get("item1").getAverageRating());
        assertEquals(20L, result.get("item1").getTotalRatings());
    }
}
