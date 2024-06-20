package org.jetbrains.kotlinx.spark.api.jupyter

interface Properties : MutableMap<String, String?> {

    companion object {
        internal const val sparkPropertiesName = "sparkProperties"

        internal const val sparkMasterName = "spark.master"
        internal const val appNameName = "spark.app.name"
        internal const val sparkName = "spark"
        internal const val scalaName = "scala"
        internal const val versionName = "v"
        internal const val displayLimitName = "displayLimit"
        internal const val displayTruncateName = "displayTruncate"
        internal const val remoteName = "remote"
        internal const val debugName = "debug"
    }

    /**
     * The value which limits the number of rows while displaying an RDD or Dataset.
     * Default: 20
     */
    var displayLimit: Int
        set(value) { this[displayLimitName] = value.toString() }
        get() = this[displayLimitName]?.toIntOrNull() ?: 20

    /**
     * The value which limits the number characters per cell while displaying an RDD or Dataset.
     * `-1` for no limit.
     * Default: 30
     */
    var displayTruncate: Int
        set(value) { this[displayTruncateName] = value.toString() }
        get() = this[displayTruncateName]?.toIntOrNull() ?: 30

    var debug: Boolean
        set(value) { this[debugName] = value.toString() }
        get() = this[debugName]?.toBoolean() ?: false

    operator fun invoke(block: Properties.() -> Unit): Properties = apply(block)
}
