# Native runtime compatibility isolation
Legacy Natives no longer loads libkernelsu or declares JNI. It remains only as a compile-time compatibility shim for old screens pending deletion. Root writes fail closed (false/-1); read-only values are unsupported placeholders, not LSPosed metrics. LSPosed/SystemUI bridge is unchanged. Android build and phone testing have not been performed.
