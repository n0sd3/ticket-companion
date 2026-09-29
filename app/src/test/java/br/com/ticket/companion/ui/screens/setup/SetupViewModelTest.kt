package br.com.ticket.companion.ui.screens.setup

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.ticket.companion.data.local.AppDatabase
import br.com.ticket.companion.data.remote.TicketApiFactory
import br.com.ticket.companion.domain.connection.ConnectionManager
import br.com.ticket.companion.service.CompanionComponents
import br.com.ticket.companion.util.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class SetupViewModelTest {
    @Test fun `double tapping confirm binds once and never crashes the process`() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        val uncaught = mutableListOf<Throwable>()
        Thread.setDefaultUncaughtExceptionHandler { _, error -> uncaught += error }
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("setup-vm-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val manager = ConnectionManager(prefs, TicketApiFactory(allowHttp = true))
            MockWebServer().use { server ->
                server.enqueue(MockResponse().setBody("""{"valid":true,"deviceId":"${manager.deviceId}","tokenName":"Caixa","company":{"name":"Loja","slug":"loja"}}"""))
                val viewModel = SetupViewModel(context, manager, database.notificationDao(), SecureStorage(prefs))
                viewModel.updateServerUrl(server.url("/").toString())
                viewModel.updateToken("token-a")
                viewModel.verify().join()
                assertEquals("Loja", viewModel.uiState.value.companyName)

                viewModel.confirm(false).join()
                viewModel.confirm(false).join()

                assertNotNull(manager.active)
                assertEquals("loja", manager.active!!.companySlug)
                assertEquals(0, manager.retiredConnections.size)
                assertTrue(viewModel.isConfigured.value)
                assertTrue("o listener deve ligar ao vincular a empresa", CompanionComponents.isListenerEnabled(context))
                assertTrue("exceção não tratada: $uncaught", uncaught.isEmpty())
            }
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previousHandler)
            database.close()
            Dispatchers.resetMain()
        }
    }

    @Test fun `web only mode saves a canonical https origin and rejects anything else`() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("setup-vm-web-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val storage = SecureStorage(prefs)
            val viewModel = SetupViewModel(context, ConnectionManager(prefs, TicketApiFactory(allowHttp = false)), database.notificationDao(), storage)

            viewModel.updateServerUrl("  https://Loja.Crm.Exemplo.com/x/ ")
            assertEquals("https://loja.crm.exemplo.com", viewModel.openWebOnly())
            assertEquals("https://loja.crm.exemplo.com", storage.webUrl)
            assertEquals("https://loja.crm.exemplo.com", viewModel.webOrigin())
            assertFalse("modo só web não liga o listener", CompanionComponents.isListenerEnabled(context))

            viewModel.updateServerUrl("http://loja.crm.exemplo.com")
            assertNull(viewModel.openWebOnly())
            assertNotNull(viewModel.uiState.value.error)
            assertEquals("endereço salvo não pode ser trocado por um inválido", "https://loja.crm.exemplo.com", storage.webUrl)
        } finally {
            database.close()
            Dispatchers.resetMain()
        }
    }
}
