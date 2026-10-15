import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

extensions.configure<PublishingExtension> {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("Vintage Pick Up Notifier")
            description.set("A Minecraft 1.7.10 Forge backport of Pick Up Notifier, maintained by Angelic Industries.")
            url.set("https://github.com/AngelicIndustries/VintagePickUpNotifier")
            organization {
                name.set("Angelic Industries")
                url.set("https://angelic.industries/")
            }
            scm {
                url.set("https://github.com/AngelicIndustries/VintagePickUpNotifier")
                connection.set("scm:git:https://github.com/AngelicIndustries/VintagePickUpNotifier.git")
                developerConnection.set("scm:git:ssh://git@github.com/AngelicIndustries/VintagePickUpNotifier.git")
            }
            licenses {
                license {
                    name.set("GNU Lesser General Public License, version 3")
                    url.set("https://www.gnu.org/licenses/lgpl-3.0.html")
                    distribution.set("repo")
                }
            }
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    if (name.startsWith("runServer")) {
        doFirst("stripClientOnlyMods") {
            classpath = classpath.filter { file ->
                val n = file.name
                !n.contains("ModularUI2", ignoreCase = true) && !n.contains("Baubles-Expanded", ignoreCase = true) && !n.contains("angelica", ignoreCase = true)
            }
        }
    }
}
