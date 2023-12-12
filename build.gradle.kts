
plugins {
    id("java")
    application
}

group = "pl.marcinmilkowski"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:24.0.0")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.apache.lucene:lucene-core:9.7.0")
    implementation("org.apache.lucene:lucene-analysis-common:9.7.0")
    implementation("org.apache.lucene:lucene-queryparser:9.7.0")
    implementation("org.apache.commons:commons-csv:1.10.0")
    implementation("net.loomchild:segment:2.0.1")
    // JAX-B dependencies for JDK 9+
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:2.3.2")
    implementation("org.glassfish.jaxb:jaxb-runtime:2.3.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0-rc2")
    implementation("com.github.java-json-tools:json-schema-validator:2.2.14")
    testImplementation("junit:junit:4.13.1")
}

tasks.test {
    useJUnitPlatform()
}

application {
    // Define the main class for the application.
    mainClass.set("pl.marcinmilkowski.JSONSearcher")
}
