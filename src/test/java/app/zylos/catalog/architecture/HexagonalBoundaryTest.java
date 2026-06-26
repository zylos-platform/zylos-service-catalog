package app.zylos.catalog.architecture;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "app.zylos.catalog", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalBoundaryTest {

    @ArchTest
    static final ArchRule hexagonal_layers_are_respected = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Domain")
            .definedBy("app.zylos.catalog.domain..")
            .layer("Application")
            .definedBy("app.zylos.catalog.application..")
            .layer("Adapters")
            .definedBy("app.zylos.catalog.adapter..")
            .whereLayer("Adapters")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("Adapters")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Application", "Adapters");
}
