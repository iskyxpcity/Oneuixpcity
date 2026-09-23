#!/usr/bin/env python3
"""Guard against accidentally re-enabling KernelSU JNI."""
from pathlib import Path
root = Path(__file__).resolve().parent
native_path = root / 'app/src/main/java/com/oneuixp/app/Natives.kt'
assert not native_path.exists(), 'obsolete KernelSU native compatibility layer must remain absent'
for dead in ('core/tasks', 'ui/webui', 'data/model', 'ui/screen/flash', 'ui/screen/modulerepo'):
    assert not (root / 'app/src/main/java/com/oneuixp/app' / dead).exists(), dead
gradle = (root / 'app/build.gradle.kts').read_text()
assert 'externalNativeBuild' not in gradle
assert (root / 'app/src/main/java/com/oneuixp/app/statusbar/SystemUiEntry.java').exists()
print('PASS: no KernelSU Natives shim/JNI sources/build; SystemUI hook retained')

# Legacy KernelSU boot/preload components must never return to the install manifest.
manifest = (root / 'app/src/main/AndroidManifest.xml').read_text()
for component in ('MagicaService', 'BootCompletedReceiver', 'AppZygotePreload', 'useAppZygote', 'zygotePreloadName'):
    assert component not in manifest, component
for name in ('AppZygotePreload.java', 'BootCompletedReceiver.java', 'MagicaService.java'):
    assert not (root / 'app/src/main/java/com/oneuixp/app/magica' / name).exists(), name
assert 'setSuEnabled' not in (root / 'app/src/main/java/com/oneuixp/app/data/repository/SettingsRepositoryImpl.kt').read_text()
print('PASS: legacy KernelSU boot and Zygote components removed; auto-jailbreak disabled')

# The project must remain independent of the removed KernelSU native source tree.
assert not (root / 'app/src/main/cpp').exists()
assert not (root / 'restore_native_headers.sh').exists()
workflow = (root / '.github/workflows/build.yml').read_text()
assert 'restore_native_headers' not in workflow
assert 'ndk;' not in workflow.lower()
print('PASS: obsolete KernelSU C++ sources and header restoration removed; CI does not restore them')

# Theme extraction guard: legacy KernelSU feature backend must stay disconnected.
settings_impl = (root / 'app/src/main/java/com/oneuixp/app/data/repository/SettingsRepositoryImpl.kt').read_text()
assert 'execKsud(' not in settings_impl
assert 'getFeatureStatus(' not in settings_impl
assert 'ShellUtils.fastCmd' not in settings_impl
reboot_popup = (root / 'app/src/main/java/com/oneuixp/app/ui/component/rebootlistpopup/RebootListPopup.kt').read_text()
assert 'reboot(reason)' not in reboot_popup
assert '"recovery"' not in reboot_popup and '"bootloader"' not in reboot_popup and '"download"' not in reboot_popup
assert 'statusbar_xp_systemui' in reboot_popup
theme = (root / 'app/src/main/java/com/oneuixp/app/ui/theme/Theme.kt').read_text()
assert 'fun OneuiXPTheme(' in theme
print('PASS: KernelSU feature/reboot paths disconnected; OneuiXP dual-theme wrapper active')

# Never bring back the abandoned KernelSU intent drain or unused adaptive side rails.
assert not (root / 'app/src/main/java/com/oneuixp/app/ui/navigation3/IntentDispatcher.kt').exists()
for name in ('NavigationRailMiuix.kt', 'NavigationRailMaterial.kt'):
    assert not (root / 'app/src/main/java/com/oneuixp/app/ui/component/bottombar' / name).exists(), name
assert 'fun SideRail(' not in (root / 'app/src/main/java/com/oneuixp/app/ui/component/bottombar/BottomBar.kt').read_text()
print('PASS: no old intent dispatcher or inactive adaptive navigation rails')
