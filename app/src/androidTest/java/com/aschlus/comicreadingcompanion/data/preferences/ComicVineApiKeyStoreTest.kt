package com.aschlus.comicreadingcompanion.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComicVineApiKeyStoreTest {

    private lateinit var context: Context
    private lateinit var store: ComicVineApiKeyStore

    @Before
    fun setUp() {
        context =
            ApplicationProvider.getApplicationContext()

        store = ComicVineApiKeyStore(context)

        runBlocking {
            store.clear()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            store.clear()
        }
    }

    @Test
    fun saveAndGet_returnsSavedKey() = runBlocking {
        store.save("  test-comic-vine-key  ")

        assertEquals(
            "test-comic-vine-key",
            store.get()
        )
    }

    @Test
    fun save_replacesPreviouslySavedKey() = runBlocking {
        store.save("first-test-key")
        store.save("second-test-key")

        assertEquals(
            "second-test-key",
            store.get()
        )
    }

    @Test
    fun clear_removesSavedKey() = runBlocking {
        store.save("test-comic-vine-key")

        store.clear()

        assertNull(store.get())
    }

    @Test
    fun save_rejectsBlankKey() {
        org.junit.Assert.assertThrows(
            IllegalArgumentException::class.java
        ) {
            runBlocking {
                store.save("   ")
            }
        }
    }
}