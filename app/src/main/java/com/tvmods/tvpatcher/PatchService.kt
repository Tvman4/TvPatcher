package com.tvmods.tvpatcher

import android.os.SystemClock
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class PatchService : IPatchService.Stub() {

    companion object {
        private const val NORMAL_PACKAGE =
            "com.AnotherAxiom.GorillaTag"

        private const val MODDED_PACKAGE =
            "com.TvMods.GorillaTag"

        private val NORMAL_OBB =
            File("/sdcard/Android/obb/$NORMAL_PACKAGE")

        private val MODDED_OBB =
            File("/sdcard/Android/obb/$MODDED_PACKAGE")

        private val NORMAL_CACHE =
            File("/sdcard/Android/data/$NORMAL_PACKAGE/cache")

        private val MODDED_CACHE =
            File("/sdcard/Android/data/$MODDED_PACKAGE/cache")
    }

    override fun copyObb(): String {

        return try {

            if (!NORMAL_OBB.exists()) {
                return "Normal Gorilla Tag OBB folder was not found."
            }

            val files = NORMAL_OBB.listFiles()
                ?.filter {
                    it.isFile &&
                        it.name.endsWith(
                            ".obb",
                            ignoreCase = true
                        )
                }
                ?: emptyList()

            if (files.isEmpty()) {
                return "No OBB files were found."
            }

            if (!MODDED_OBB.exists()) {
                if (!MODDED_OBB.mkdirs()) {
                    return "Could not create the modded OBB folder."
                }
            }

            var copied = 0

            for (source in files) {

                val destinationName =
                    source.name.replace(
                        NORMAL_PACKAGE,
                        MODDED_PACKAGE
                    )

                val destination =
                    File(
                        MODDED_OBB,
                        destinationName
                    )

                copyFile(
                    source,
                    destination
                )

                copied++
            }

            "✓ Copied $copied OBB file(s)."

        } catch (e: Throwable) {

            "OBB copy failed: ${
                e.message ?: e.javaClass.simpleName
            }"
        }
    }

    override fun restoreCache(): String {

        return try {

            if (!NORMAL_CACHE.exists()) {
                return "Normal Gorilla Tag cache was not found."
            }

            if (!MODDED_CACHE.exists()) {
                if (!MODDED_CACHE.mkdirs()) {
                    return "Could not create modded cache."
                }
            }

            copyDirectory(
                NORMAL_CACHE,
                MODDED_CACHE
            )

            "✓ Cache restored."

        } catch (e: Throwable) {

            "Cache restore failed: ${
                e.message ?: e.javaClass.simpleName
            }"
        }
    }

    private fun copyFile(
        source: File,
        destination: File
    ) {

        destination.parentFile?.mkdirs()

        val temporary =
            File(
                destination.parentFile,
                ".${destination.name}.tmp"
            )

        if (temporary.exists()) {
            temporary.delete()
        }

        FileInputStream(source).use { input ->

            FileOutputStream(temporary).use { output ->

                val buffer =
                    ByteArray(1024 * 1024)

                while (true) {

                    val read =
                        input.read(buffer)

                    if (read == -1) {
                        break
                    }

                    output.write(
                        buffer,
                        0,
                        read
                    )
                }

                output.flush()
            }
        }

        if (destination.exists()) {
            destination.delete()
        }

        if (!temporary.renameTo(destination)) {

            // Fallback if rename fails.
            FileInputStream(temporary).use { input ->

                FileOutputStream(destination).use { output ->

                    val buffer =
                        ByteArray(1024 * 1024)

                    while (true) {

                        val read =
                            input.read(buffer)

                        if (read == -1) {
                            break
                        }

                        output.write(
                            buffer,
                            0,
                            read
                        )
                    }

                    output.flush()
                }
            }

            temporary.delete()
        }
    }

    private fun copyDirectory(
        source: File,
        destination: File
    ) {

        if (!destination.exists()) {
            destination.mkdirs()
        }

        val children =
            source.listFiles()
                ?: return

        for (child in children) {

            val target =
                File(
                    destination,
                    child.name
                )

            if (child.isDirectory) {

                copyDirectory(
                    child,
                    target
                )

            } else {

                copyFile(
                    child,
                    target
                )
            }
        }
    }

    override fun destroy() {

        SystemClock.sleep(50)

        System.exit(0)
    }
}
