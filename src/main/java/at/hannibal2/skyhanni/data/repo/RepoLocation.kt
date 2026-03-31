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
 * Represents the location of a GitHub repository.
 * @param user The GitHub username or organization.
 * @param repo The repository name.
 * @param branch The branch name, defaults to "main".
 * @param shouldError If true, will throw an error if the latest commit SHA cannot be fetched, or if the download fails.
 */
data class RepoLocation(
    val user: String,
    val repo: String,
    val branch: String = "main",
    private val shouldError: Boolean = false,
) {
    companion object {
        private const val DEF_REF_SPEC = "+refs/heads/*:refs/remotes/origin/*"
    }

    constructor(config: AbstractRepoLocationConfig, shouldError: Boolean = false) : this(
        config.user,
        config.repoName,
        config.branch,
        shouldError,
    )

    val location = "$user/$repo/$branch"
    private val apiName = "GitHub - $location"
    private val commitApiUrl: String = "https://api.github.com/repos/$user/$repo/commits/$branch"
    private val sshConfigurer = TransportConfigCallback { transport ->
        if (transport is SshTransport) transport.sshSessionFactory = SshSessionFactory.getInstance()
    }

    private fun String.isSshUri() = startsWith("git@") || startsWith("ssh://")

    private fun RepoFileSystem.getAvailableSources(): List<String> {
        val sources = mutableListOf(
            "https://github.com/$user/$repo.git",
            "https://mirror.ghproxy.com/https://github.com/$user/$repo.git"
        )

        val userHome = FS.DETECTED.userHome() ?: return sources.also {
            logger.debug("Skipping SSH fallback: Unable to determine user home directory.")
        }
        // Check if the directory exists and has any files that don't end in .pub
        val keyPresent = File(userHome, ".ssh").listFiles()?.any { file ->
            file.isFile && !file.name.endsWith(".pub") && file.name.startsWith("id_")
        } ?: false

        if (keyPresent) sources.add("git@github.com:$user/$repo.git")
        else logger.debug("Skipping SSH fallback: No private keys found in ~/.ssh/")

        return sources
    }

    suspend fun getLatestCommit(silentError: Boolean = true): RepoCommit? {
        val (_, jsonResponse) = ApiUtils.getJsonResponse(commitApiUrl, apiName, silentError).assertSuccessWithData() ?: run {
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
                setString("remote", "origin", "fetch", DEF_REF_SPEC)
            }.save()

            git.fetch().apply {
                setRemote("origin")
                setDepth(1)
                setRefSpecs(RefSpec(DEF_REF_SPEC))
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
        logger.logNonDestructiveError("Failed to link existing directory to Git: $e")
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
        logger.logNonDestructiveError("Failed to pull latest changes for $location\n$e")
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
            logger.logNonDestructiveError("Failed to clone $location from $source\n$e")
            root.deleteRecursively()
            root.mkdirs()
            false
        }
        if (success) true else null
    } ?: false

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
            ApiUtils.getZipResponse(destinationZip, fullArchiveUrl, apiName, !shouldError)
            true
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Failed to download archive from $fullArchiveUrl")
            SkyHanniMod.logger.error("Failed to download archive from $fullArchiveUrl", e)
            false
        }
    }
}
