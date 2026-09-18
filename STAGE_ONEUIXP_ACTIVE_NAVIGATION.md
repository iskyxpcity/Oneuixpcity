# OneuiXP active navigation isolation

This change operates on the existing OneuiXP source, retaining the verified LSPosed/SystemUI entry, status bridge and boot report code unchanged.

The active Navigation3 destination registry now contains only the four-tab main screen, its compatibility tab destinations, About and ColorPalette. KernelSU log, app-profile, template, repository-detail and flash destinations are no longer registered; the legacy Route declarations remain temporarily for source compilation until their unused callers can be removed as a group. This is not an APK build or a device test.
