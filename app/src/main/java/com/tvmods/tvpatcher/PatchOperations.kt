package com.tvmods.tvpatcher

import java.io.File

object PatchOperations {

    private const val SOURCE_PACKAGE = "com.AnotherAxiom.GorillaTag"
    private const val TARGET_PACKAGE = "com.TvMods.GorillaTag"

    private val sourceObb =
        File("/sdcard/Android/obb/$SOURCE_PACKAGE")

    private val targetObb =
        File("/sdcard/Android/obb/$TARGET_PACKAGE")

    fun copyObb(): String {
        return try {
            if (!sourceObb.exists()) {
                return "Source OBB folder was not found."
            }

            val files = sourceObb.listFiles()
                ?.filter { it.isFile && it.extension.equals("obb", true) }
                ?: emptyList()

            if (files.isEmpty()) {
                return "No OBB files were found."
            }

            if (!targetObb.exists() && !targetObb.mkdirs()) {
                return "Could not create destination OBB folder."
            }

            var copied = 0

            for (source in files) {
                val destinationName =
                    source.name.replace(
                        SOURCE_PACKAGE,
                        TARGET_PACKAGE,
                        ignoreCase = false
                    )

                val destination = File(targetObb, destinationName)

                source.inputStream().use { input ->
                    destination.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                copied++
            }

            "✓ OBB copy complete\n$copied file(s) copied."

        } catch (e: Exception) {
            "OBB copy failed:\n${e.message ?: "Unknown error"}"
        }
    }

    fun restoreCache(): String {
        return try {
            val sourceCache =
                File("/sdcard/Android/data/$SOURCE_PACKAGE/cache")

            val targetCache =
                File("/sdcard/Android/data/$TARGET_PACKAGE/cache")

            if (!sourceCache.exists()) {
                return "Source cache was not found."
            }

            if (targetCache.exists()) {
                targetCache.deleteRecursively()
            }

            if (!targetCache.mkdirs()) {
                return "Could not create destination cache."
            }

            copyDirectory(sourceCache, targetCache)

            "✓ Cache restored successfully."

        } catch (e: Exception) {
            "Cache restore failed:\n${e.message ?: "Unknown error"}"
        }
    }

    private fun copyDirectory(
        source: File,
        destination: File
    ) {
        if (source.isDirectory) {
            if (!destination.exists()) {
                destination.mkdirs()
            }

            source.listFiles()?.forEach { child ->
                copyDirectory(
                    child,
                    File(destination, child.name)
                )
            }
        } else {
            source.inputStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}
