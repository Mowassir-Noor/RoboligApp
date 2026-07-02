package com.robolig.controller.di

import com.robolig.controller.video.BoundaryAwareMjpegDecoder
import com.robolig.controller.video.MjpegDecoder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds
    abstract fun bindMjpegDecoder(boundaryAwareMjpegDecoder: BoundaryAwareMjpegDecoder): MjpegDecoder
}
