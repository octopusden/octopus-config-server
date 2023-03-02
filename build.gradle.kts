plugins {
    kotlin("jvm")
    id("org.springframework.boot")
    id("com.bmuschko.docker-spring-boot-application") version "7.1.0"
    id("maven-publish")
    id("io.github.gradle-nexus.publish-plugin")
    signing
}

group = "org.octopusden.cloud.config-server"

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<GenerateModuleMetadata> {
    // The value 'enforced-platform' is provided in the validation
    // error message
    suppressedValidationErrors.add("enforced-platform")
}

repositories {
    mavenCentral()
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(System.getenv("MAVEN_USERNAME"))
            password.set(System.getenv("MAVEN_PASSWORD"))
        }
    }
}

publishing {
    repositories {
        maven {

        }
    }
    publications {
        create<MavenPublication>("bootJar") {
            from(components["java"])
            artifact(tasks.getByName("bootJar"))
            pom {
                name.set(project.name)
                description.set("Octopus module: ${project.name}")
                url.set("https://github.com/octopusden/octopus-config-server.git")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    url.set("https://github.com/kzaporozhtsev/octopus-config-server.git")
                    connection.set("scm:git://github.com/octopusden/octopus-config-server.git")
                }
                developers {
                    developer {
                        id.set("octopus")
                        name.set("octopus")
                    }
                }
            }
        }
    }
}


signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["bootJar"])
}

springBoot {
    buildInfo()
}

docker {
    springBootApplication {
        baseImage.set("${rootProject.properties["docker.registry"]}/openjdk:11")
        ports.set(listOf(8765, 8765))
        images.set(setOf("${rootProject.properties["publishing.docker.registry"]}/${project.name}:${project.version}"))
    }
}

dependencies {
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:${project.property("spring-cloud.version")}"))
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${project.property("spring-boot.version")}"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.cloud:spring-cloud-config-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus:1.10.4")
}
