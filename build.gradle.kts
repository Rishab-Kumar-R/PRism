plugins {
    java
    id("io.quarkus")
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:quarkus-langchain4j-bom:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-amazon-lambda-http")
    implementation("io.quarkus:quarkus-jdbc-h2")
    implementation("io.quarkus:quarkus-hibernate-orm-panache")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkiverse.githubapp:quarkus-github-app:2.14.1")
    implementation("io.quarkiverse.langchain4j:quarkus-langchain4j-ai-gemini")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-hibernate-orm")
    implementation("io.quarkus:quarkus-rest")
    testImplementation("io.quarkus:quarkus-junit")
    testImplementation("io.rest-assured:rest-assured")
}

group = "dev.rishabkumar"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}
