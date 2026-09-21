package me.heartrace.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.heartrace.data.HeartraceDataSource
import me.heartrace.data.HeartraceRepository
import me.heartrace.data.ReminderScheduler
import me.heartrace.data.ReminderSchedulerPort
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindDataSource(repository: HeartraceRepository): HeartraceDataSource

    @Binds
    @Singleton
    abstract fun bindReminderScheduler(scheduler: ReminderScheduler): ReminderSchedulerPort
}
