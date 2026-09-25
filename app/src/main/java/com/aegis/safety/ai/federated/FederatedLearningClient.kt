package com.aegis.safety.ai.federated

import javax.inject.Inject
import javax.inject.Singleton

interface FederatedLearningClient {
    fun uploadWeights()
}

@Singleton
class NoOpFederatedLearningClient @Inject constructor() : FederatedLearningClient {
    override fun uploadWeights() {
        // Does nothing
    }
}
