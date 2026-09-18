# Stage 8 — Tools page replacement

- Replaced the active SuperUserPager with a title-only Tools page, retaining Miuix Scaffold/TopAppBar and the existing UI-mode switch.
- Removed its old Miuix and Material renderers, including search, top actions, app lists and Root-related page actions.
- Updated navigation labels to the new `oneuixp_tools` resource; added localized Chinese and English fallback labels.
- Left unrelated KernelSU pages and repositories intact pending a separate dependency-safe cleanup; this is NOT a complete KernelSU removal or verified build.
