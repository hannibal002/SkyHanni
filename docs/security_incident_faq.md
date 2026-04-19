
**SkyHanni got hacked on April 18, 2026 at 00:01 UTC. An attacker found a way to upload malicious mod versions to our GitHub releases for a short period of time. Current data shows a very small number of affected users, though an exact total cannot be confirmed. No maintainer account was accessed, and Modrinth downloads were unaffected.** **You are potentially at risk if:**
* **You downloaded ANY version of SkyHanni 8.x.x from GitHub.**
* **You manually downloaded from GitHub Releases during the affected window (see FAQ for times).**
* **Your game pulled an update via the GitHub auto-updater during the affected window AND you have restarted your game since then.**

**If you downloaded exclusively from Modrinth, you are safe.**

**Full details, precise timelines, and step-by-step instructions on what to do if you might be affected are below.**

<details>
<summary>SkyHanni Security Incident FAQ</summary>

# SkyHanni Security Incident FAQ

On April 18, 2026 at 00:01 UTC, an attacker uploaded malicious SkyHanni versions through stolen GitHub Actions tokens. This document covers what happened, who is affected, and what you should do.

## 1. What happened?

An attacker opened a pull request that automatically triggered a `pull_request_target` workflow. No maintainer reviewed or approved this pull request. The workflow ran immediately when the PR was opened and allowed the attacker to steal our GitHub Actions tokens and repository secrets. With those, they uploaded malicious versions of the mod to GitHub Releases. Modrinth was not affected. The malicious mod contained malware that can steal account session tokens and credentials.

## 2. Which versions are affected?

Compromised versions on GitHub Releases:

- **8.0.0, 8.0.1, 8.0.2** (fake versions uploaded by the attacker, we never released these)
- **7.16.1** (our legitimate release, but the JAR was replaced with a malicious one)
- **7.0.0** (our legitimate release, but the JAR was replaced with a malicious one)

**Versions downloaded from Modrinth are safe.** The attack only affected GitHub Releases.

## 3. When did this happen?

| Version | Start Time (UTC)    | End Time (UTC)      | Duration |
|---------|---------------------|---------------------|----------|
| 7.0.0   | 2026-04-18 00:??:?? | 2026-04-18 01:34:43 | <90 min  |
| 7.16.1  | 2026-04-18 00:??:?? | 2026-04-18 01:34:43 | <90 min  |
| 8.0.0   | 2026-04-18 00:01:44 | 2026-04-18 00:11:31 | ~10 min  |
| 8.0.1   | 2026-04-18 00:25:36 | 2026-04-18 00:26:36 | <1 min   |
| 8.0.2   | 2026-04-18 00:27:13 | 2026-04-18 00:28:13 | <1 min   |


If your mod was not downloaded or updated during these windows, you are not affected.

## 4. Am I affected?

You are at risk if **all** of the following are true:
- Your mod was downloaded or updated from GitHub Releases or the GitHub auto-updater during one of the time windows above
- You launched the game with that mod at any point after the download, even days later

If you downloaded SkyHanni from Modrinth, you are not affected. If your mod was installed before the incident and was not updated during the affected windows, you are safe.

## 5. How many users were affected?

Confirmed download counts are limited: the modified 7.0.0 JARs had 4 combined downloads. We are only aware of 1 user that ran any compromised version, but we cannot confirm a specific total number. The overall impact appears to be small. Claims of thousands of affected users are circulating but are not supported by any data we have.

## 6. How do I check which version I have?

Check the filename of the JAR in your `.minecraft/mods` folder. Do not launch the game just to check your version.

## 7. How do I verify my mod is clean?

Calculate the SHA256 hash of the JAR file in your mods folder and compare it to the known hashes below. You can use this online tool: <https://emn178.github.io/online-tools/sha256_checksum.html>

**Clean hashes:**
- 7.16.1: `0fc2457d2cd5e5e68974eb818cbdea20c760ef2f0ccad1a19435ecad45ec4820`
- 7.0.0 (mc 1.21.10): `d791252eed960ef33dab25412ce3ed7edaac352b9517022fc3ba4c6f8d3ea239`
- 7.0.0 (mc 1.21.11): `1f2d9ce90de86bd094e6a901663c9390fceff9f57bbff05e16d34babfcf40fc8`

**Known malicious hash:**
- 7.16.1 (RAT): `547408592909b01781caf568307f415fe324530680dee4592fa8f93e71b37377`

If your hash does not match any of the above, play it safe and redownload from Modrinth.

## 8. What should I do if I might be affected?

1. If Minecraft is running, close it immediately.
2. Delete the SkyHanni mod from your mods folder.
3. Run a malware scan on your PC.
4. From another device if possible, change your Microsoft account password and sign out of all sessions.
5. Change your Discord password and sign out of all sessions.
6. Check authorized apps in your Discord settings and Microsoft account dashboard for unknown entries and revoke them.
7. Log out of all active sessions on critical services (email, banking) to invalidate potentially stolen session cookies.
8. Review saved passwords in your browser and change critical ones.
9. Redownload SkyHanni from Modrinth: <https://modrinth.com/mod/skyhanni>

## 9. Is the SkyHanni source code compromised?

No. The attacker only had access to workflow tokens, which allowed them to upload and replace release assets. They could not push to the repository, and branch protection held. The source code and commit history are clean. No maintainer account or password was compromised.

## 10. What have you done to prevent this from happening again?

**Immediate response:**
- Disabled all automated build and release processes for now
- Revoked all access keys that could have been stolen
- Removed all malicious releases
- Locked existing releases so they can no longer be modified or replaced
- Tightened permissions so automated processes can no longer upload or change files
- External contributions now require manual approval before any automated process runs
- Banned and reported the attacker's GitHub account

**Planned improvements:**
- Removing or rewriting the workflow that was exploited so it can never expose access keys again
- Rework how we handle auto updates in general

## 11. Where can I ask questions?

On Discord, please wait for the support channel to reopen. Until then, this FAQ should cover the common questions.

## 12. Where can I learn more about this type of attack?

This is a known class of supply chain attack targeting GitHub Actions workflows. Security researchers at Orca documented successful exploitation of the same vulnerability pattern against repositories owned by Microsoft, Google, and Nvidia. If you're curious about the technical details: <https://orca.security/resources/blog/pull-request-nightmare-part-2-exploits/>

</details>
