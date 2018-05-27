import java.io.File
import java.lang.System.err
import java.nio.file.Paths
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if(args.size < 3) {
        err.println("setupj [package] [name] [directory]")
        exitProcess(1)
    }

    val userWorkingDir = System.getenv("USER_WORKING_DIR") ?: "."

    val groupId = args[0]
    val name = args[1]
    val rootDir = Paths.get(userWorkingDir, args[2], name).toAbsolutePath().normalize().toFile()
    if(rootDir.exists()) {
        err.println("Directory $rootDir already exists.")
        exitProcess(1)
    }

    println("Setting up package=$groupId name=$name into=$rootDir")

    rootDir.mkdirs()
    File("resources", "gitignore").copyTo(File(rootDir, ".gitignore"))
    File(rootDir, "pom.xml").writeText(
        File("resources", "kotlin-common.template.xml").readText()
            .replace("ARTIFACT_NAME", name)
            .replace("GROUP_ID", groupId)
    )

    val srcDir = File(rootDir, "src/main/kotlin/${groupId.split(".").joinToString("/")}")
    srcDir.mkdirs()
    File(srcDir, "Main.kt").writeText("""

        fun main(args: Array<String>) {

        }
    """.trimIndent())

}