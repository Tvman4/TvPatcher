package com.tvmods.tvpatcher

import android.os.IBinder
import java.io.File

class PatchService : IPatchService.Stub() {

    companion object {
        private const val SOURCE_PACKAGE =
            "com.AnotherAxiom.GorillaTag"

        private const val TARGET_PACKAGE =
            "com.TvMods.GorillaTag"

        private const val OBB_ROOT =
            "/sdcard/Android/obb"

        private const val DATA_ROOT =
            "/sdcard/Android/data"
    }

    override fun isReady(): Boolean {
        return true
    }

    override fun copyObb(): String {
        return try {
            val source =
                File("$OBB_ROOT/$SOURCE_PACKAGE")

            val destination =
                File("$OBB_ROOT/$TARGET_PACKAGE")

            if (!source.exists()) {
                return "ERROR: Source OBB folder was not found."
            }

            val obbFiles = source.listFiles()
                ?.filter {
                    it.isFile &&
                    it.name.endsWith(".obb", ignoreCase = true)
                }
                ?: emptyList()

            if (obbFiles.isEmpty()) {
                return "ERROR: No OBB files found."
            }

            if (!destination.exists()) {
                if (!destination.mkdirs()) {
                    return "ERROR: Could not create target OBB folder."
                }
            }

            var copied = 0

            for (file in obbFiles) {

                val newName = file.name.replace(
                    SOURCE_PACKAGE,
                    TARGET_PACKAGE
                )

                val output =
                    File(destination, newName)

                copyFile(file, output)

                copied++
            }

            "SUCCESS: Copied $copied OBB file(s)."

        } catch (t: Throwable) {
            "ERROR: ${t.message ?: t.javaClass.simpleName}"
        }
    }

    override fun restoreCache(): String {
        return try {

            val source =
                File("$DATA_ROOT/$SOURCE_PACKAGE/cache")

            val destination =
                File("$DATA_ROOT/$TARGET_PACKAGE/cache")

            if (!source.exists()) {
                return "ERROR: Source cache was not found."
            }

            if (destination.exists()) {
                destination.deleteRecursively()
            }

            if (!destination.mkdirs()) {
                return "ERROR: Could not create target cache."
            }

            copyDirectory(
                source,
                destination
            )

            "SUCCESS: Cache restored."

        } catch (t: Throwable) {
            "ERROR: ${t.message ?: t.javaClass.simpleName}"
        }
    }

    private fun copyFile(
        source: File,
        destination: File
    ) {
        source.inputStream().use { input ->
            destination.outputStream().use { output ->
                val buffer = ByteArray(1024 * 1024)

                while (true) {
                    val count = input.read(buffer)

                    if (count <= 0) {
                        break
                    }

                    output.write(
                        buffer,
                        0,
                        count
                    )
                }

                output.flush()
            }
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

                val target =
                    File(destination, child.name)

                copyDirectory(
                    child,
                    target
                )
            }

        } else {
            copyFile(
                source,
                destination
            )
        }
    }

    override fun asBinder(): IBinder {
        return this
    }

    fun destroy() {
        System.exit(0)
    }
}
