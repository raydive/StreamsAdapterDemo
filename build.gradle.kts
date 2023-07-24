plugins {
    id("java")
}

group = "com.github.raydive"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.amazonaws:aws-java-sdk-kinesis:1.12.512")
    implementation("com.amazonaws:aws-java-sdk-dynamodb:1.12.512")
    implementation("com.amazonaws:aws-java-sdk-cloudwatch:1.12.512")
    implementation("com.amazonaws:amazon-kinesis-client:1.15.0")
    implementation("com.amazonaws:dynamodb-streams-kinesis-adapter:1.6.0")
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

tasks.test {
    useJUnitPlatform()
}