# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x     | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

We take security seriously. **Do not open a public issue** for security
vulnerabilities. Instead, report privately to the maintainer via GitHub:

1. Open a **security advisory** at
   <https://github.com/malik-cat/myfinancialbook-android-app/security/advisories/new>,
   or
2. Email the maintainer directly and reference the affected version and
   repository name.

You should receive a response within **72 hours**. If the issue is
confirmed, a fix will be released as soon as possible, and the vulnerability
will be disclosed after the fix is available. Please do not disclose the
issue publicly until it has been resolved.

## Supply-Chain Security (SLSA)

This repository is hardened against software supply-chain attacks.

### Provenance

- Every tagged release (`v*`) is built by the
  [SLSA workflow](.github/workflows/slsa.yml) using the
  [SLSA GitHub Generator](https://github.com/slsa-framework/slsa-github-generator)
  generic generator.
- The generator emits a **cryptographically signed, non-forgeable** SLSA
  Build Level 3 in-toto attestation (`provenance.intoto.jsonl`) and attaches
  it to the GitHub release together with the signed APK artifact.
- To verify a release's provenance:

  ```shell
  go install github.com/slsa-framework/slsa-verifier/v2/cli/slsa-verifier@latest
  slsa-verifier verify-artifact app-release.apk \
    --provenance-path provenance.intoto.jsonl \
    --source-uri github.com/malik-cat/myfinancialbook-android-app \
    --source-versioned-tag v1.1
  ```

### Build requirements

To reach SLSA Build Level 3, builds must run on **GitHub-hosted runners**
(never self-hosted) and may only be triggered from trusted refs
(`v*` tags or `main`). A `google-services.json` file is required at build
time. Provide it as the **`GOOGLE_SERVICES_JSON` repository secret**; the
SLSA workflow writes it into `app/google-services.json` before building.

### Branch protection (recommended)

Enable the following rules on `main` in
**Settings > Branches > Branch protection rules**:

- Require pull request reviews (at least 1 reviewer, and `CODEOWNERS`).
- Require status checks to pass: `SLSA`, `Dependency review`,
  `Scorecard analysis`, and `Build`.
- Dismiss stale reviews, require approval of the most recent push.
- Require signed commits.
- Do not allow force pushes or deletions.
- Require linear history.

### Dependency hygiene

- [Dependabot](.github/dependabot.yml) opens weekly, low-risk dependency
  updates with grouped PRs for Compose and Firebase.
- The [Dependency Review](.github/workflows/dependency-review.yml) action
  blocks PRs that introduce known-vulnerable or incompatible dependencies.
- The [Scorecard](.github/workflows/scorecard.yml) workflow continuously
  evaluates the repository's security posture and uploads results to the
  Security > Code scanning tab.
