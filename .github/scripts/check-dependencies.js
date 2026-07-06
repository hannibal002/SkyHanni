// Execution context: base branch, called from check_dependencies.yml

module.exports = async ({github, context, core}) => {
  const labelName = "Waiting on Dependency PR";
  const pr = context.payload.pull_request;
  const owner = context.repo.owner;
  const repo = context.repo.repo;

  if (pr.state === 'closed') {
      await recheckAllDependentPRs({ github, owner, repo, labelName, core });
      return;
  }

  await checkPrDependencies({
      github,
      owner,
      repo,
      issue_number: pr.number,
      body: pr.body || '',
      currentLabels: pr.labels,
      labelName,
      core,
  });
};


async function checkPrDependencies({ github, owner, repo, issue_number, body, currentLabels, labelName, core }) {
  if (!/## Dependencies/.test(body)) {
      core.info(`Skipping: no Dependencies section in PR ${owner}/${repo}#${issue_number}`);
      return;
  }

  const dependencies = [];

  const urlLinks = body.match(/- https:\/\/github\.com\/[\w-]+\/[\w-]+\/pull\/\d+/g) || [];
  for (const link of urlLinks) {
      const [, depOwner, depRepo, depNum] = link.match(/github\.com\/([\w-]+)\/([\w-]+)\/pull\/(\d+)/);
      dependencies.push({ owner: depOwner, repo: depRepo, pull_number: Number(depNum) });
  }

  for (const match of body.matchAll(/- #(\d+)/g)) {
      dependencies.push({ owner, repo, pull_number: Number(match[1]) });
  }

  if (dependencies.length === 0) {
      core.info(`Skipping: no dependency links found in PR ${owner}/${repo}#${issue_number}`);
      return;
  }

  let hasOpen = false;
  for (const dep of dependencies) {
      if (dep.owner === "hannibal002" && dep.repo === "SkyHanni-REPO") continue;
      let depPr;
      try {
          const { data } = await github.rest.pulls.get({
              owner: dep.owner,
              repo: dep.repo,
              pull_number: dep.pull_number,
          });
          depPr = data;
      } catch (err) {
          if (err.status === 404) {
              core.warning(`Dependency PR ${dep.owner}/${dep.repo}#${dep.pull_number} not found, skipping`);
              continue;
          }
          throw err;
      }
      if (depPr.state === "open") {
          hasOpen = true;
          break;
      }
  }

  const baseParams = { owner, repo, issue_number };
  const existing = currentLabels.map(l => l.name);

  if (hasOpen && !existing.includes(labelName)) {
      await github.rest.issues.addLabels({ ...baseParams, labels: [labelName] });
      core.info(`Added label "${labelName}" to PR ${owner}/${repo}#${issue_number}`);
  } else if (!hasOpen && existing.includes(labelName)) {
      try {
          await github.rest.issues.removeLabel({ ...baseParams, name: labelName });
          core.info(`Removed label "${labelName}" from PR ${owner}/${repo}#${issue_number}`);
      } catch (err) {
          if (err.status === 404) {
              core.info(`Label "${labelName}" already removed on PR ${owner}/${repo}#${issue_number}`);
          } else {
              throw err;
          }
      }
  } else {
      core.info(`No changes required for label "${labelName}" on PR ${owner}/${repo}#${issue_number}`);
  }
}

async function recheckAllDependentPRs({ github, owner, repo, labelName, core }) {
  core.info(`PR closed, rechecking all open PRs with label "${labelName}"`);
  const openPrs = await github.paginate(github.rest.issues.listForRepo, {
      owner,
      repo,
      labels: labelName,
      state: 'open',
      per_page: 100,
  });

  for (const issue of openPrs) {
      if (!issue.pull_request) continue;
      await checkPrDependencies({
          github,
          owner,
          repo,
          issue_number: issue.number,
          body: issue.body || '',
          currentLabels: issue.labels,
          labelName,
          core,
      });
  }
}
