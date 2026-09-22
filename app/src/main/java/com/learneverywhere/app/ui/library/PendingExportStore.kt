package com.learneverywhere.app.ui.library

import java.io.File
import java.io.InputStream
import java.util.UUID

/** Keep only this small file name in saved UI state while SAF is on screen. */
class PendingExportStore(private val directory: File) {
    fun prepare(bytes: ByteArray): String {
        if (!directory.isDirectory && !directory.mkdirs()) throw IllegalStateException("Could not create private export directory")
        val name = "pending-${UUID.randomUUID()}.json"
        val temporary = File.createTempFile("pending-", ".tmp", directory)
        try {
            temporary.outputStream().use { it.write(bytes) }
            if (!temporary.renameTo(File(directory, name))) throw IllegalStateException("Could not prepare export")
            return name
        } finally {
            temporary.delete()
        }
    }

    fun open(name: String): InputStream? = file(name).takeIf { it.isFile }?.inputStream()
    fun delete(name: String) { file(name).delete() }

    private fun file(name: String): File {
        require(name.matches(Regex("pending-[0-9a-f-]{36}\\.json")))
        return File(directory, name)
    }
}
