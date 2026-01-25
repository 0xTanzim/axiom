# Axiom Deployment Guide

Complete guide for deploying docs to GitHub Pages and publishing to Maven Central.

---

## Part 1: GitHub Pages (Docs Site)

### URL Structure

After deployment, your docs will be available at:

```
https://0xtanzim.github.io/axiom/
https://0xtanzim.github.io/axiom/docs
https://0xtanzim.github.io/axiom/docs/getting-started
```

### Configuration (Already Set Up)

**1. Next.js Config (`docs/next.config.mjs`):**
```javascript
const isProd = process.env.NODE_ENV === 'production';

const config = {
  output: 'export',              // Static HTML export
  basePath: isProd ? '/axiom' : '', // /axiom prefix for GitHub Pages
  images: { unoptimized: true }, // Required for static export
};
```

**2. GitHub Actions (`.github/workflows/docs.yml`):**
- Triggers on push to `main` when `docs/**` changes
- Builds Next.js static export
- Deploys to GitHub Pages

### Enable GitHub Pages (One-Time Setup)

1. Go to: `https://github.com/0xtanzim/axiom/settings/pages`
2. Under "Build and deployment":
   - Source: **GitHub Actions** (not "Deploy from a branch")
3. Save

### Deploy Manually

Push changes to docs:
```bash
cd docs
# Make changes
git add .
git commit -m "docs: update documentation"
git push origin main
```

Or trigger manually:
1. Go to: Actions → Deploy Docs → Run workflow

### Verify Deployment

After ~2 minutes:
- Check: https://0xtanzim.github.io/axiom/
- Check Actions tab for build status

---

## Part 2: Maven Central Publishing

### Prerequisites Checklist

- [ ] Maven Central account at https://central.sonatype.com/
- [ ] Namespace `io.github.0xtanzim` claimed and verified
- [ ] GPG key generated for signing
- [ ] GitHub secrets configured

### Step 1: Create Maven Central Account

1. Go to https://central.sonatype.com/
2. Sign in with GitHub
3. Your namespace `io.github.0xtanzim` should be auto-verified

### Step 2: Generate Access Token

1. Go to https://central.sonatype.com/account
2. Click "Generate Token"
3. Save the **username** and **password** (token)

### Step 3: Generate GPG Key

```bash
# Generate GPG key (use your email)
gpg --full-generate-key
# Choose: RSA and RSA, 4096 bits, no expiration

# List keys to get your key ID
gpg --list-secret-keys --keyid-format LONG
# Output: sec   rsa4096/ABC123DEF456 2024-01-01 [SC]
#                       ↑ This is your KEY_ID

# Export public key (upload to keyserver)
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID

# Export private key for GitHub Actions
gpg --armor --export-secret-keys YOUR_KEY_ID > private.key
cat private.key  # Copy this entire content
```

### Step 4: Configure GitHub Secrets

Go to: `https://github.com/0xtanzim/axiom/settings/secrets/actions`

Add these secrets:

| Secret Name | Value |
|-------------|-------|
| `MAVEN_CENTRAL_USERNAME` | Token username from Step 2 |
| `MAVEN_CENTRAL_TOKEN` | Token password from Step 2 |
| `GPG_PRIVATE_KEY` | Full content of private.key |
| `GPG_PASSPHRASE` | Passphrase you set during GPG generation |

### Step 5: Version Management

**Current Structure:**
```
axiom-parent (0.1.1-SNAPSHOT)  ← Parent POM
├── axiom-core
├── axiom-persistence
├── axiom-persistence-processor
└── axiom-framework  ← THE ONE dependency users add
```

**All modules inherit the parent version automatically!**

When you run `mvn versions:set -DnewVersion=0.1.1`:
- Parent version changes to 0.1.1
- ALL child modules change to 0.1.1
- All internal dependencies update to 0.1.1

### Step 6: Release Process

**Option A: Automated Script (Recommended)**

```bash
# Updates all version references automatically
./scripts/update-version-references.sh 0.1.3

# Review changes
git diff

# Commit and tag
git add .
git commit -m "chore: release 0.1.3"
git tag -a v0.1.3 -m "Release v0.1.3"
git push origin main && git push origin v0.1.3
```

The script automatically updates:
- All pom.xml files (via `mvn versions:set`)
- README.md examples
- Documentation (docs/*.mdx)
- Archetype templates and examples

**Option B: Manual Process**

If you prefer manual control:

```bash
# 1. Update POMs
mvn versions:set -DnewVersion=0.1.3 -DgenerateBackupPoms=false

# 2. Update docs and examples manually
# - README.md
# - docs/content/docs/*.mdx
# - axiom-archetype/pom.xml (usage example)
# - axiom-archetype/src/main/resources/archetype-resources/pom.xml

# 3. Rebuild archetype
mvn clean install -pl axiom-archetype -DskipTests

# 4. Commit and tag
git add .
git commit -m "chore: release 0.1.3"
git tag -a v0.1.3 -m "Release v0.1.3"
git push origin main && git push origin v0.1.3
```

**Option C: Manual Trigger (GitHub Actions UI)**

1. Go to: Actions → Release → Run workflow
2. Enter version: `0.1.3`
3. Click "Run workflow"

The tag push triggers:
1. CI build and test
2. Deploy to Maven Central
3. Create GitHub Release

### Step 7: Verify on Maven Central

After release (~10-30 minutes):
- Check: https://central.sonatype.com/artifact/io.github.0xtanzim/axiom
- Search: https://search.maven.org/search?q=g:io.github.0xtanzim

Users can then add:
```xml
<dependency>
    <groupId>io.github.0xtanzim</groupId>
    <artifactId>axiom</artifactId>
    <version>0.1.1</version>
</dependency>
```

---

## Part 3: Version Bump Workflow

### After Each Release

```bash
# Bump to next development version
mvn versions:set -DnewVersion=0.2.0-SNAPSHOT -DgenerateBackupPoms=false
git add .
git commit -m "chore: bump version to 0.2.0-SNAPSHOT"
git push origin main
```

### Release Checklist

- [ ] All tests pass (`mvn clean verify`)
- [ ] Documentation updated
- [ ] CHANGELOG updated
- [ ] Version bumped from SNAPSHOT
- [ ] Tag created and pushed
- [ ] GitHub Release created
- [ ] Maven Central verified
- [ ] Bump to next SNAPSHOT version

---

## Part 4: CI Pipeline Summary

### Trigger Matrix

| Event | ci.yml | docs.yml | release.yml |
|-------|--------|----------|-------------|
| Push to main | ✅ | ✅ (if docs changed) | ❌ |
| Pull request | ✅ | ❌ | ❌ |
| Tag v* push | ✅ | ❌ | ✅ |
| Manual dispatch | ❌ | ✅ | ✅ |

### What Each Pipeline Does

**ci.yml:**
- Runs on every push/PR
- Builds with Java 25
- Runs all tests
- Uploads test results

**docs.yml:**
- Triggers when docs/ changes
- Builds Next.js static site
- Deploys to GitHub Pages

**release.yml:**
- Triggers on v* tag OR manual dispatch
- Sets version from tag/input
- Builds and tests
- Signs with GPG
- Deploys to Maven Central
- Creates GitHub Release

---

## Version Management

### The Version Mismatch Problem

**Problem:** `mvn versions:set` only updates `<version>` tags in POMs, NOT:
- Comments in POMs (like usage examples)
- Documentation files (README.md, *.mdx)
- Archetype templates
- Code examples in docs

**Impact:** After releasing 0.1.2, users might see:
- README says "use version 0.1.1"
- Archetype pom.xml shows `-DarchetypeVersion=0.1.1`
- Docs show dependency examples with 0.1.1
- But Maven Central only has 0.1.2!

**Solution:** Always use `./scripts/update-version-references.sh` to update ALL version references atomically.

### Version Update Checklist

Before releasing, ensure these files have correct version:

- [ ] All pom.xml `<version>` tags (automated by mvn versions:set)
- [ ] README.md dependency examples
- [ ] docs/content/docs/*.mdx (all documentation)
- [ ] axiom-archetype/pom.xml (usage example in description)
- [ ] axiom-archetype/.../archetype-resources/pom.xml (template)

Run: `grep -r "0\.1\.[0-9]" README.md docs/content/docs/*.mdx axiom-archetype/ | grep -v target | grep -v .flattened`

---

## Troubleshooting

### Docs Not Deploying

1. Check GitHub Pages is set to "GitHub Actions" source
2. Check Actions tab for errors
3. Verify `docs/out` is created during build

### Maven Central Errors

**"Missing sources/javadoc":**
- Already configured in parent POM ✅

**"GPG signature failed":**
- Check GPG_PRIVATE_KEY secret has full key including headers
- Check GPG_PASSPHRASE is correct

**"401 Unauthorized":**
- Regenerate Maven Central token
- Update MAVEN_CENTRAL_USERNAME and MAVEN_CENTRAL_TOKEN

### Version Mismatch

If child modules have wrong version:
```bash
mvn versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false
# This updates ALL modules at once
```

---

## Quick Commands

```bash
# Build locally
mvn clean verify

# Run tests only
mvn test

# Set release version (all modules)
mvn versions:set -DnewVersion=0.1.1 -DgenerateBackupPoms=false

# Set next SNAPSHOT version
mvn versions:set -DnewVersion=0.2.0-SNAPSHOT -DgenerateBackupPoms=false

# Build docs locally
cd docs && pnpm dev

# Build docs for production
cd docs && pnpm build
```
