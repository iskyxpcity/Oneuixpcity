from pathlib import Path

p = Path('hook/src/main/java/io/github/soclear/oneuix/hook/systemui/StatusBarPowerRuntime.kt')
s = p.read_text()

old = '''        runCatching {
            val clazz = param.classLoader.loadClass(
                "com.android.systemui.statusbar.phone.PhoneStatusBarView"
            )
            clazz.declaredConstructors.forEach { constructor ->
                module.hook(constructor).intercept { chain ->
                    val result = chain.proceed()
                    (chain.thisObject as? View)?.let { statusBarView ->
                        statusBarView.post {
                            registerStatusBarView(statusBarView)
                            ensurePreferenceWatcher(module, statusBarView.context)
                        }
                    }
                    result
                }
            }
        }
'''
new = '''        runCatching {
            val clazz = param.classLoader.loadClass(
                "com.android.systemui.statusbar.phone.PhoneStatusBarView"
            )
            clazz.declaredConstructors.forEach { constructor ->
                module.hook(constructor).intercept { chain ->
                    val result = chain.proceed()
                    (chain.thisObject as? View)?.let { statusBarView ->
                        statusBarView.post {
                            registerStatusBarView(statusBarView)
                            ensurePreferenceWatcher(module, statusBarView.context)
                        }
                    }
                    result
                }
            }
        }

        // Follow Samsung's real dark/light icon transition instead of keeping the
        // color captured when the power view was first configured.
        runCatching {
            val clockClass = param.classLoader.loadClass(
                "com.android.systemui.statusbar.policy.QSClockIndicatorView"
            )
            clockClass.declaredMethods
                .filter { it.name == "onDarkChanged" }
                .forEach { method ->
                    module.hook(method).intercept { chain ->
                        val result = chain.proceed()
                        (chain.thisObject as? TextView)?.let { clock ->
                            val color = clock.currentTextColor
                            mainHandler.post { applyIndicatorTextColor(color) }
                        }
                        result
                    }
                }
        }
'''
if old not in s:
    raise SystemExit('install block not found')
s = s.replace(old, new, 1)

old = '''    private fun renderSample(
        sample: Sample,
        current: Preference.SystemUI.StatusBarPowerRuntime,
    ) {
        val active = synchronized(holders) {
            holders.values.filter { it.statusBarView.isAttachedToWindow }
        }
        active.forEach { holder ->
'''
new = '''    private fun renderSample(
        sample: Sample,
        current: Preference.SystemUI.StatusBarPowerRuntime,
    ) {
        val active = synchronized(holders) {
            holders.values.filter { it.statusBarView.isAttachedToWindow }
        }
        active.forEach { holder ->
            // Fallback color sync for transitions that do not dispatch onDarkChanged.
            val currentColor = resolveIndicatorTextColor(holder.statusBarView)
            holder.slots.forEach { textView ->
                if (textView.currentTextColor != currentColor) {
                    textView.setTextColor(currentColor)
                }
            }
'''
if old not in s:
    raise SystemExit('renderSample block not found')
s = s.replace(old, new, 1)

marker = '''    private fun resolveIndicatorTextColor(statusBarView: View): Int {
'''
insert = '''    private fun applyIndicatorTextColor(color: Int) {
        val active = synchronized(holders) {
            holders.values.filter { it.statusBarView.isAttachedToWindow }
        }
        active.forEach { holder ->
            holder.slots.forEach { textView ->
                if (textView.currentTextColor != color) {
                    textView.setTextColor(color)
                }
            }
        }
    }

'''
if marker not in s:
    raise SystemExit('resolve color marker not found')
s = s.replace(marker, insert + marker, 1)

p.write_text(s)
