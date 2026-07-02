package com.robolig.controller.communication

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunicationStateStore
    @Inject
    constructor() {
        private val mutableState = MutableStateFlow(CommunicationState())

        val state: StateFlow<CommunicationState> = mutableState.asStateFlow()

        fun update(transform: (CommunicationState) -> CommunicationState) {
            mutableState.update(transform)
        }
    }
