rootProject.name = "config-server"

pluginManagement {
    plugins {
        kotlin("jvm") version (extra["kotlin.version"] as String)
        id("org.springframework.boot") version (extra["spring-boot.version"] as String)
        id("io.github.gradle-nexus.publish-plugin") version("1.1.0") apply(false)
        id("com.bmuschko.docker-spring-boot-application") version (extra["bmuschko-docker-plugin.version"] as String)
    }
}
