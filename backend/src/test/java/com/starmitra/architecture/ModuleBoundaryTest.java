package com.starmitra.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Modular-monolith boundary enforcement (§27) —
 * 1. api layer never reaches persistence of ANY module
 * 2. a module's persistence is only touched by its own application/domain/api
 * 3. portal modules (admin/judgeportal) own no persistence
 */
class ModuleBoundaryTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.starmitra");

    private static final String[] DOMAIN_MODULES = {
            "identity", "profile", "skill", "media", "discovery", "connect", "room",
            "portfolio", "competition", "submission", "voting", "judge", "rubric",
            "scoring", "progression", "leaderboard", "notification", "moderation", "social",
            "pricing"};

    @Test
    void apiLayerNeverTouchesPersistence() {
        for (String m : DOMAIN_MODULES) {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..modules." + m + ".api..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..modules.*.persistence..")
                    .as(m + ".api must not touch persistence");
            rule.check(CLASSES);
        }
    }

    @Test
    void modulesDoNotTouchOtherModulesPersistence() {
        for (String owner : DOMAIN_MODULES) {
            for (String other : DOMAIN_MODULES) {
                if (owner.equals(other)) continue;
                ArchRule rule = noClasses()
                        .that().resideInAPackage("..modules." + other + "..")
                        .should().dependOnClassesThat()
                        .resideInAPackage("..modules." + owner + ".persistence..")
                        .as(other + " must not depend on " + owner + ".persistence");
                rule.check(CLASSES);
            }
        }
    }

    @Test
    void portalModulesOwnNoPersistence() {
        for (String portal : new String[]{"admin", "judgeportal"}) {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..modules." + portal + "..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..modules.*.persistence..")
                    .as(portal + " must orchestrate via contracts, not persistence");
            rule.check(CLASSES);
        }
    }

    @Test
    void controllersNeverExposeEntities() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..api..")
                .should().dependOnClassesThat()
                .resideInAPackage("..persistence..")
                .as("api layer must use DTOs, not entities");
        rule.check(CLASSES);
    }
}
