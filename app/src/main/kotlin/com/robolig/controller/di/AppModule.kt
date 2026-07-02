package com.robolig.controller.di

import android.content.Context
import android.content.SharedPreferences
import com.robolig.controller.core.AndroidLogger
import com.robolig.controller.core.AppLogger
import com.robolig.controller.core.ApplicationScope
import com.robolig.controller.core.DefaultDispatcher
import com.robolig.controller.core.IoDispatcher
import com.robolig.controller.core.MainDispatcher
import com.robolig.controller.core.PreferenceConstants
import com.robolig.controller.utils.AndroidMonotonicClock
import com.robolig.controller.utils.ControllerPreferences
import com.robolig.controller.utils.ControllerPreferencesImpl
import com.robolig.controller.utils.MonotonicClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    abstract fun bindAppLogger(androidLogger: AndroidLogger): AppLogger

    @Binds
    abstract fun bindMonotonicClock(androidMonotonicClock: AndroidMonotonicClock): MonotonicClock

    @Binds
    abstract fun bindControllerPreferences(controllerPreferencesImpl: ControllerPreferencesImpl): ControllerPreferences

    companion object {
        @Provides
        @Singleton
        fun provideSharedPreferences(
            @ApplicationContext context: Context,
        ): SharedPreferences =
            context.getSharedPreferences(
                PreferenceConstants.CONTROLLER_PREFERENCES,
                Context.MODE_PRIVATE,
            )

        @Provides
        @DefaultDispatcher
        fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

        @Provides
        @IoDispatcher
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

        @Provides
        @MainDispatcher
        fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    }
}
