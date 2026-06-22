package app.zylos.catalog.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProductStatusTest {

    @ParameterizedTest
    @CsvSource({
        "DRAFT,PUBLISHED,true",
        "DRAFT,DISCONTINUED,true",
        "DRAFT,DRAFT,false",
        "PUBLISHED,DISCONTINUED,true",
        "PUBLISHED,DRAFT,false",
        "PUBLISHED,PUBLISHED,false",
        "DISCONTINUED,PUBLISHED,false",
        "DISCONTINUED,DRAFT,false",
        "DISCONTINUED,DISCONTINUED,false"
    })
    void transitionMatrix(ProductStatus from, ProductStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    @Test
    void onlyDiscontinuedIsTerminal() {
        assertThat(ProductStatus.DISCONTINUED.isTerminal()).isTrue();
        assertThat(ProductStatus.DRAFT.isTerminal()).isFalse();
        assertThat(ProductStatus.PUBLISHED.isTerminal()).isFalse();
    }

    @Test
    void ensureCanTransitionThrowsOnIllegalTransition() {
        assertThatThrownBy(() -> ProductStatus.DISCONTINUED.ensureCanTransitionTo(ProductStatus.PUBLISHED))
                .isInstanceOf(IllegalStatusTransitionException.class);
    }

    @Test
    void ensureCanTransitionPassesOnLegalTransition() {
        assertThatCode(() -> ProductStatus.DRAFT.ensureCanTransitionTo(ProductStatus.PUBLISHED))
                .doesNotThrowAnyException();
    }
}
