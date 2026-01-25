#!/bin/bash
# Update version references across the entire repository
# Usage: ./scripts/update-version-references.sh 0.1.3

set -e

if [ -z "$1" ]; then
    echo "❌ Error: No version specified"
    echo "Usage: $0 <new-version>"
    echo "Example: $0 0.1.3"
    exit 1
fi

NEW_VERSION="$1"
OLD_VERSION=$(grep -oP '<version>\K[0-9]+\.[0-9]+\.[0-9]+' pom.xml | head -1)

if [ -z "$OLD_VERSION" ]; then
    echo "❌ Error: Could not detect current version from pom.xml"
    exit 1
fi

echo "🔄 Updating version references from $OLD_VERSION to $NEW_VERSION"
echo ""

# Function to update version in files
update_file() {
    local file=$1
    local pattern=$2
    local replacement=$3

    if [ -f "$file" ]; then
        if grep -q "$pattern" "$file" 2>/dev/null; then
            sed -i "s/$pattern/$replacement/g" "$file"
            echo "  ✓ $file"
        fi
    fi
}

# 1. Update Maven POMs (automated by mvn versions:set)
echo "📦 Updating Maven POM versions..."
mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false
echo ""

# 2. Update documentation files (replace ALL 0.x.x patterns)
echo "📚 Updating documentation..."
echo "  Searching for version patterns to update..."

# Find all Axiom version references (0.1.0, 0.1.1, 0.1.2, etc.)
PATTERN='0\.[0-9]+\.[0-9]+'

# Count occurrences before
BEFORE_COUNT=$(grep -r -E "$PATTERN" docs/content/docs/*.mdx README.md 2>/dev/null | wc -l)

# Update all documentation files
find docs/content/docs -name "*.mdx" -type f -exec sed -i -E "s/$PATTERN/$NEW_VERSION/g" {} \;
sed -i -E "s/$PATTERN/$NEW_VERSION/g" README.md

# Count after
AFTER_COUNT=$(grep -r -E "$PATTERN" docs/content/docs/*.mdx README.md 2>/dev/null | wc -l)

echo "  ✓ Updated $BEFORE_COUNT version references in docs"
echo "  ✓ README.md"
echo ""

# 3. Update archetype examples (replace ALL patterns)
echo "🏗️  Updating archetype usage examples..."
sed -i -E "s/DarchetypeVersion=$PATTERN/DarchetypeVersion=$NEW_VERSION/g" axiom-archetype/pom.xml
echo "  ✓ axiom-archetype/pom.xml"

sed -i -E "s/<axiom\.version>$PATTERN<\/axiom\.version>/<axiom.version>$NEW_VERSION<\/axiom.version>/g" axiom-archetype/src/main/resources/archetype-resources/pom.xml
echo "  ✓ axiom-archetype template"
echo ""

# 4. Rebuild archetype to update templates
echo "🔨 Rebuilding archetype..."
mvn clean install -pl axiom-archetype -DskipTests -q
echo "  ✓ Archetype rebuilt"
echo ""

# 5. Summary
echo "✅ Version update complete!"
echo ""
echo "New version: $NEW_VERSION"
echo ""

# Verification: Check for any remaining old version patterns
echo "🔍 Verification: Checking for remaining old versions..."
REMAINING=$(grep -r -E '0\.[0-9]+\.[0-9]+' README.md docs/content/docs/*.mdx axiom-archetype/pom.xml axiom-archetype/src/main/resources/archetype-resources/pom.xml 2>/dev/null | grep -v ".flattened-pom.xml" | grep -v "target/" | grep -v "$NEW_VERSION" | grep -v "DEPLOYMENT.md")

if [ -z "$REMAINING" ]; then
    echo "  ✅ All version references updated to $NEW_VERSION"
else
    echo "  ⚠️  WARNING: Found old version references:"
    echo "$REMAINING"
    echo ""
    echo "  Please review these files manually!"
fi
echo ""

echo "Files updated:"
echo "  • All pom.xml files"
echo "  • README.md"
echo "  • docs/content/docs/*.mdx"
echo "  • axiom-archetype templates"
echo ""
echo "Next steps:"
echo "  1. Review changes: git diff"
echo "  2. Commit: git add . && git commit -m \"chore: release $NEW_VERSION\""
echo "  3. Tag: git tag -a v$NEW_VERSION -m \"Release v$NEW_VERSION\""
echo "  4. Push: git push origin main && git push origin v$NEW_VERSION"
