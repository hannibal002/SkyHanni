<!-- remove all unused parts 

The title of your PR should be descriptive and concise.
It should be in the format of `Type: Description`. For example, `Feature: Add new command` or `Fix: Bug in command`.
If there are multiple types of changes these can be separated by a plus.
For example, `Feature + Fix: Add new command and fix bug in command`.
Commonly used labels are Improvement, Backend, Feature and Fix.

## PR Reviews

When your PR is marked as ready for review, some of our maintainers will look through your code to make sure everything is good to go.
In order to do this, they may request some changes you will need to do, **or fix smaller stuff (like merge conflicts) for you**.
If a maintainer has reviewed your PR, make sure to **pull any of their changes** into your local project before doing more work on your code.
Having maintainers fix small stuff for you helps us speed up the process of merging your PR,
so if some of your systems warrant further care, be sure to let us know (preferably with a code comment).

Make sure to only mark your PR as "Ready to review" when it is.
If you still want to do major changes, you can keep a draft PR open until then.

## Keyword Labels

Some labels and checks are controlled by keywords in this description.

Write `waiting_on_hypixel_alpha` if the feature your PR relies on is currently only available on the Hypixel alpha server.
This adds the "Waiting on Hypixel" label and blocks the PR from being merged until you remove the line again.

Write `exclude_from_changelog` if this PR has no changelog entries at all, for example when reverting a pull request,
or when fixing a pull request that was merged but not yet released in a beta. Explain the reason in the What section instead.

A keyword line has to match exactly, on its own line. Leading or trailing spaces, list markers such as `- `,
and a different capitalization all prevent it from being recognized. See CONTRIBUTING.md for details.
-->

## Dependencies
- pr_number_or_link_here

## What
Describe what this pull request does, including technical details, screenshots, links to discord, etc.

<details>
<summary>Images</summary>

<!-- drop images here -->

</details>

## Changelog New Features
+ Added Cool new feature. - your_name_here
    * Optional extra info.

## Changelog Improvements
+ Improved cool feature. - your_name_here
    * Optional extra info.

## Changelog Fixes
+ Fixed cool feature. - your_name_here
    * Optional extra info.

## Changelog Removed Features
+ Removed cool feature. - your_name_here
    * Optional extra info.

## Changelog Technical Details
+ Something technical you changed in the backend. - your_name_here
    * Optional extra info.
