package br.com.ticket.companion.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import br.com.ticket.companion.data.remote.TicketApiFactory
import br.com.ticket.companion.domain.connection.ConnectionManager
import br.com.ticket.companion.domain.parser.PixClassifier
import br.com.ticket.companion.util.SecureStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext context: Context): SharedPreferences {
        val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            context, "ticket_companion_secure_prefs", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Provides @Singleton fun provideSecureStorage(prefs: SharedPreferences) = SecureStorage(prefs)
    @Provides @Singleton fun provideTicketApiFactory() = TicketApiFactory()
    @Provides @Singleton fun provideConnectionManager(prefs: SharedPreferences, factory: TicketApiFactory) = ConnectionManager(prefs, factory)
    @Provides @Singleton fun providePixClassifier() = PixClassifier()
}
