#!/usr/bin/env bash
# Renames the package from com.eyalm.adns to your new package across the repo.
# Run this from the repo root: bash rename_package.sh
set -euo pipefail

OLD_PKG="com.eyalm.adns"
NEW_PKG="com.kernelpanic.baladdns"   # <-- change this if you want a different package name

OLD_PATH=$(echo "$OLD_PKG" | tr '.' '/')
NEW_PATH=$(echo "$NEW_PKG" | tr '.' '/')

echo "Renaming $OLD_PKG -> $NEW_PKG"

# 1. Replace package references inside all source/config files
grep -rlI "$OLD_PKG" \
  --include="*.kt" --include="*.kts" --include="*.xml" \
  --include="*.pro" --include="*.aidl" --include="*.toml" . \
  | xargs sed -i.bak "s/${OLD_PKG//./\\.}/${NEW_PKG}/g"

# clean up .bak files sed leaves behind
find . -name "*.bak" -delete

# 2. Move Kotlin source directories to the new package path
for src_root in app/src/main/java app/src/test/java app/src/androidTest/java app/src/main/aidl; do
  old_dir="$src_root/$OLD_PATH"
  new_dir="$src_root/$NEW_PATH"
  if [ -d "$old_dir" ]; then
    mkdir -p "$(dirname "$new_dir")"
    git mv "$old_dir" "$new_dir"
  fi
done

echo "Done. Now:"
echo "  1. Re-sync Gradle in Android Studio"
echo "  2. Search remaining hits with: grep -rn \"$OLD_PKG\" . --include=*.kt --include=*.xml"
echo "  3. Rebuild: ./gradlew assembleDebug"
