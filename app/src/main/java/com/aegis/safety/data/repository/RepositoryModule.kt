package com.aegis.safety.data.repository

import com.aegis.safety.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindAuth(impl: AuthRepository): AuthRepositoryInterface

    @Binds @Singleton
    abstract fun bindContacts(impl: EmergencyContactRepository): EmergencyContactRepositoryInterface

    @Binds @Singleton
    abstract fun bindIncidents(impl: IncidentRepository): IncidentRepositoryInterface

    @Binds @Singleton
    abstract fun bindLocations(impl: LocationRepository): LocationRepositoryInterface

    @Binds @Singleton
    abstract fun bindSafeZones(impl: SafeZoneRepository): SafeZoneRepositoryInterface

    @Binds @Singleton
    abstract fun bindNotifications(impl: NotificationRepository): NotificationRepositoryInterface

    @Binds @Singleton
    abstract fun bindChat(impl: ChatRepository): ChatRepositoryInterface
}