package com.example.vista.downloads

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Access for platform-owned entry points and integration verification of the same singleton. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface OfflineDownloadsAccess { fun offlineDownloads(): OfflineDownloads }
