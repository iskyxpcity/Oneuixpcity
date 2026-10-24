from pathlib import Path
import subprocess

ROOT = Path(".")
def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")
def write(rel, value):
    p = ROOT / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(value, encoding="utf-8")
def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one anchor, found {count}")
    return text.replace(old, new, 1)

# Restore the original Theme Center layout from the supplied archive.
source_ui = ROOT / "extracted/app/src/main/java/com/alohaoneui/mod/ui/category/DetailPaneThemeCenter.kt"
assert source_ui.is_file(), "Source DetailPaneThemeCenter.kt missing from archive"
write("app/src/main/java/com/alohaoneui/mod/ui/category/DetailPaneThemeCenter.kt",
      source_ui.read_text(encoding="utf-8"))

# Remove the redundant standalone category from the feature catalog.
p = "app/src/main/java/com/alohaoneui/app/alohaoneui/AlohaOneUIFeatureCatalog.kt"
s = read(p)
s = replace_once(s, '    ThemeStoreService(Package.THEME_STORE, "主题商店服务"),\n', '', "remove redundant Theme Store category")
s = replace_once(
    s,
    '''        AlohaOneUICategory.ThemeStoreService to toggles(
            "Theme Store Service",
            "other.setThemeStoreRegion",
        ),
''',
    '',
    "remove redundant Theme Store category metadata",
)
write(p, s)

# Remove the extra page routing and use the existing Theme Center card/page.
p = "app/src/main/java/com/alohaoneui/app/ui/screen/tools/AlohaOneUICategoryScreen.kt"
s = read(p)
s = replace_once(s, "import com.alohaoneui.mod.ui.category.DetailPaneThemeStoreService\n", "", "remove redundant page import")
s = replace_once(s, "import com.alohaoneui.mod.ui.category.onThemeStoreServiceEvent\n", "", "remove redundant page event import")
s = replace_once(
    s,
    "            AlohaOneUICategory.ThemeStoreService -> DetailPaneThemeStoreService(preference.other, toolsViewModel::onThemeStoreServiceEvent, modifier)\n",
    "",
    "remove redundant page route",
)
write(p, s)

# Remove the special category-title override introduced only for the extra card.
p = "app/src/main/java/com/alohaoneui/app/ui/screen/tools/AlohaOneUIUiResources.kt"
s = read(p)
s = replace_once(
    s,
    '''        if (category == AlohaOneUICategory.ThemeStoreService) {
            return@remember resolveString(context, listOf("alohaoneui_category_themestoreservice"))
                ?: category.fallbackTitle
        }
''',
    "",
    "remove redundant category title override",
)
write(p, s)

# Remove only the redundant category label; keep the feature's setting strings.
for p in ["app/src/main/res/values/alohaoneui_strings.xml",
          "app/src/main/res/values-zh-rCN/alohaoneui_strings.xml"]:
    s = read(p)
    lines = s.splitlines(keepends=True)
    matches = [line for line in lines if 'name="alohaoneui_category_themestoreservice"' in line]
    if len(matches) != 1:
        raise RuntimeError(f"{p}: expected one redundant category string, found {len(matches)}")
    write(p, "".join(line for line in lines if 'name="alohaoneui_category_themestoreservice"' not in line))

# Delete the custom page so no orphan implementation remains.
Path("app/src/main/java/com/alohaoneui/mod/ui/category/DetailPaneThemeStoreService.kt").unlink()

# Preserve all pre-existing module actions from main, adding only the needed action.
subprocess.run(["git", "fetch", "origin", "main", "--depth=1"], check=True)
p = "common/src/main/java/com/alohaoneui/common/ModuleActions.kt"
s = subprocess.check_output(["git", "show", "origin/main:" + p], text=True)
anchor = "object ModuleActions {"
if s.count(anchor) != 1:
    raise RuntimeError("Could not locate original ModuleActions object")
if "THEME_STORE_REGION_CHANGED" not in s:
    s = s.replace(anchor, anchor + '\n    const val THEME_STORE_REGION_CHANGED = "com.alohaoneui.action.THEME_STORE_REGION_CHANGED"', 1)
    # Keep Kotlin formatting: each const property on its own line.
    s = s.replace('THEME_STORE_REGION_CHANGED = "com.alohaoneui.action.THEME_STORE_REGION_CHANGED"\n', 'THEME_STORE_REGION_CHANGED = "com.alohaoneui.action.THEME_STORE_REGION_CHANGED"\n', 1)
write(p, s)

# Add the hooked app to the Xposed scope list without disturbing existing scopes.
p = "app/src/main/resources/META-INF/xposed/scope.list"
s = read(p)
if "com.samsung.android.themestore" not in s.splitlines():
    write(p, s.rstrip() + "\ncom.samsung.android.themestore\n")

# Verify that the existing card is the only UI entry and that both theme adapters
# plus the complete region selector are present in the copied source layout.
catalog = read("app/src/main/java/com/alohaoneui/app/alohaoneui/AlohaOneUIFeatureCatalog.kt")
screen = read("app/src/main/java/com/alohaoneui/app/ui/screen/tools/AlohaOneUICategoryScreen.kt")
ui = read("app/src/main/java/com/alohaoneui/mod/ui/category/DetailPaneThemeCenter.kt")
main = read("hook/src/main/java/com/alohaoneui/hook/Main.kt")
actions = read("common/src/main/java/com/alohaoneui/common/ModuleActions.kt")
assert "ThemeStoreService" not in catalog
assert "DetailPaneThemeStoreService" not in screen
assert "ThemeCenter(Package.THEME_CENTER" in catalog
assert "ThemeStoreRegionSelector(" in ui and "ThemeStoreRegionChip(" in ui
assert "UiMode.Material" in ui and "UiMode.Miuix" in ui
assert "ThemeCenterEvent.SelectThemeStoreRegion" in ui
assert "ThemeStoreRegion.hook(preference.other)" in main
assert "THEME_STORE_REGION_CHANGED" in actions
assert "com.samsung.android.themestore" in read("app/src/main/resources/META-INF/xposed/scope.list")
print("Correction passed: Theme Store region controls are merged into the existing Theme Center page; redundant card/page removed.")
