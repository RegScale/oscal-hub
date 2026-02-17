package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryItemComment;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.CommentResponse;
import gov.nist.oscal.tools.api.repository.LibraryItemCommentRepository;
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
 * Unit tests for LibraryCommentService
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LibraryCommentServiceTest {

    @Mock
    private LibraryItemCommentRepository commentRepository;

    @Mock
    private LibraryItemRepository libraryItemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LibraryCommentService commentService;

    private User testUser;
    private User otherUser;
    private LibraryItem testItem;
    private LibraryItemComment testComment;
    private LibraryItemComment testReply;
    private static final String TEST_ITEM_ID = "test-item-uuid-123";
    private static final String TEST_COMMENT_ID = "test-comment-uuid-456";
    private static final String TEST_REPLY_ID = "test-reply-uuid-789";
    private static final String TEST_USERNAME = "testuser";
    private static final String OTHER_USERNAME = "otheruser";

    @BeforeEach
    void setUp() {
        // Create test users
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(TEST_USERNAME);
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername(OTHER_USERNAME);

        // Create test library item
        testItem = new LibraryItem();
        testItem.setId(1L);
        testItem.setItemId(TEST_ITEM_ID);
        testItem.setTitle("Test Catalog");
        testItem.setOscalType("catalog");
        testItem.setCreatedBy(testUser);

        // Create test comment
        testComment = new LibraryItemComment(TEST_COMMENT_ID, testItem, testUser, "This is a test comment");
        testComment.setId(1L);
        testComment.setCreatedAt(LocalDateTime.now().minusHours(1));
        testComment.setUpdatedAt(LocalDateTime.now().minusHours(1));
        testComment.setDeleted(false);

        // Create test reply
        testReply = new LibraryItemComment(TEST_REPLY_ID, testItem, otherUser, "This is a reply", testComment);
        testReply.setId(2L);
        testReply.setCreatedAt(LocalDateTime.now());
        testReply.setUpdatedAt(LocalDateTime.now());
        testReply.setDeleted(false);

        // Set up common mocks
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername(OTHER_USERNAME)).thenReturn(Optional.of(otherUser));
        when(libraryItemRepository.findByItemId(TEST_ITEM_ID)).thenReturn(Optional.of(testItem));
    }

    // ==================== createComment Tests ====================

    @Test
    void testCreateComment_topLevelComment_createsSuccessfully() {
        // Arrange
        String content = "New comment content";
        when(commentRepository.save(any(LibraryItemComment.class))).thenAnswer(invocation -> {
            LibraryItemComment comment = invocation.getArgument(0);
            comment.setId(10L);
            return comment;
        });

        // Act
        CommentResponse response = commentService.createComment(TEST_ITEM_ID, content, null, TEST_USERNAME);

        // Assert
        assertNotNull(response);
        assertEquals(content, response.getContent());
        assertEquals(TEST_USERNAME, response.getUsername());
        assertEquals("Test User", response.getUserDisplayName());
        assertNull(response.getParentCommentId());

        // Verify save was called with correct data
        ArgumentCaptor<LibraryItemComment> commentCaptor = ArgumentCaptor.forClass(LibraryItemComment.class);
        verify(commentRepository).save(commentCaptor.capture());
        LibraryItemComment savedComment = commentCaptor.getValue();
        assertEquals(content, savedComment.getContent());
        assertEquals(testItem, savedComment.getLibraryItem());
        assertEquals(testUser, savedComment.getUser());
        assertNull(savedComment.getParentComment());
        assertNotNull(savedComment.getCommentId()); // UUID should be generated
    }

    @Test
    void testCreateComment_reply_createsSuccessfully() {
        // Arrange
        String replyContent = "This is a reply";
        when(commentRepository.findByCommentId(TEST_COMMENT_ID)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(LibraryItemComment.class))).thenAnswer(invocation -> {
            LibraryItemComment comment = invocation.getArgument(0);
            comment.setId(20L);
            return comment;
        });

        // Act
        CommentResponse response = commentService.createComment(TEST_ITEM_ID, replyContent, TEST_COMMENT_ID, OTHER_USERNAME);

        // Assert
        assertNotNull(response);
        assertEquals(replyContent, response.getContent());
        assertEquals(OTHER_USERNAME, response.getUsername());
        assertEquals(TEST_COMMENT_ID, response.getParentCommentId());

        // Verify save was called with correct parent
        ArgumentCaptor<LibraryItemComment> commentCaptor = ArgumentCaptor.forClass(LibraryItemComment.class);
        verify(commentRepository).save(commentCaptor.capture());
        LibraryItemComment savedReply = commentCaptor.getValue();
        assertEquals(testComment, savedReply.getParentComment());
    }

    @Test
    void testCreateComment_itemNotFound_throwsException() {
        // Arrange
        when(libraryItemRepository.findByItemId("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            commentService.createComment("nonexistent", "Content", null, TEST_USERNAME));
        assertTrue(exception.getMessage().contains("Library item not found"));
    }

    @Test
    void testCreateComment_userNotFound_throwsException() {
        // Arrange
        when(userRepository.findByUsername("unknownuser")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            commentService.createComment(TEST_ITEM_ID, "Content", null, "unknownuser"));
        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    void testCreateComment_parentCommentNotFound_throwsException() {
        // Arrange
        when(commentRepository.findByCommentId("nonexistent-parent")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            commentService.createComment(TEST_ITEM_ID, "Content", "nonexistent-parent", TEST_USERNAME));
        assertTrue(exception.getMessage().contains("Parent comment not found"));
    }

    @Test
    void testCreateComment_parentCommentFromDifferentItem_throwsException() {
        // Arrange
        LibraryItem otherItem = new LibraryItem();
        otherItem.setId(99L);
        otherItem.setItemId("other-item-id");

        LibraryItemComment parentFromOtherItem = new LibraryItemComment("other-comment", otherItem, testUser, "Other comment");
        when(commentRepository.findByCommentId("other-comment")).thenReturn(Optional.of(parentFromOtherItem));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            commentService.createComment(TEST_ITEM_ID, "Content", "other-comment", TEST_USERNAME));
        assertTrue(exception.getMessage().contains("does not belong to this library item"));
    }

    // ==================== getComments Tests ====================

    @Test
    void testGetComments_withThreadedComments_returnsNestedStructure() {
        // Arrange
        when(commentRepository.findTopLevelCommentsByItemId(TEST_ITEM_ID))
            .thenReturn(Collections.singletonList(testComment));
        when(commentRepository.findRepliesByParentComment(testComment))
            .thenReturn(Collections.singletonList(testReply));
        when(commentRepository.findRepliesByParentComment(testReply))
            .thenReturn(Collections.emptyList());

        // Act
        List<CommentResponse> result = commentService.getComments(TEST_ITEM_ID);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        CommentResponse topLevel = result.get(0);
        assertEquals(TEST_COMMENT_ID, topLevel.getCommentId());
        assertEquals(1, topLevel.getReplies().size());

        CommentResponse reply = topLevel.getReplies().get(0);
        assertEquals(TEST_REPLY_ID, reply.getCommentId());
        assertEquals(0, reply.getReplies().size());
    }

    @Test
    void testGetComments_noComments_returnsEmptyList() {
        // Arrange
        when(commentRepository.findTopLevelCommentsByItemId(TEST_ITEM_ID))
            .thenReturn(Collections.emptyList());

        // Act
        List<CommentResponse> result = commentService.getComments(TEST_ITEM_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetComments_multipleTopLevelComments_returnsAll() {
        // Arrange
        LibraryItemComment comment2 = new LibraryItemComment("comment-2", testItem, otherUser, "Second comment");
        comment2.setId(3L);
        comment2.setCreatedAt(LocalDateTime.now());
        comment2.setUpdatedAt(LocalDateTime.now());
        comment2.setDeleted(false);

        when(commentRepository.findTopLevelCommentsByItemId(TEST_ITEM_ID))
            .thenReturn(Arrays.asList(testComment, comment2));
        when(commentRepository.findRepliesByParentComment(any()))
            .thenReturn(Collections.emptyList());

        // Act
        List<CommentResponse> result = commentService.getComments(TEST_ITEM_ID);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // ==================== updateComment Tests ====================

    @Test
    void testUpdateComment_byOwner_updatesSuccessfully() {
        // Arrange
        String newContent = "Updated comment content";
        when(commentRepository.findByCommentId(TEST_COMMENT_ID)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(LibraryItemComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CommentResponse response = commentService.updateComment(TEST_COMMENT_ID, newContent, TEST_USERNAME);

        // Assert
        assertNotNull(response);
        assertEquals(newContent, response.getContent());

        // Verify content was updated
        ArgumentCaptor<LibraryItemComment> commentCaptor = ArgumentCaptor.forClass(LibraryItemComment.class);
        verify(commentRepository).save(commentCaptor.capture());
        assertEquals(newContent, commentCaptor.getValue().getContent());
    }

    @Test
    void testUpdateComment_byNonOwner_throwsException() {
        // Arrange
        when(commentRepository.findByCommentId(TEST_COMMENT_ID)).thenReturn(Optional.of(testComment));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            commentService.updateComment(TEST_COMMENT_ID, "New content", OTHER_USERNAME));
        assertTrue(exception.getMessage().contains("not authorized"));
    }

    @Test
    void testUpdateComment_commentNotFound_throwsException() {
        // Arrange
        when(commentRepository.findByCommentId("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            commentService.updateComment("nonexistent", "New content", TEST_USERNAME));
        assertTrue(exception.getMessage().contains("Comment not found"));
    }

    // ==================== deleteComment Tests ====================

    @Test
    void testDeleteComment_byOwner_softDeletesSuccessfully() {
        // Arrange
        when(commentRepository.findByCommentId(TEST_COMMENT_ID)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(LibraryItemComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        commentService.deleteComment(TEST_COMMENT_ID, TEST_USERNAME);

        // Assert - verify soft delete
        ArgumentCaptor<LibraryItemComment> commentCaptor = ArgumentCaptor.forClass(LibraryItemComment.class);
        verify(commentRepository).save(commentCaptor.capture());
        assertTrue(commentCaptor.getValue().getDeleted());
    }

    @Test
    void testDeleteComment_byNonOwner_throwsException() {
        // Arrange
        when(commentRepository.findByCommentId(TEST_COMMENT_ID)).thenReturn(Optional.of(testComment));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            commentService.deleteComment(TEST_COMMENT_ID, OTHER_USERNAME));
        assertTrue(exception.getMessage().contains("not authorized"));
    }

    @Test
    void testDeleteComment_commentNotFound_throwsException() {
        // Arrange
        when(commentRepository.findByCommentId("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            commentService.deleteComment("nonexistent", TEST_USERNAME));
        assertTrue(exception.getMessage().contains("Comment not found"));
    }

    // ==================== getCommentCount Tests ====================

    @Test
    void testGetCommentCount_withComments_returnsCount() {
        // Arrange
        when(commentRepository.countByItemId(TEST_ITEM_ID)).thenReturn(15L);

        // Act
        Long count = commentService.getCommentCount(TEST_ITEM_ID);

        // Assert
        assertEquals(15L, count);
    }

    @Test
    void testGetCommentCount_noComments_returnsZero() {
        // Arrange
        when(commentRepository.countByItemId(TEST_ITEM_ID)).thenReturn(null);

        // Act
        Long count = commentService.getCommentCount(TEST_ITEM_ID);

        // Assert
        assertEquals(0L, count);
    }

    // ==================== getBatchCommentCounts Tests ====================

    @Test
    void testGetBatchCommentCounts_multipleItems_returnsBatchCounts() {
        // Arrange
        List<String> itemIds = Arrays.asList("item1", "item2", "item3");
        List<Object[]> counts = Arrays.asList(
            new Object[]{"item1", 10L},
            new Object[]{"item2", 5L}
            // item3 has no comments
        );
        when(commentRepository.countByItemIds(itemIds)).thenReturn(counts);

        // Act
        Map<String, Long> result = commentService.getBatchCommentCounts(itemIds);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(10L, result.get("item1"));
        assertEquals(5L, result.get("item2"));
        assertEquals(0L, result.get("item3")); // Default for items with no comments
    }

    @Test
    void testGetBatchCommentCounts_emptyList_returnsEmptyMap() {
        // Arrange
        List<String> itemIds = Collections.emptyList();

        // Act
        Map<String, Long> result = commentService.getBatchCommentCounts(itemIds);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(commentRepository, never()).countByItemIds(any());
    }

    @Test
    void testGetBatchCommentCounts_nullList_returnsEmptyMap() {
        // Act
        Map<String, Long> result = commentService.getBatchCommentCounts(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Edge Cases ====================

    @Test
    void testCreateComment_emptyParentCommentId_treatedAsTopLevel() {
        // Arrange
        when(commentRepository.save(any(LibraryItemComment.class))).thenAnswer(invocation -> {
            LibraryItemComment comment = invocation.getArgument(0);
            comment.setId(10L);
            return comment;
        });

        // Act
        CommentResponse response = commentService.createComment(TEST_ITEM_ID, "Content", "", TEST_USERNAME);

        // Assert
        assertNull(response.getParentCommentId());

        ArgumentCaptor<LibraryItemComment> commentCaptor = ArgumentCaptor.forClass(LibraryItemComment.class);
        verify(commentRepository).save(commentCaptor.capture());
        assertNull(commentCaptor.getValue().getParentComment());
    }

    @Test
    void testCommentResponse_editedComment_showsIsEdited() {
        // Arrange
        testComment.setUpdatedAt(LocalDateTime.now()); // Different from createdAt
        when(commentRepository.findTopLevelCommentsByItemId(TEST_ITEM_ID))
            .thenReturn(Collections.singletonList(testComment));
        when(commentRepository.findRepliesByParentComment(testComment))
            .thenReturn(Collections.emptyList());

        // Act
        List<CommentResponse> result = commentService.getComments(TEST_ITEM_ID);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsEdited());
    }

    @Test
    void testCommentResponse_userWithNoDisplayName_usesUsername() {
        // Arrange
        User noNameUser = new User();
        noNameUser.setId(99L);
        noNameUser.setUsername("noname");
        noNameUser.setFirstName(null);
        noNameUser.setLastName(null);

        LibraryItemComment commentByNoName = new LibraryItemComment("no-name-comment", testItem, noNameUser, "Content");
        commentByNoName.setId(100L);
        commentByNoName.setCreatedAt(LocalDateTime.now());
        commentByNoName.setUpdatedAt(LocalDateTime.now());
        commentByNoName.setDeleted(false);

        when(userRepository.findByUsername("noname")).thenReturn(Optional.of(noNameUser));
        when(commentRepository.save(any(LibraryItemComment.class))).thenReturn(commentByNoName);

        // Act
        CommentResponse response = commentService.createComment(TEST_ITEM_ID, "Content", null, "noname");

        // Assert
        assertEquals("noname", response.getUserDisplayName()); // Falls back to username
    }
}
