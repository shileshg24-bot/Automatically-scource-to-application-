package com.example.geminiassistant

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Minimal client for Google's Gemini generateContent REST endpoint.
 * Uses BuildConfig.GEMINI_API_KEY which is loaded from local.properties
 * (never hardcode your key directly in source).
 */
class GeminiClient {

    private val client = OkHttpClient()
    private val model = "gemini-flash-latest"

    fun ask(prompt: String, callback: (String) -> Unit) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

        val json = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", prompt)
                ))
            ))
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-goog-api-key", BuildConfig.GEMINI_API_KEY)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback("Error: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val resBody = it.body?.string() ?: "{}"
                    try {
                        val obj = JSONObject(resBody)
                        val text = obj.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                        callback(text)
                    } catch (e: Exception) {
                        callback("Error parsing response: $resBody")
                    }
                }
            }
        })
    }
}
