package at.hannibal2.skyhanni.data.repo.filesystem

import at.hannibal2.skyhanni.data.repo.RepoLogger
import java.io.File

class DiskRepoFileSystem(
    override val root: File,
    override val logger: RepoLogger,
) : RepoFileSystem {
    override fun exists(path: String) = File(root, path).isFile
    override fun readAllBytes(path: String) = File(root, path).readBytes()
    override fun write(path: String, data: ByteArray) {
        val f = File(root, path)
        f.parentFile.mkdirs()
        f.writeBytes(data)
    }

    override fun deleteRecursively(path: String) {
        File(root, path).deleteRecursively()
    }

    override fun list(path: String) = root.resolve(path).listFiles { file ->
        file.exists() && file.extension == "json"
    }?.mapNotNull { it.name }?.toList().orEmpty()

    override fun validatePath(relativePath: String) {
        val outPath = root.toPath().resolve(relativePath).normalize()
        if (!outPath.startsWith(root.toPath())) throw RuntimeException(
            "SkyHanni detected an invalid zip file. This is a potential security risk, " +
                "please report this on the SkyHanni discord.",
        )
    }
}
