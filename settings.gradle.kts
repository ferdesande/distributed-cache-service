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

            plugin("detekt", "io.gitlab.arturbosch.detekt").versionRef("detekt")
            plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
            plugin("kotlin-spring", "org.jetbrains.kotlin.plugin.spring").versionRef("kotlin")
            plugin("kotlin-jpa", "org.jetbrains.kotlin.plugin.jpa").versionRef("kotlin")
            plugin("spring-boot", "org.springframework.boot").versionRef("spring-boot")
            plugin("spring-dependency-management", "io.spring.dependency-management")
                .versionRef("spring-dependency-management")

            library("spring-boot-starter","org.springframework.boot", "spring-boot-starter").withoutVersion()
            library("kotlin-reflect", "org.jetbrains.kotlin", "kotlin-reflect").withoutVersion()
            library("spring-boot-starter-test", "org.springframework.boot", "spring-boot-starter-test").withoutVersion()
            library("kotlin-test-junit5", "org.jetbrains.kotlin", "kotlin-test-junit5").withoutVersion()
            library("junit-platform-launcher", "org.junit.platform", "junit-platform-launcher").withoutVersion()
        }
    }
}
