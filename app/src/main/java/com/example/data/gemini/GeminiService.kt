package com.example.data.gemini

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

object GeminiService {
    private const val TAG = "GeminiService"

    suspend fun generateResponse(
        prompt: String,
        systemInstruction: String? = null,
        isJsonMode: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is missing or is the default placeholder!")
            return@withContext "Error: API Key is not configured. Please add Gemini API key securely in the Secrets panel."
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = if (isJsonMode) {
                GenerationConfig(
                    temperature = 0.2f,
                    responseMimeType = "application/json"
                )
            } else {
                GenerationConfig(temperature = 0.7f)
            },
            systemInstruction = systemInstruction?.let {
                Content(parts = listOf(Part(text = it)))
            }
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val generatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!generatedText.isNullOrBlank()) {
                generatedText
            } else {
                "Error: Empty response received from Gemini."
            }
        } catch (e: Exception) {
            Log.e(TAG, "API Call failed", e)
            "Error: ${e.message}"
        }
    }

    suspend fun generateVideoAutoSummary(
        context: android.content.Context,
        videoTitle: String
    ): String {
        // 1. Check Cache first
        val cached = VideoSummaryCache.getSummary(context, videoTitle)
        if (cached != null) return cached

        // 2. Query Gemini
        val systemInstruction = "You are an intelligent, hyper-concise video metadata model. In one short, single sentence (maximum 7 words), summarize what a video with this filename represents. Start with a relevant emoji. Do NOT mention details like 'based on the filename' or 'This looks like'. Keep it creative and direct!"
        val prompt = "Filename: \"$videoTitle\""
        
        val response = generateResponse(prompt = prompt, systemInstruction = systemInstruction)
        val cleanResponse = response.trim()
        
        // 3. Save to cache if successful
        if (cleanResponse.isNotEmpty() && !cleanResponse.startsWith("Error")) {
            VideoSummaryCache.saveSummary(context, videoTitle, cleanResponse)
            return cleanResponse
        }
        return "🎬 Ready to stream"
    }
}

object VideoSummaryCache {
    private const val PREFS_NAME = "video_summaries_cache_prefs"
    private val memoryCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun getSummary(context: android.content.Context, videoTitle: String): String? {
        val cachedInMemory = memoryCache[videoTitle]
        if (cachedInMemory != null) return cachedInMemory

        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val cachedInPrefs = prefs.getString(videoTitle, null)
        if (cachedInPrefs != null) {
            memoryCache[videoTitle] = cachedInPrefs
        }
        return cachedInPrefs
    }

    fun saveSummary(context: android.content.Context, videoTitle: String, summary: String) {
        memoryCache[videoTitle] = summary
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putString(videoTitle, summary).apply()
    }
}

data class GeminiSubtitle(
    val id: String = java.util.UUID.randomUUID().toString(),
    var startMs: Long,
    var endMs: Long,
    var text: String
)

object GeminiSubtitleParser {
    fun parseSubtitlesJson(jsonStr: String): List<GeminiSubtitle> {
        val list = mutableListOf<GeminiSubtitle>()
        try {
            var cleanJson = jsonStr.trim()
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substringAfter("```json")
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substringAfter("```")
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substringBeforeLast("```")
            }
            cleanJson = cleanJson.trim()

            val jsonArray = org.json.JSONArray(cleanJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val startMs = obj.optLong("startMs", 0L)
                val endMs = obj.optLong("endMs", 0L)
                val text = obj.optString("text", "")
                if (text.isNotEmpty()) {
                    list.add(GeminiSubtitle(startMs = startMs, endMs = endMs, text = text))
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiSubtitleParser", "JSON Parsing of subtitles failed", e)
        }
        return list
    }
}

