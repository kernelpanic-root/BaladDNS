#!/usr/bin/env bash
set -euo pipefail

# 1. Fix attribution string
sed -i.bak 's/Created by Eyal Meirom/Created by Kernelpanic/' app/src/main/res/values/strings.xml
rm -f app/src/main/res/values/strings.xml.bak

# 2. Remove Hebrew locale entirely
rm -rf app/src/main/res/values-iw

# 3. Point GitHub links at your fork instead of upstream
FILES=(
  "app/src/main/java/com/kernelpanic/baladdns/ui/screens/settings/MainSettingsScreen.kt"
  "app/src/main/java/com/kernelpanic/baladdns/ui/screens/HomeScreen.kt"
  "app/src/main/java/com/kernelpanic/baladdns/data/nextdns/setup/SetupGuideCatalog.kt"
  "app/src/main/java/com/kernelpanic/baladdns/viewmodel/MainViewModel.kt"
)
for f in "${FILES[@]}"; do
  sed -i.bak 's#github.com/eyalm2000/adns#github.com/kernelpanic-root/BaladDNS#g' "$f"
  rm -f "$f.bak"
done

# 4. Fix the update-check API endpoint (different URL shape, missed by step 3)
VM="app/src/main/java/com/kernelpanic/baladdns/viewmodel/MainViewModel.kt"
sed -i.bak 's#api.github.com/repos/eyalm2000/adns#api.github.com/repos/kernelpanic-root/BaladDNS#g' "$VM"
rm -f "$VM.bak"

echo "Done. Verify with:"
echo "  grep -rn 'eyalm2000\\|Eyal Meirom' --include=*.kt --include=*.xml ."
