package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.DocumentType;

import java.util.List;

/**
 * Response for the package-completeness panel: for each "core" document type,
 * how many non-expired documents of that type are attached to the authorization.
 */
public class PackageCompletenessResponse {

    public static class Item {
        private DocumentType documentType;
        private int presentCount;
        private boolean satisfied;

        public Item() {}

        public Item(DocumentType documentType, int presentCount) {
            this.documentType = documentType;
            this.presentCount = presentCount;
            this.satisfied = presentCount > 0;
        }

        public DocumentType getDocumentType() { return documentType; }
        public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }
        public int getPresentCount() { return presentCount; }
        public void setPresentCount(int presentCount) { this.presentCount = presentCount; }
        public boolean isSatisfied() { return satisfied; }
        public void setSatisfied(boolean satisfied) { this.satisfied = satisfied; }
    }

    private List<Item> coreDocuments;

    public PackageCompletenessResponse() {}

    public PackageCompletenessResponse(List<Item> coreDocuments) {
        this.coreDocuments = coreDocuments;
    }

    public List<Item> getCoreDocuments() { return coreDocuments; }
    public void setCoreDocuments(List<Item> coreDocuments) { this.coreDocuments = coreDocuments; }
}
