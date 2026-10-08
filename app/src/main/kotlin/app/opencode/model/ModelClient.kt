package app.opencode.model

import kotlinx.coroutines.flow.Flow

interface ModelClient {
    val providerId: String
    fun stream(request: ChatRequest): Flow<StreamEvent>
}
