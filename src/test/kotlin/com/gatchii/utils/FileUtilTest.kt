package com.gatchii.utils

import org.junit.jupiter.api.Test
import shared.common.UnitTest
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertEquals

@UnitTest
class FileUtilTest {

    @Test
    fun `test that file is created with correct content`() {
        // given
        val path = "test-files/testFile.txt"
        val content = "Test content"

        // when
        FileUtil.writeFile(path, content)

        // then
        val createdFile = File(path)
        assertTrue(createdFile.exists(), "File should be created")
        assertEquals(content, createdFile.readText(), "File content should match the expected content")

        // Cleanup
        createdFile.delete()
        createdFile.parentFile?.delete()
    }

    @Test
    fun `test that content is overwritten when writing to existing file`() {
        // given
        val path = "test-files/existingFile.txt"
        val initialContent = "Initial content"
        val newContent = "New content"
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(initialContent)

        // when
        FileUtil.writeFile(path, newContent)

        // then
        assertTrue(file.exists(), "File should exist")
        assertEquals(newContent, file.readText(), "File content should reflect the new content")

        // Cleanup
        file.delete()
        file.parentFile?.delete()
    }

    @Test
    fun `test that directories are created if they do not exist`() {
        // given
        val path = "test-files/new-directory/testFile.txt"
        val content = "Test content"
        val file = File(path)

        // when
        FileUtil.writeFile(path, content)

        // then
        assertTrue(file.exists(), "File should be created")
        assertTrue(file.parentFile.exists(), "Parent directory should be created")
        assertEquals(content, file.readText(), "File content should match the expected content")

        // Cleanup
        file.delete()
        file.parentFile?.delete()
        file.parentFile?.parentFile?.delete()
    }
}