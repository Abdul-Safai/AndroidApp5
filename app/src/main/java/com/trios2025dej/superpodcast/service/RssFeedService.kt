package com.trios2025dej.superpodcast.service

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.w3c.dom.Node
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Url
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.trios2025dej.superpodcast.util.DateUtils

class RssFeedService private constructor() {

    suspend fun getFeed(xmlFileURL: String): RssFeedResponse? {

        // ✅ No BuildConfig dependency (always safe)
        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(interceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("${xmlFileURL.split("?")[0]}/")
            .client(client)
            .build()

        val service = retrofit.create(FeedService::class.java)

        return try {
            val result = service.getFeed(xmlFileURL)
            if (!result.isSuccessful) return null

            val body = result.body() ?: return null
            val xmlString = body.string()
            if (xmlString.isBlank()) return null

            val dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val rss = RssFeedResponse(episodes = mutableListOf())

            withContext(Dispatchers.IO) {
                val doc = dBuilder.parse(xmlString.byteInputStream())
                domToRssFeedResponse(doc, rss)
            }

            rss
        } catch (t: Throwable) {
            null
        }
    }

    companion object {
        val instance: RssFeedService by lazy { RssFeedService() }
    }

    private fun domToRssFeedResponse(node: Node, rssFeedResponse: RssFeedResponse) {
        if (node.nodeType == Node.ELEMENT_NODE) {
            val nodeName = node.nodeName
            val parentName = node.parentNode?.nodeName ?: ""
            val grandParentName = node.parentNode?.parentNode?.nodeName ?: ""

            if (parentName == "item" && grandParentName == "channel") {
                val currentItem = rssFeedResponse.episodes?.lastOrNull()
                if (currentItem != null) {
                    when (nodeName) {
                        "title" -> currentItem.title = node.textContent
                        "description" -> currentItem.description = node.textContent
                        "itunes:duration" -> currentItem.duration = node.textContent
                        "guid" -> currentItem.guid = node.textContent
                        "pubDate" -> currentItem.pubDate = node.textContent
                        "link" -> currentItem.link = node.textContent
                        "enclosure" -> {
                            currentItem.url = node.attributes?.getNamedItem("url")?.textContent
                            currentItem.type = node.attributes?.getNamedItem("type")?.textContent
                        }
                    }
                }
            }

            if (parentName == "channel") {
                when (nodeName) {
                    "title" -> rssFeedResponse.title = node.textContent
                    "description" -> rssFeedResponse.description = node.textContent
                    "itunes:summary" -> rssFeedResponse.summary = node.textContent
                    "item" -> rssFeedResponse.episodes?.add(RssFeedResponse.EpisodeResponse())
                    "pubDate" -> rssFeedResponse.lastUpdated =
                        DateUtils.xmlDateToDate(node.textContent)
                }
            }
        }

        val nodeList = node.childNodes
        for (i in 0 until nodeList.length) {
            domToRssFeedResponse(nodeList.item(i), rssFeedResponse)
        }
    }
}

interface FeedService {
    @Headers(
        "Content-Type: application/xml; charset=utf-8",
        "Accept: application/xml"
    )
    @GET
    suspend fun getFeed(@Url xmlFileURL: String): Response<ResponseBody>
}
