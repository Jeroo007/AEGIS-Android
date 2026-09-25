import os
import re

BASE_DIR = r"d:\AEGIS\AEGIS-Android"

def replace_in_file(rel_path, pattern, replacement):
    path = os.path.join(BASE_DIR, rel_path)
    if not os.path.exists(path):
        print(f"File not found: {path}")
        return
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    new_content = re.sub(pattern, replacement, content, count=1, flags=re.DOTALL)
    with open(path, "w", encoding="utf-8") as f:
        f.write(new_content)

def append_to_file(rel_path, content):
    path = os.path.join(BASE_DIR, rel_path)
    if not os.path.exists(path):
        print(f"File not found: {path}")
        return
    with open(path, "a", encoding="utf-8") as f:
        f.write("\n" + content + "\n")

# 1. Update libs.versions.toml
toml_path = "gradle/libs.versions.toml"
with open(os.path.join(BASE_DIR, toml_path), "a", encoding="utf-8") as f:
    pass # we'll use a regex replacement

replace_in_file(toml_path, r"\[versions\]", "[versions]\ntflite = \"2.16.1\"\nonnx = \"1.18.0\"\n")
replace_in_file(toml_path, r"\[libraries\]", "[libraries]\ntensorflow-lite = { group = \"org.tensorflow\", name = \"tensorflow-lite\", version.ref = \"tflite\" }\ntensorflow-lite-support = { group = \"org.tensorflow\", name = \"tensorflow-lite-support\", version.ref = \"tflite\" }\ntensorflow-lite-gpu = { group = \"org.tensorflow\", name = \"tensorflow-lite-gpu\", version.ref = \"tflite\" }\nonnxruntime-android = { group = \"com.microsoft.onnxruntime\", name = \"onnxruntime-android\", version.ref = \"onnx\" }\n")

# 2. Update build.gradle.kts
gradle_path = "app/build.gradle.kts"
replace_in_file(gradle_path, r"android\s*\{", "android {\n    androidResources {\n        noCompress += listOf(\"tflite\", \"onnx\")\n    }\n")
replace_in_file(gradle_path, r"dependencies\s*\{", "dependencies {\n    implementation(libs.tensorflow.lite)\n    implementation(libs.tensorflow.lite.support)\n    implementation(libs.tensorflow.lite.gpu)\n    implementation(libs.onnxruntime.android)\n")

# 3. Modify AudioDistressAnalyzer
audio_analyzer_path = "app/src/main/java/com/aegis/safety/ai/audio/AudioDistressAnalyzer.kt"
replace_in_file(audio_analyzer_path, r"data class Result\(", "data class Result(\n        val engine: String = \"heuristic\",")

# 4. MultimodalFusionEngine
fusion_engine_path = "app/src/main/java/com/aegis/safety/ai/fusion/MultimodalFusionEngine.kt"
replace_in_file(fusion_engine_path, r"class MultimodalFusionEngine @Inject constructor\(\) \{", """class MultimodalFusionEngine @Inject constructor() {
    private val _config = kotlinx.coroutines.flow.MutableStateFlow(com.aegis.safety.ai.fusion.FusionConfig())
    val config: kotlinx.coroutines.flow.StateFlow<com.aegis.safety.ai.fusion.FusionConfig> = kotlinx.coroutines.flow.asStateFlow(_config)
    fun configure(newConfig: com.aegis.safety.ai.fusion.FusionConfig) {
        _config.value = newConfig
    }
    fun fuse(input: AiScore, context: com.aegis.safety.ai.fusion.ContextSnapshot = com.aegis.safety.ai.fusion.ContextSnapshot()): RiskLevel {
        if (input.explicitSos) return RiskLevel.CRITICAL
        return RiskLevel.LOW
    }
""")

# 5. AegisDatabase and AegisDao
db_path = "app/src/main/java/com/aegis/safety/data/local/AegisDatabase.kt"
replace_in_file(db_path, r"entities = \[(.*?)\], version = 1", "entities = [\\1, com.aegis.safety.core.database.entities.JourneyEntity::class], version = 2\n// TODO: add migration from v1 to v2\n")

dao_path = "app/src/main/java/com/aegis/safety/data/local/AegisDao.kt"
replace_in_file(dao_path, r"\}", """
    @androidx.room.Query("SELECT * FROM journeys WHERE id = :id")
    suspend fun getJourney(id: String): com.aegis.safety.core.database.entities.JourneyEntity?
}
""")

# 6. AegisApiService
api_path = "app/src/main/java/com/aegis/safety/data/remote/AegisApiService.kt"
replace_in_file(api_path, r"\}", """
    @POST("journeys")
    suspend fun startJourney(@Body body: com.aegis.safety.data.remote.dto.JourneyRequest): Response<Unit>
    @GET("journeys/active")
    suspend fun getActiveJourneys(): Response<Unit>
    @GET("journeys/{id}")
    suspend fun getJourney(@Path("id") id: String): Response<Unit>
    @POST("journeys/{id}/location")
    suspend fun updateLocation(@Path("id") id: String): Response<Unit>
    @POST("journeys/{id}/arrive")
    suspend fun arriveJourney(@Path("id") id: String): Response<Unit>
    @POST("journeys/{id}/cancel")
    suspend fun cancelJourney(@Path("id") id: String): Response<Unit>
}
""")

# 7. RepositoryModule
repo_module_path = "app/src/main/java/com/aegis/safety/di/RepositoryModule.kt"
replace_in_file(repo_module_path, r"\}", """
    @Binds
    @Singleton
    abstract fun bindJourneyRepository(
        journeyRepository: com.aegis.safety.data.repository.JourneyRepository
    ): com.aegis.safety.domain.repository.JourneyRepositoryInterface
}
""")

# 8. AiModule
ai_module_path = "app/src/main/java/com/aegis/safety/di/AiModule.kt"
replace_in_file(ai_module_path, r"\}", """
    @dagger.Provides
    @Singleton
    fun provideAiModelRegistry(): com.aegis.safety.ai.calibration.AiModelRegistry = com.aegis.safety.ai.calibration.AiModelRegistry()

    @dagger.Provides
    @Singleton
    fun provideTemperatureScaler(): com.aegis.safety.ai.calibration.TemperatureScaler = com.aegis.safety.ai.calibration.TemperatureScaler()
    
    @dagger.Binds
    @Singleton
    abstract fun bindFederatedLearningClient(
        client: com.aegis.safety.ai.federated.NoOpFederatedLearningClient
    ): com.aegis.safety.ai.federated.FederatedLearningClient
    
    @dagger.Binds
    @Singleton
    abstract fun bindWakeWordModel(
        model: com.aegis.safety.ai.voice.OnnxWakeWordModel
    ): com.aegis.safety.ai.voice.WakeWordModel
}
""")

# 9. AndroidManifest.xml
manifest_path = "app/src/main/AndroidManifest.xml"
replace_in_file(manifest_path, r"<application", """<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
    <application""")

replace_in_file(manifest_path, r"</application>", """
        <service android:name=".ai.voice.VoiceTriggerService" android:foregroundServiceType="microphone"/>
        <receiver android:name=".ai.voice.VoiceTriggerReceiver"/>
    </application>""")

print("Modifications done.")
