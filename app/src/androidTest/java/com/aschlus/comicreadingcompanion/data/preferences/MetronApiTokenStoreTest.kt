package com.aschlus.comicreadingcompanion.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MetronApiTokenStoreTest {

    private lateinit var store: MetronApiTokenStore

    @Before
    fun setUp() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()

        store = MetronApiTokenStore(context)

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
    fun saveAndGet_returnsSavedToken() = runBlocking {
        store.save("  test-metron-token  ")

        assertEquals("test-metron-token", store.get())
    }

    @Test
    fun save_replacesPreviouslySavedToken() = runBlocking {
        store.save("first-token")
        store.save("second-token")

        assertEquals("second-token", store.get())
    }

    @Test
    fun clear_removesSavedToken() = runBlocking {
        store.save("test-metron-token")

        store.clear()

        assertNull(store.get())
    }

    @Test
    fun save_rejectsBlankToken() {
        org.junit.Assert.assertThrows(
            IllegalArgumentException::class.java
        ) {
            runBlocking {
                store.save("   ")
            }
        }
    }
}