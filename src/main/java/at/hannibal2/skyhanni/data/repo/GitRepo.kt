package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.repo.filesystem.RepoFileSystem
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.api.ApiUtils
import at.hannibal2.skyhanni.data.git.commit.CommitsApiResponse
import at.hannibal2.skyhanni.utils.json.fromJsonOrNull
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.PullResult
import org.eclipse.jgit.api.ResetCommand.ResetType
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.merge.ContentMergeStrategy
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.util.FS
import java.io.File

/**
 * Represents the location of a Git repository.
 * @param config the [AbstractRepoLocationConfig] containing the information for this repository.
 * @param shouldErrorProvider if it resolves true, will throw an error if the latest commit SHA cannot be fetched, or if the download fails.
 */
class GitRepo(
    val config: AbstractRepoLocationConfig,
    private val shouldErrorProvider: () -> Boolean = { false },
) {
    private val user get() = config.user
    private val repo get() = config.repoName
    private val branch get() = config.branch

    val location get() = "$user/$repo/$branch"

    private val shouldError get() = shouldErrorProvider()
    private val commitApiUrl: String get() = "https://api.github.com/repos/$user/$repo/commits/$branch"
    private val shallowRefSpec get() = "+refs/heads/$branch:refs/remotes/origin/$branch"
    private val sshConfigurer = TransportConfigCallback { transport ->
        if (transport is SshTransport) transport.sshSessionFactory = SshSessionFactory.getInstance()
    }

    private fun String.isSshUri() = startsWith("git@") || startsWith("ssh://")

    private fun RepoFileSystem.getAvailableSources(): List<String> {
        val sources = mutableListOf("https://github.com/$user/$repo.git")

        val userHome = FS.DETECTED.userHome() ?: return sources.also {
            logger.debug("Skipping SSH fallback: Unable to determine user home directory.")
        }
        val keyPresent = File(userHome, ".ssh").listFiles()?.any { file ->
            file.isFile && !file.name.endsWith(".pub") && file.name.startsWith("id_")
        } ?: false

        if (keyPresent) sources.add("git@github.com:$user/$repo.git")
        else logger.debug("Skipping SSH fallback: No private keys found in ~/.ssh/")

        return sources
    }

    suspend fun getLatestCommit(silentError: Boolean = true): RepoCommit? {
        val (_, jsonResponse) = ApiUtils.getJsonResponse(commitApiUrl, location, silentError).assertSuccessWithData() ?: run {
            SkyHanniMod.logger.error("Failed to fetch latest commits.")
            return null
        }
        val apiResponse = ConfigManager.gson.fromJsonOrNull<CommitsApiResponse>(jsonResponse) ?: run {
            SkyHanniMod.logger.error("Failed to parse latest commit response: $jsonResponse")
            return null
        }
        return RepoCommit(sha = apiResponse.sha, time = apiResponse.commit.committer.date)
    }

    fun RepoFileSystem.loadFromJGit(): Boolean {
        val gitFile = File(root, ".git")
        return if (gitFile.exists() && tryPullRepo() != null) true
        else if (root.listFiles()?.isNotEmpty() == true) tryGitConvertLocalRepo()
        else tryCloneRepo()
    }

    private fun RepoFileSystem.tryGitConvertLocalRepo(): Boolean = runCatching {
        Git.init().setDirectory(root).call().use { git ->
            git.repository.config.apply {
                setString("remote", "origin", "url", "https://github.com/$user/$repo.git")
                setString("remote", "origin", "fetch", shallowRefSpec)
            }.save()

            git.fetch().apply {
                setRemote("origin")
                setDepth(1)
                setRefSpecs(RefSpec(shallowRefSpec))
                setProgressMonitor(RepoJGitMonitor(this@tryGitConvertLocalRepo))
            }.call()

            git.reset().apply {
                setMode(ResetType.HARD)
                setRef("origin/$branch")
                setProgressMonitor(RepoJGitMonitor(this@tryGitConvertLocalRepo))
            }.call()

            git.branchCreate().apply {
                setName(branch)
                setStartPoint("origin/$branch")
                setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                setForce(true)
            }.call()

            logger.debug("Successfully linked and matched existing files for $location")
        }
        true
    }.getOrElse { e ->
        logger.error("Failed to link existing directory to Git: $e")
        false
    }

    private fun RepoFileSystem.tryPullRepo(): PullResult? = runCatching {
        Git.open(root).use { localRepo ->
            val remoteUrl = localRepo.repository.config.getString("remote", "origin", "url").orEmpty()
            localRepo.pull().apply {
                setRemote("origin")
                setRemoteBranchName(branch)
                setStrategy(MergeStrategy.THEIRS)
                setContentMergeStrategy(ContentMergeStrategy.THEIRS)
                if (remoteUrl.isSshUri()) setTransportConfigCallback(sshConfigurer)
                setProgressMonitor(RepoJGitMonitor(this@tryPullRepo))
            }.call()?.takeIf { it.isSuccessful }?.also {
                val latestHash = localRepo.repository.resolve(Constants.HEAD)?.name ?: "<unknown>"
                logger.debug("Pulled latest changes ($latestHash) for $location")
            }
        }
    }.getOrElse { e ->
        logger.error("Failed to pull latest changes for $location\n$e")
        null
    }

    private fun RepoFileSystem.tryCloneRepo(): Boolean = getAvailableSources().firstNotNullOfOrNull { source ->
        val success = runCatching {
            Git.cloneRepository().apply {
                setURI(source)
                setBranch(branch)
                setDirectory(root)
                setDepth(1)
                setCloneAllBranches(false)
                setNoCheckout(false)
                if (source.isSshUri()) setTransportConfigCallback(sshConfigurer)
                setProgressMonitor(RepoJGitMonitor(this@tryCloneRepo))
            }.call().use { cloned ->
                logger.debug("Cloned ${cloned.repository.directory.absolutePath} for $location via $source")
            }
            true
        }.getOrElse { e ->
            logger.error("Failed to clone $location from $source\n$e")
            deleteRecursively("")
            root.mkdirs()
            false
        }
        if (success) true else null
    } ?: false

    fun getLocalHeadSha(repoDir: File): String? = runCatching {
        Git.open(repoDir).use { git ->
            git.repository.resolve(Constants.HEAD)?.name
        }
    }.getOrElse { null }

    suspend fun downloadCommitZipToFile(destinationZip: File, shaOverride: String? = null): Boolean {
        val shaToUse = shaOverride ?: getLatestCommit(!shouldError)?.sha ?: run {
            if (shouldError) ErrorManager.skyHanniError("Cannot get full archive URL without a valid SHA")
            return false
        }
        val fullArchiveUrl = "https://github.com/$user/$repo/archive/$shaToUse.zip"
        return try {
            if (shouldError) {
                SkyHanniMod.logger.info("Downloading $shaToUse for $location\nUrl: $fullArchiveUrl")
            }
            ApiUtils.getZipResponse(destinationZip, fullArchiveUrl, location, !shouldError)
            true
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Failed to download archive from $fullArchiveUrl")
            SkyHanniMod.logger.error("Failed to download archive from $fullArchiveUrl", e)
            false
        }
    }
}
