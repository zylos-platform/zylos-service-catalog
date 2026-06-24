package app.zylos.catalog.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import app.zylos.catalog.domain.exception.IllegalStatusTransitionException;

class ProductStatusTest {

    @ParameterizedTest
    @CsvSource({
        "DRAFT,PUBLISHED,true",
        "DRAFT,DISCONTINUED,true",
        "DRAFT,UNPUBLISHED,false",
        "DRAFT,DRAFT,false",
        "PUBLISHED,UNPUBLISHED,true",
        "PUBLISHED,DISCONTINUED,true",
        "PUBLISHED,DRAFT,false",
        "PUBLISHED,PUBLISHED,false",
        "UNPUBLISHED,PUBLISHED,true",
        "UNPUBLISHED,DISCONTINUED,true",
        "UNPUBLISHED,DRAFT,false",
        "DISCONTINUED,PUBLISHED,false",
        "DISCONTINUED,UNPUBLISHED,false",
        "DISCONTINUED,DRAFT,false",
        "DISCONTINUED,DISCONTINUED,false"
    })
    void transitionMatrix(ProductStatus from, ProductStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    @Test
    void draftIsAOneWayOnRamp() {
        assertThat(ProductStatus.PUBLISHED.canTransitionTo(ProductStatus.DRAFT)).isFalse();
        assertThat(ProductStatus.UNPUBLISHED.canTransitionTo(ProductStatus.DRAFT))
                .isFalse();
    }

    @Test
    void onlyDiscontinuedIsTerminal() {
        assertThat(ProductStatus.DISCONTINUED.isTerminal()).isTrue();
        assertThat(ProductStatus.DRAFT.isTerminal()).isFalse();
        assertThat(ProductStatus.PUBLISHED.isTerminal()).isFalse();
        assertThat(ProductStatus.UNPUBLISHED.isTerminal()).isFalse();
    }

    @Test
    void ensureCanTransitionThrowsOnIllegalTransition() {
        assertThatThrownBy(() -> ProductStatus.DISCONTINUED.ensureCanTransitionTo(ProductStatus.PUBLISHED))
                .isInstanceOf(IllegalStatusTransitionException.class);
    }

    @Test
    void ensureCanTransitionPassesOnLegalTransition() {
        assertThatCode(() -> ProductStatus.PUBLISHED.ensureCanTransitionTo(ProductStatus.UNPUBLISHED))
                .doesNotThrowAnyException();
    }
}
