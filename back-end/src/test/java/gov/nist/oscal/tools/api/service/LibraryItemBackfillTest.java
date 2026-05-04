package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.Visibility;
import gov.nist.oscal.tools.api.repository.LibraryItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LibraryItemBackfillTest {

    @Autowired
    LibraryItemRepository repo;

    @Test
    void allExistingRowsDefaultToPrivate() {
        List<LibraryItem> all = repo.findAll();
        assertThat(all).allMatch(i -> i.getVisibility() == Visibility.PRIVATE);
    }
}
