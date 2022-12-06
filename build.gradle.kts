plugins {
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.13.0"
}

group = "org.hezisudio"
version = "0.1.1"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}
dependencies{
//    implementation ("com.beust:klaxon:5.5")
    implementation ("io.github.evanrupert:excelkt:1.0.2")
}
