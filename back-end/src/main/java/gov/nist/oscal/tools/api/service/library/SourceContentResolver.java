package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.model.library.SourceContent;

public interface SourceContentResolver {

    /** Which builder this resolver primarily handles. */
    SourceType supportedType();

    /** All source types this resolver can handle. Default: just supportedType(). */
    default java.util.Set<SourceType> supportedTypes() {
        return java.util.Set.of(supportedType());
    }

    /**
     * Loads content for the given builder row id. The id is the builder table's
     * primary key (Long), not the library item id.
     *
     * @throws IllegalArgumentException if no row with that id exists
     * @throws SecurityException if the caller is not authorized to read the source row
     */
    SourceContent resolve(Long builderRowId, String callerUsername);
}
