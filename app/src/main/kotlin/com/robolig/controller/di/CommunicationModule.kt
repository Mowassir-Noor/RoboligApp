package com.robolig.controller.di

import com.robolig.controller.communication.CommandQueue
import com.robolig.controller.protocol.BinaryPacketBuilder
import com.robolig.controller.protocol.BinaryPacketParser
import com.robolig.controller.protocol.Checksum
import com.robolig.controller.protocol.PacketBuilder
import com.robolig.controller.protocol.PacketDecoder
import com.robolig.controller.protocol.PacketParser
import com.robolig.controller.protocol.ProtocolPacketDecoder
import com.robolig.controller.protocol.XorChecksum
import com.robolig.controller.utils.MonotonicClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CommunicationModule {
    @Binds
    abstract fun bindChecksum(xorChecksum: XorChecksum): Checksum

    @Binds
    abstract fun bindPacketBuilder(binaryPacketBuilder: BinaryPacketBuilder): PacketBuilder

    @Binds
    abstract fun bindPacketParser(binaryPacketParser: BinaryPacketParser): PacketParser

    @Binds
    abstract fun bindPacketDecoder(protocolPacketDecoder: ProtocolPacketDecoder): PacketDecoder

    companion object {
        @Provides
        @Singleton
        fun provideCommandQueue(clock: MonotonicClock): CommandQueue = CommandQueue(clock)
    }
}
