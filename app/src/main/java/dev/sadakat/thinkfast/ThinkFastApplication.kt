package dev.sadakat.thinkfast

import android.app.Application
import dev.sadakat.thinkfast.di.databaseModule
import dev.sadakat.thinkfast.di.repositoryModule
import dev.sadakat.thinkfast.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class ThinkFastApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Koin
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@ThinkFastApplication)
            modules(
                databaseModule,
                repositoryModule,
                viewModelModule
            )
        }
    }
}
