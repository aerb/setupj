import java.io.File
import java.lang.System.err
import java.nio.file.Paths
import kotlin.system.exitProcess

fun fatal(message: String): Nothing {
    err.println(message)
    exitProcess(1)
}

fun main(args: Array<String>) {
    val templates = File("resources").listFiles()
        .filter { it.name.endsWith(".pom.xml") }
        .map { it.name.removeSuffix(".pom.xml") }

    val useage: String by lazy {
        buildString {
            appendln("setupj [--template | -t] [package] [name] [directory]")
            appendln("templates:")
            templates.forEach {
                appendln("- $it")
            }
        }
    }

    val positional = args.toMutableList()

    val i = args.indexOfFirst { it.startsWith("-") }
    val template: String =
        if(i >= 0) {
            val flag = args[i]
            positional.removeAt(i)
            if(flag == "-t" || flag == "--template") {
                val arg = positional.getOrNull(i) ?: fatal("Missing argument for $flag.\n\n$useage")
                positional.removeAt(i)
                arg.takeIf { it in templates } ?: fatal("$arg is an invalidate template.")
            } else {
                fatal("Invalid option $flag\n\n$useage")
            }
        } else {
            "kotlin-common"
        }

    if(positional.size < 3) {
        fatal(useage)
    }

    val userWorkingDir = System.getenv("USER_WORKING_DIR") ?: "."

    val groupId = positional[0]
    val name = positional[1]
    val path = positional[2]

    val rootDir =
        if(path.startsWith("/")) {
            Paths.get(path, name)
        } else {
            Paths.get(userWorkingDir, path, name)
        }.toAbsolutePath().normalize().toFile()

    if(rootDir.exists()) {
        fatal("Directory $rootDir already exists.")
    }

    println("Setting up package=$groupId name=$name template=$template into=$rootDir")
    print("Is this correct? (y/n): ")

    if(readLine()?.toLowerCase() !in setOf("y", "yes")) {
        exitProcess(1)
    }

    fun installResource(source: String, target: String, transform: (String) -> String = { it }) {
        val fp = File(rootDir, target)
        println("Installing $fp")
        fp.parentFile.mkdirs()
        fp.writeText(
            transform(
                File("resources", source).readText()
            )
        )
    }

    installResource("gitignore", ".gitignore")
    installResource("$template.pom.xml", "pom.xml") {
        it.replace("ARTIFACT_NAME", name)
          .replace("GROUP_ID", groupId)
    }

    installResource("EmptyMain.kt", "src/main/kotlin/${groupId.split(".").joinToString("/")}/Main.kt") {
        it.replace("PACKAGE", groupId)
    }

    installResource("EmptyTest.kt", "src/test/kotlin/${groupId.split(".").joinToString("/")}/Tests.kt") {
        it.replace("PACKAGE", groupId)
    }

    val p = ProcessBuilder("git", "init").directory(rootDir).redirectErrorStream(true).start()
    p.inputStream.reader().readLines().forEach { println(it) }
    p.waitFor()
}