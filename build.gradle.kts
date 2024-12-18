plugins {
    java
    application
}

group = "com.example"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    // JavaMail API
    implementation("com.sun.mail:javax.mail:1.6.2")

    // OpenCSV для чтения CSV
    implementation("com.opencsv:opencsv:5.5.2")

    // SLF4J API и Logback для логирования
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("ch.qos.logback:logback-classic:1.2.6")
}

application {
    // Укажите главный класс приложения
    mainClass.set("com.example.EmailSender")
}
