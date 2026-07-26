package gov.nist.oscal.tools.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A library file referenced by the database is missing from blob storage.
 * Mapped to 404 so a missing file surfaces as "not found" instead of a
 * generic 500 (GlobalErrorAdvice re-throws @ResponseStatus exceptions).
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class LibraryFileNotFoundException extends RuntimeException {

    public LibraryFileNotFoundException(String blobPath) {
        super("Library file not found: " + blobPath);
    }
}
