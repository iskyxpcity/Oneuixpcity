#!/usr/bin/env python3
"""Guard against accidentally re-enabling KernelSU JNI."""
from pathlib import Path
root = Path(__file__).resolve().parent
native = (root / 'app/src/main/java/me/weishu/kernelsu/Natives.kt').read_text()
gradle = (root / 'app/build.gradle.kts').read_text()
assert 'System.loadLibrary' not in native
assert 'external fun ' not in native and 'external get' not in native
assert 'externalNativeBuild' not in gradle
assert 'fun setAppProfile(profile: Profile?): Boolean = false' in native
assert 'fun setSuEnabled(enabled: Boolean): Boolean = false' in native
assert (root / 'app/src/main/java/me/weishu/kernelsu/statusbar/SystemUiEntry.java').exists()
print('PASS: no KernelSU JNI loading/declarations/build; unsupported writes fail closed; SystemUI hook retained')

# Legacy KernelSU boot/preload components must never return to the install manifest.
manifest = (root / 'app/src/main/AndroidManifest.xml').read_text()
for component in ('MagicaService', 'BootCompletedReceiver', 'AppZygotePreload', 'useAppZygote', 'zygotePreloadName'):
    assert component not in manifest, component
for name in ('AppZygotePreload.java', 'BootCompletedReceiver.java', 'MagicaService.java'):
    assert not (root / 'app/src/main/java/me/weishu/kernelsu/magica' / name).exists(), name
assert 'get() = false' in (root / 'app/src/main/java/me/weishu/kernelsu/data/repository/SettingsRepositoryImpl.kt').read_text()
print('PASS: legacy KernelSU boot and Zygote components removed; auto-jailbreak disabled')

# The project must remain independent of the removed KernelSU native source tree.
assert not (root / 'app/src/main/cpp').exists()
assert not (root / 'restore_native_headers.sh').exists()
workflow = (root / '.github/workflows/build.yml').read_text()
assert 'restore_native_headers' not in workflow
assert 'ndk;' not in workflow.lower()
print('PASS: obsolete KernelSU C++ sources and header restoration removed; CI does not restore them')

# Theme extraction guard: legacy KernelSU feature backend must stay disconnected.
settings_impl = (root / 'app/src/main/java/me/weishu/kernelsu/data/repository/SettingsRepositoryImpl.kt').read_text()
assert 'execKsud(' not in settings_impl
assert 'getFeatureStatus(' not in settings_impl
assert 'ShellUtils.fastCmd' not in settings_impl
reboot_popup = (root / 'app/src/main/java/me/weishu/kernelsu/ui/component/rebootlistpopup/RebootListPopup.kt').read_text()
assert 'reboot(reason)' not in reboot_popup
assert '"recovery"' not in reboot_popup and '"bootloader"' not in reboot_popup and '"download"' not in reboot_popup
assert 'statusbar_xp_systemui' in reboot_popup
theme = (root / 'app/src/main/java/me/weishu/kernelsu/ui/theme/Theme.kt').read_text()
assert 'fun OneuiXPTheme(' in theme
print('PASS: KernelSU feature/reboot paths disconnected; OneuiXP dual-theme wrapper active')
