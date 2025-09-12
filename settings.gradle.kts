rootProject.name = "cacheservice"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("kotlin", "2.0.21")
            version("spring-dependency-management", "1.1.7")
            version("spring-boot", "3.5.5")
            version("detekt", "1.23.8")
            version("testcontainers", "1.21.3")
            version("restassured", "5.5.6")

            plugin("detekt", "io.gitlab.arturbosch.detekt").versionRef("detekt")
            plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
            plugin("kotlin-spring", "org.jetbrains.kotlin.plugin.spring").versionRef("kotlin")
            plugin("kotlin-jpa", "org.jetbrains.kotlin.plugin.jpa").versionRef("kotlin")
            plugin("spring-boot", "org.springframework.boot").versionRef("spring-boot")
            plugin("spring-dependency-management", "io.spring.dependency-management")
                .versionRef("spring-dependency-management")

            library("spring-boot-starter", "org.springframework.boot", "spring-boot-starter").withoutVersion()
            library(
                "spring-boot-starter-data-redis",
                "org.springframework.boot",
                "spring-boot-starter-data-redis"
            ).withoutVersion()
            library("kotlin-reflect", "org.jetbrains.kotlin", "kotlin-reflect").withoutVersion()
            library("spring-boot-starter-web", "org.springframework.boot", "spring-boot-starter-web").withoutVersion()
            library("spring-boot-starter-aop", "org.springframework.boot", "spring-boot-starter-aop").withoutVersion()
            library("spring-boot-starter-test", "org.springframework.boot", "spring-boot-starter-test").withoutVersion()
            library("spring-boot-starter-validation", "org.springframework.boot", "spring-boot-starter-validation")
                .withoutVersion()
            library("kotlin-test-junit5", "org.jetbrains.kotlin", "kotlin-test-junit5").withoutVersion()
            library("junit-platform-launcher", "org.junit.platform", "junit-platform-launcher").withoutVersion()
            library("tngtech-archunit", "com.tngtech.archunit", "archunit-junit5").version("1.4.1")
            library("testcontainers-bom", "org.testcontainers", "testcontainers-bom").versionRef("testcontainers")
            library("testcontainers-junit-jupiter", "org.testcontainers", "junit-jupiter").withoutVersion()
            library("testcontainers-testcontainers", "org.testcontainers", "testcontainers").withoutVersion()
            library("restassured-core", "io.rest-assured", "rest-assured").versionRef("restassured")
            library("restassured-kotlin", "io.rest-assured", "kotlin-extensions").versionRef("restassured")
        }
    }
}
