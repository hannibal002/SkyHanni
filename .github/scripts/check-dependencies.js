module.exports = async ({github, context, core}) => {
    const labelName = "Waiting on Dependency PR";
    const pr = context.payload.pull_request;
    const owner = context.repo.owner;
    const repo = context.repo.repo;
    const issue_number = pr.number;
    const body = pr.body || "";

    // Only run if they declared a Dependencies section
    if (!/## Dependencies/.test(body)) {
        core.info(`Skipping: no Dependencies section in PR ${owner}/${repo}#${issue_number}`);
        return;
    }

    // Pull out all “- https://github.com/.../pull/123” lines
    const links = body.match(/- https:\/\/github\.com\/[\w-]+\/[\w-]+\/pull\/\d+/g) || [];
    if (links.length === 0) {
        core.info(`Skipping: no dependency links found in PR ${owner}/${repo}#${issue_number}`);
        return;
    }

    // See if any of those PRs are still open
    let hasOpen = false;
    for (const link of links) {
        const [, depOwner, depRepo, depNum] =
            link.match(/github\.com\/([\w-]+)\/([\w-]+)\/pull\/(\d+)/);
        const {data: dep} = await github.rest.pulls.get({
            owner: depOwner,
            repo: depRepo,
            pull_number: Number(depNum),
        });
        if (dep.state === "open") {
            hasOpen = true;
            break;
        }
    }

    // Common params between both of the API calls
    const baseParams = { owner, repo, issue_number };
    const existing = pr.labels.map(l => l.name);

    if (hasOpen && !existing.includes(labelName)) {
        await github.rest.issues.addLabels({
            ...baseParams,
            labels: [labelName],
        });
        core.info(`Added label \"${labelName}\" to PR ${owner}/${repo}#${issue_number}`);
    } else if (!hasOpen && existing.includes(labelName)) {
        try {
            await github.rest.issues.removeLabel({
                ...baseParams,
                name: labelName,
            });
            core.info(`Removed label "${labelName}" from PR ${owner}/${repo}#${issue_number}`);
        } catch (err) {
            if (err.status === 404) {
                core.info(`Label "${labelName}" already removed on PR ${owner}/${repo}#${issue_number}`);
            } else {
                throw err;
            }
        }
    } else {
        core.info(`No changes required for label \"${labelName}\" on PR ${owner}/${repo}#${issue_number}`);
    }
};
