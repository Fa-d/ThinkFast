package dev.sadakat.thinkfaster.domain.model

enum class AppTarget(val packageName: String, val displayName: String) {
    FACEBOOK("com.facebook.katana", "Facebook"),
    INSTAGRAM("com.instagram.android", "Instagram");

    companion object {
        fun fromPackageName(packageName: String): AppTarget? {
            return entries.firstOrNull { it.packageName == packageName }
        }

        fun getAllPackageNames(): List<String> {
            return entries.map { it.packageName }
        }
    }
}
