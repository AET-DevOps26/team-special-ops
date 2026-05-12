# Branch protection setup (admin runbook)

Branch protection is a GitHub setting, not code. The repo admin (Owner role)
must configure it manually after this PR merges.

## Steps

1. Go to **Repo → Settings → Branches**.
2. Click **Add branch protection rule** (or edit the existing rule for `main`).
3. **Branch name pattern:** `main`
4. Tick **Require a pull request before merging**.
   - **Require approvals:** 1
   - **Dismiss stale pull request approvals when new commits are pushed:** ✓
5. Tick **Require status checks to pass before merging**.
   - **Require branches to be up to date before merging:** ✓
   - **Required status checks** (after the first CI run completes, these become
     selectable in the search box):
     - `spec-lint`
     - `java-services (user-progress)`
     - `java-services (catalog)`
     - `java-services (chat)`
     - `genai`
     - `web-client`
6. Tick **Do not allow bypassing the above settings**.
7. Click **Create / Save changes**.

## Verification

Open a throwaway PR that intentionally breaks the spec (e.g., delete a required
field in `api/openapi.yaml`). Confirm:

- `spec-lint` fails.
- The PR cannot be merged.

Revert the change and confirm the same PR becomes mergeable after CI is green.
