package com.gatchii.plugins

import com.ucasoft.ktor.simpleCache.SimpleCache
import com.ucasoft.ktor.simpleRedisCache.redisCache
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.conditionalheaders.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.routing.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureHTTP() {
  install(CachingHeaders) {
    options { call, outgoingContent ->
      when (outgoingContent.contentType?.withoutParameters()) {
        ContentType.Text.CSS -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 24 * 60 * 60))
        else -> null
      }
    }
  }
  install(Compression)
  install(ConditionalHeaders)
  install(CORS) {
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowMethod(HttpMethod.Patch)
    allowHeader(HttpHeaders.Authorization)
    allowHeader("MyCustomHeader")
    //anyHost() // Don't do this in production if possible. Try to limit it.
  }
  install(ForwardedHeaders) // WARNING: for security, do not include this if not behind a reverse proxy
  install(XForwardedHeaders) // WARNING: for security, do not include this if not behind a reverse proxy
  install(DefaultHeaders) {
    header("X-Engine", "Ktor") // will send this header with each response
  }
  install(SimpleCache) {
    redisCache {
      invalidateAt = 10.seconds
      host = "localhost"
      port = 6379
    }
  }
  routing {
  }
}
