# Publishing Guide

**Version:** 0.1.0-draft
**Last Updated:** 2026-01-12

---

## Overview

This guide covers publishing Axiom to Maven Central,
including setup, configuration, and release process.

---

## Prerequisites

### 1. Maven Central Account

1. Create account at https://central.sonatype.com/
2. Complete namespace verification
3. Generate user token for API access

### 2. Namespace Ownership

**Option A: Domain-based (Preferred)**
- Register domain (e.g., `axiom.io`)
- Add DNS TXT record for verification
- Namespace: `io.axiom`

**Option B: GitHub-based**
- Use GitHub organization
- Namespace: `io.github.{org}`
- Automatic verification via GitHub

### 3. GPG Key Setup

```bash
# Generate key (RSA 4096)
gpg --full-generate-key

# Choose:
# - RSA and RSA
# - 4096 bits
# - Key does not expire
# - Real name: Axiom Release
# - Email: release@axiom.io (or your email)
# - Comment: Maven Central Signing Key

# List keys
gpg --list-keys

# Export public key to keyserver
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
gpg --keyserver keys.openpgp.org --send-keys <KEY_ID>

# Export private key (for CI)
gpg --export-secret-keys --armor <KEY_ID> > private.key
```

---

## Project Configuration

### Gradle (build.gradle.kts)

```kotlin
plugins {
    `java-library`
    `maven-publish`
    signing
}

group = "io.axiom"
version = "0.1.0-SNAPSHOT"

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("Axiom Core")
                description.set("DX-first Java web framework")
                url.set("https://github.com/axiom-framework/axiom")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("axiom")
                        name.set("Axiom Team")
                        email.set("team@axiom.io")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/axiom-framework/axiom.git")
                    developerConnection.set("scm:git:ssh://github.com/axiom-framework/axiom.git")
                    url.set("https://github.com/axiom-framework/axiom")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            val releasesUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsUrl else releasesUrl)

            credentials {
                username = findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
                password = findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    val signingKey = findProperty("signingKey") as String? ?: System.getenv("GPG_PRIVATE_KEY")
    val signingPassword = findProperty("signingPassword") as String? ?: System.getenv("GPG_PASSPHRASE")
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["mavenJava"])
}
```

### Maven (pom.xml)

```xml
<project>
    <groupId>io.axiom</groupId>
    <artifactId>axiom-core</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>Axiom Core</name>
    <description>DX-first Java web framework</description>
    <url>https://github.com/axiom-framework/axiom</url>

    <licenses>
        <license>
            <name>Apache License 2.0</name>
            <url>https://www.apache.org/licenses/LICENSE-2.0</url>
        </license>
    </licenses>

    <developers>
        <developer>
            <id>axiom</id>
            <name>Axiom Team</name>
            <email>team@axiom.io</email>
        </developer>
    </developers>

    <scm>
        <connection>scm:git:git://github.com/axiom-framework/axiom.git</connection>
        <developerConnection>scm:git:ssh://github.com/axiom-framework/axiom.git</developerConnection>
        <url>https://github.com/axiom-framework/axiom</url>
    </scm>

    <distributionManagement>
        <snapshotRepository>
            <id>ossrh</id>
            <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
        </snapshotRepository>
        <repository>
            <id>ossrh</id>
            <url>https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/</url>
        </repository>
    </distributionManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-source-plugin</artifactId>
                <version>3.3.0</version>
                <executions>
                    <execution>
                        <id>attach-sources</id>
                        <goals><goal>jar-no-fork</goal></goals>
                    </execution>
                </executions>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-javadoc-plugin</artifactId>
                <version>3.6.0</version>
                <executions>
                    <execution>
                        <id>attach-javadocs</id>
                        <goals><goal>jar</goal></goals>
                    </execution>
                </executions>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-gpg-plugin</artifactId>
                <version>3.1.0</version>
                <executions>
                    <execution>
                        <id>sign-artifacts</id>
                        <phase>verify</phase>
                        <goals><goal>sign</goal></goals>
                    </execution>
                </executions>
            </plugin>

            <plugin>
                <groupId>org.sonatype.central</groupId>
                <artifactId>central-publishing-maven-plugin</artifactId>
                <version>0.4.0</version>
                <extensions>true</extensions>
                <configuration>
                    <publishingServerId>central</publishingServerId>
                    <tokenAuth>true</tokenAuth>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## GitHub Actions CI/CD

### `.github/workflows/ci.yml`

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        java: [21, 23, 25]

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK ${{ matrix.java }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
          distribution: 'temurin'

      - name: Build with Gradle
        run: ./gradlew build

      - name: Run tests
        run: ./gradlew test

      - name: Upload test results
        uses: actions/upload-artifact@v4
        if: failure()
        with:
          name: test-results-${{ matrix.java }}
          path: '**/build/reports/tests/'
```

### `.github/workflows/release.yml`

```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: 'temurin'

      - name: Validate tag version
        run: |
          TAG_VERSION=${GITHUB_REF#refs/tags/v}
          GRADLE_VERSION=$(./gradlew properties -q | grep "version:" | awk '{print $2}')
          if [ "$TAG_VERSION" != "$GRADLE_VERSION" ]; then
            echo "Tag version ($TAG_VERSION) does not match Gradle version ($GRADLE_VERSION)"
            exit 1
          fi

      - name: Build
        run: ./gradlew build

      - name: Publish to Maven Central
        run: ./gradlew publish
        env:
          OSSRH_USERNAME: ${{ secrets.OSSRH_USERNAME }}
          OSSRH_PASSWORD: ${{ secrets.OSSRH_PASSWORD }}
          GPG_PRIVATE_KEY: ${{ secrets.GPG_PRIVATE_KEY }}
          GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          generate_release_notes: true
          files: |
            axiom-core/build/libs/*.jar
            axiom-http/build/libs/*.jar
```

---

## Release Process

### 1. Pre-Release Checklist

- [ ] All tests passing on all Java versions
- [ ] Version updated (remove `-SNAPSHOT`)
- [ ] CHANGELOG.md updated
- [ ] Documentation updated
- [ ] No SNAPSHOT dependencies

### 2. Version Update

```bash
# In build.gradle.kts, change:
version = "0.1.0-SNAPSHOT"
# To:
version = "0.1.0"
```

### 3. Create Release Tag

```bash
# Commit version change
git add .
git commit -m "Release 0.1.0"

# Create tag
git tag -a v0.1.0 -m "Release 0.1.0"

# Push
git push origin main --tags
```

### 4. Verify Release

1. Check GitHub Actions for successful build
2. Log into https://central.sonatype.com/
3. Verify staging repository
4. If valid, release/promote
5. Wait for sync to Maven Central (~30 min)

### 5. Post-Release

```bash
# Update to next SNAPSHOT
# In build.gradle.kts:
version = "0.2.0-SNAPSHOT"

git add .
git commit -m "Prepare for next development cycle"
git push
```

---

## Module Publishing Order

Due to dependencies, publish in this order:

1. `axiom-bom` (BOM first)
2. `axiom-core` (no deps)
3. `axiom-http` (depends on core)
4. `axiom-runtime-jdk` (depends on http)
5. `axiom-json-jackson` (depends on http)
6. `axiom-json-gson` (depends on http)
7. `axiom-test` (depends on core)
8. Other runtimes

---

## Versioning Policy

### Semantic Versioning

```
MAJOR.MINOR.PATCH

MAJOR: Breaking changes
MINOR: New features (backward compatible)
PATCH: Bug fixes (backward compatible)
```

### Pre-1.0 Rules

- `0.x.y`: API can change
- Breaking changes increment MINOR
- Bug fixes increment PATCH

### Post-1.0 Rules

- `1.0.0+`: Stable API
- Breaking changes require MAJOR bump
- Deprecation before removal

---

## Artifact Checklist

Each release must include:

- [ ] `axiom-core-X.Y.Z.jar` - Main artifact
- [ ] `axiom-core-X.Y.Z-sources.jar` - Source code
- [ ] `axiom-core-X.Y.Z-javadoc.jar` - Documentation
- [ ] `axiom-core-X.Y.Z.pom` - POM file
- [ ] `axiom-core-X.Y.Z.jar.asc` - GPG signature
- [ ] All checksums (MD5, SHA1)

Repeat for each module.

---

## Troubleshooting

### GPG Signing Fails

```bash
# Check key is available
gpg --list-keys

# Test signing
echo "test" | gpg --clearsign

# If passphrase issues in CI, use pinentry-mode loopback
gpg --pinentry-mode loopback --sign
```

### Upload Fails

1. Check credentials are correct
2. Verify namespace ownership
3. Check for duplicate artifacts
4. Review Sonatype error messages

### Sync to Maven Central Delayed

- Snapshots: Available immediately
- Releases: 30 min to 2 hours
- Check https://search.maven.org/

---

## Useful Links

- [Maven Central Portal](https://central.sonatype.com/)
- [Publishing Guide](https://central.sonatype.org/publish/publish-guide/)
- [GPG Setup](https://central.sonatype.org/publish/requirements/gpg/)
- [Gradle Plugin](https://github.com/vanniktech/gradle-maven-publish-plugin)
- [GitHub Actions](https://docs.github.com/en/actions)

---

*Follow this guide to ensure consistent, secure releases.*
