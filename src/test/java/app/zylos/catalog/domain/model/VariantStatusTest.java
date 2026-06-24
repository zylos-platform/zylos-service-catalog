package app.zylos.catalog.domain.model;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import app.zylos.catalog.domain.exception.IllegalStatusTransitionException;

class VariantStatusTest {

    @ParameterizedTest
    @CsvSource({
        "ACTIVE,INACTIVE,true",
        "ACTIVE,DISCONTINUED,true",
        "ACTIVE,ACTIVE,false",
        "INACTIVE,ACTIVE,true",
        "INACTIVE,DISCONTINUED,true",
        "INACTIVE,INACTIVE,false",
        "DISCONTINUED,ACTIVE,false",
        "DISCONTINUED,INACTIVE,false",
        "DISCONTINUED,DISCONTINUED,false"
    })
    void transitionMatrix(VariantStatus from, VariantStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    @Test
    void onlyActiveIsPurchasable() {
        assertThat(VariantStatus.ACTIVE.isPurchasable()).isTrue();
        assertThat(VariantStatus.INACTIVE.isPurchasable()).isFalse();
        assertThat(VariantStatus.DISCONTINUED.isPurchasable()).isFalse();
    }

    @Test
    void onlyDiscontinuedIsTerminal() {
        assertThat(VariantStatus.DISCONTINUED.isTerminal()).isTrue();
        assertThat(VariantStatus.ACTIVE.isTerminal()).isFalse();
        assertThat(VariantStatus.INACTIVE.isTerminal()).isFalse();
    }

    @Test
    void ensureCanTransitionThrowsOnIllegalTransition() {
        assertThatThrownBy(() -> VariantStatus.DISCONTINUED.ensureCanTransitionTo(VariantStatus.ACTIVE))
                .isInstanceOf(IllegalStatusTransitionException.class);
    }

    @Test
    void ensureCanTransitionPassesOnLegalTransition() {
        assertThatCode(() -> VariantStatus.ACTIVE.ensureCanTransitionTo(VariantStatus.INACTIVE))
                .doesNotThrowAnyException();
    }
}
