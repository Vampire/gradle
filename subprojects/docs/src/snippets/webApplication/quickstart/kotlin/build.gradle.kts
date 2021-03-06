// tag::use-war-plugin[]
plugins {
    war
}
// end::use-war-plugin[]

repositories {
    mavenCentral()
}

dependencies {
    implementation(group = "commons-io", name = "commons-io", version = "1.4")
    implementation(group = "log4j", name = "log4j", version = "1.2.15", ext = "jar")
}
