rootProject.name = "config-server"

pluginManagement {
    plugins {
        kotlin("jvm") version (extra["kotlin.version"] as String)
        id("org.octopusden.release-management") version (extra["release-management.version"] as String)
        id("org.springframework.boot") version (extra["spring-boot.version"] as String)
    }
}
