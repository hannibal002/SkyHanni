package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.git.commit.CommitsApiResponse
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.api.ApiUtils
import at.hannibal2.skyhanni.utils.json.fromJsonOrNull
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun downloadCommitTgzToFile(destinationTgz: File, shaOverride: String? = null): Boolean {
        val shaToUse = shaOverride ?: getLatestCommit(!shouldError)?.sha ?: run {
            if (shouldError) ErrorManager.skyHanniError("Cannot get full tar.gz URL without a valid SHA")
            return false
        }
        val fullArchiveUrl = "https://github.com/$user/$repo/archive/$shaToUse.tar.gz"

        val tmpFile = destinationTgz.resolveSibling("${destinationTgz.name}.tmp")

        return try {
            if (shouldError) {
                SkyHanniMod.logger.info("Downloading $shaToUse for $location\nUrl: $fullArchiveUrl")
            }
            tmpFile.parentFile?.mkdirs()
            ApiUtils.getBinaryResponse(tmpFile, fullArchiveUrl, location, !shouldError).assertSuccessWithData() ?: run {
                if (tmpFile.exists()) tmpFile.delete()
                return false
            }

            withContext(Dispatchers.IO) {
                OSUtils.atomicMoveFile(tmpFile, destinationTgz)
            }

            true
        } catch (e: Exception) {
            if (tmpFile.exists()) tmpFile.delete()

            if (shouldError) {
                ErrorManager.logErrorWithData(e, "Failed to write or swap tar.gz from $fullArchiveUrl")
                SkyHanniMod.logger.error("Failed to write or swap tar.gz from $fullArchiveUrl", e)
            }
            false
        }
    }
}
