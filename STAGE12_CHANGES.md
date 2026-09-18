# Stage 12: settings update controls (partial)

- Miuix settings: removed the KernelSU module-update switch, preserved the app-update switch and theme controls.
- Material settings: removed the KernelSU module-update switch and its `KsuIsValid` wrapper, preserving app-update visibility and both theme controls.
- Did not change the actual app-update backend: it still needs inspection and migration to iSkyJIE/OneuiXP before release.
- Other KernelSU settings and native dependencies remain; this is not a build-ready release.
