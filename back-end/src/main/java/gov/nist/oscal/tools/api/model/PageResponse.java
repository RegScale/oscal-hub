package gov.nist.oscal.tools.api.model;

import java.util.List;

/**
 * Generic pagination response wrapper for list endpoints.
 * Provides consistent pagination metadata across all paginated API responses.
 *
 * @param <T> The type of items in the page
 */
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;

    // Default constructor
    public PageResponse() {
    }

    // Constructor for Spring Data Page conversion
    public PageResponse(org.springframework.data.domain.Page<T> springPage) {
        this.content = springPage.getContent();
        this.page = springPage.getNumber();
        this.size = springPage.getSize();
        this.totalElements = springPage.getTotalElements();
        this.totalPages = springPage.getTotalPages();
        this.first = springPage.isFirst();
        this.last = springPage.isLast();
        this.hasNext = springPage.hasNext();
        this.hasPrevious = springPage.hasPrevious();
    }

    // Constructor for manual pagination (when data is already fetched)
    public PageResponse(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        this.first = page == 0;
        this.last = page >= totalPages - 1;
        this.hasNext = page < totalPages - 1;
        this.hasPrevious = page > 0;
    }

    // Static factory method for converting Spring Page with mapped content
    public static <T, R> PageResponse<R> of(org.springframework.data.domain.Page<T> springPage,
                                            java.util.function.Function<T, R> mapper) {
        List<R> mappedContent = springPage.getContent().stream()
                .map(mapper)
                .collect(java.util.stream.Collectors.toList());

        PageResponse<R> response = new PageResponse<>();
        response.content = mappedContent;
        response.page = springPage.getNumber();
        response.size = springPage.getSize();
        response.totalElements = springPage.getTotalElements();
        response.totalPages = springPage.getTotalPages();
        response.first = springPage.isFirst();
        response.last = springPage.isLast();
        response.hasNext = springPage.hasNext();
        response.hasPrevious = springPage.hasPrevious();
        return response;
    }

    // Getters and Setters
    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
}
