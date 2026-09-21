package br.com.ticket.companion.di

import android.content.Context
import br.com.ticket.companion.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context) = AppDatabase.open(context)

    @Provides fun events(db: AppDatabase) = db.notificationDao()

    @Provides fun monitoredApps(db: AppDatabase) = db.monitoredAppDao()
}
