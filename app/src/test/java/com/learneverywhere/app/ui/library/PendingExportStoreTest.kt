package com.learneverywhere.app.ui.library

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PendingExportStoreTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun `prepared export survives a new store instance and cleans up after picker`() {
        val directory = folder.newFolder()
        val data = """{"schemaVersion":1,"dictionaries":[]}""".toByteArray()
        val name = PendingExportStore(directory).prepare(data)
        val restored = PendingExportStore(directory)
        assertArrayEquals(data, restored.open(name)!!.use { it.readBytes() })
        restored.delete(name)
        assertNull(restored.open(name))
    }
}
