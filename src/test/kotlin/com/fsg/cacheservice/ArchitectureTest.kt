package com.fsg.cacheservice

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import com.tngtech.archunit.library.GeneralCodingRules
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition
import com.tngtech.archunit.library.plantuml.rules.PlantUmlArchCondition
import org.junit.jupiter.api.DisplayName
import java.util.*

@AnalyzeClasses(
    packages = ["com.fsg.cacheservice"],
    importOptions = [ImportOption.DoNotIncludeTests::class]
)
@DisplayName("Architecture Tests")
class ArchitectureTest {

    companion object {
        private const val PROJECT_ROOT = "com.fsg.cacheservice"
        private const val API_LAYER = "$PROJECT_ROOT.api"
        private const val CORE_LAYER = "$PROJECT_ROOT.core"
        private const val INFRASTRUCTURE_LAYER = "$PROJECT_ROOT.infrastructure"
        private const val CONFIGURATION_LAYER = "$PROJECT_ROOT.configuration"
        private const val SPRING_BOOT_SUBPACKAGES = "org.springframework.."
        private const val PACKAGES_ARCH_FILE = "/architecture.puml"
    }

    // HINT: when the code is ready, allowEmptyShould must be removed. It's a lifeguard to allow empty packages.
    @ArchTest
    fun `plantuml diagram is not violated`(classes: JavaClasses) {
        val plantUmlDiagram = ArchitectureTest::class.java.getResource(PACKAGES_ARCH_FILE)
        ArchRuleDefinition.classes()
            .should(
                PlantUmlArchCondition.adhereToPlantUmlDiagram(
                    Objects.requireNonNull(plantUmlDiagram),
                    PlantUmlArchCondition.Configuration.consideringOnlyDependenciesInAnyPackage("$PROJECT_ROOT..")
                )
            )
            .allowEmptyShould(true)
            .check(classes)
    }

    @ArchTest
    fun `no cycles`(classes: JavaClasses) {
        SlicesRuleDefinition.slices()
            .matching("$PROJECT_ROOT.(*)..")
            .should()
            .beFreeOfCycles()
            .allowEmptyShould(true)
            .check(classes)
    }

    @ArchTest
    fun `no class can access standard streams`(classes: JavaClasses) {
        ArchRuleDefinition.noClasses()
            .should(GeneralCodingRules.ACCESS_STANDARD_STREAMS)
            .check(classes)
    }

    @ArchTest
    fun `core should not depend on spring framework`(classes: JavaClasses) {
        ArchRuleDefinition.noClasses()
            .that().resideInAPackage("$CORE_LAYER..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(SPRING_BOOT_SUBPACKAGES)
            .because("Core should contain pure business logic without framework dependencies")
            .allowEmptyShould(true)
            .check(classes)
    }

    @ArchTest
    fun `core should not depend on external libraries`(classes: JavaClasses) {
        ArchRuleDefinition.noClasses()
            .that().resideInAPackage("$CORE_LAYER..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "redis.clients..",
                "io.lettuce..",
                "com.fasterxml.jackson..",
                "org.apache.commons.."
            )
            .because("Core should be framework and library agnostic")
            .allowEmptyShould(true)
            .check(classes)
    }

    @ArchTest
    fun `no layer should depend on base except for bootstrap`(classes: JavaClasses) {
        ArchRuleDefinition.noClasses()
            .that().resideInAnyPackage(
                "$API_LAYER..",
                "$CORE_LAYER..",
                "$INFRASTRUCTURE_LAYER..",
                "$CONFIGURATION_LAYER.."
            )
            .should().dependOnClassesThat()
            .resideInAPackage(PROJECT_ROOT)
            .because("Only bootstrap classes should reference base layer")
            .allowEmptyShould(true)
            .check(classes)
    }
}
