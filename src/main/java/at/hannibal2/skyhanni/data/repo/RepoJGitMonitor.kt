package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.data.repo.filesystem.RepoFileSystem
import org.eclipse.jgit.lib.ProgressMonitor

class RepoJGitMonitor(private val repoFs: RepoFileSystem) : ProgressMonitor {
    private var totalTasks = 0
    private var completedTasks = 0

    override fun start(totalTasks: Int) {
        this.totalTasks = totalTasks
    }

    override fun beginTask(title: String, totalWork: Int) {
        repoFs.logger.debug("Starting task: $title ($totalWork units)")
    }

    override fun update(completed: Int) {
        // This is called frequently (like every percent/file)
        // Usually, you don't want to log every single update to avoid spam
    }

    override fun endTask() {
        completedTasks++
        repoFs.logger.debug("Task completed ($completedTasks/$totalTasks)")
    }

    override fun isCancelled(): Boolean = false
    override fun showDuration(enabled: Boolean) = Unit
}
