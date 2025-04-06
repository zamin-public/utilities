package link.zamin.base.util


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.gson.Gson
import link.zamin.base.exceptions.ThirdPartyServerException
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.HttpClientBuilder
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.logging.Logger
import javax.net.ssl.*

object HttpService {

    // todo: i don't know what is it and what is the correct way to get certs but this worked for me and solved PKIX error in api calls!
    fun trustAllCerts(): SSLContext {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509ExtendedTrustManager() {
                override fun getAcceptedIssuers(): Array<X509Certificate>? {
                    return null
                }

                override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
                override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {}

                @Throws(CertificateException::class)
                override fun checkClientTrusted(xcs: Array<X509Certificate?>?, string: String?, socket: Socket?) {
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(xcs: Array<X509Certificate?>?, string: String?, socket: Socket?) {
                }

                @Throws(CertificateException::class)
                override fun checkClientTrusted(xcs: Array<X509Certificate?>?, string: String?, ssle: SSLEngine?) {
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(xcs: Array<X509Certificate?>?, string: String?, ssle: SSLEngine?) {
                }
            }
        )

        val sc: SSLContext = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)

        // Create all-trusting host name verifier

        // Create all-trusting host name verifier
        val allHostsValid = HostnameVerifier { hostname, session -> true }
        // Install the all-trusting host verifier
        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
        return sc
    }

    fun headRequestSync(url: URL): HttpsURLConnection {
        val http: HttpsURLConnection = url.openConnection() as HttpsURLConnection
        http.requestMethod = "HEAD"
        http.disconnect()
        return http
    }

    fun <T> getRequestSync(
        url: String,
        queries: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        responseClass: Class<T>,
        readTimeOut: Int = 10000
    ): T {
        val urlWithQuery = StringBuilder(url)
        if (queries.isNotEmpty()) {
            urlWithQuery.append("?")
            queries.forEach {
                urlWithQuery.append("${it.key}=${it.value}")
                urlWithQuery.append("&")
            }
        }
        try {
            val connection = if (url.contains("https"))
                (URL(urlWithQuery.toString()).openConnection() as HttpsURLConnection)
            else
                (URL(urlWithQuery.toString()).openConnection() as HttpURLConnection)
            with((connection).apply {
                headers.forEach {
                    setRequestProperty(it.key, it.value)
                }
                setRequestProperty(
                    "Content-Type",
                    "application/json"
                )
                requestMethod = "GET"
                readTimeout = readTimeOut
            }) {
                when (responseCode) {
                    in listOf(200, 201) -> {
                        inputStream.bufferedReader().use { bufferReader ->
                            bufferReader.lines().toArray().toList().joinToString("").let { line ->
                                return Gson().fromJson(line, responseClass)
                            }
                        }
                    }

                    else -> {
                        Logger.getLogger(this.javaClass.name).severe(
                            "$url GET exception: response error $responseCode, msg: ${
                                errorStream.bufferedReader().lines().toArray().toList().joinToString("")
                            }"
                        )
                        throw ThirdPartyServerException(
                            "$url GET exception: response error $responseCode, msg: ${
                                errorStream.bufferedReader().lines().toArray().toList().joinToString("")
                            }"
                        )
                    }
                }
            }
        } catch (ex: Exception) {
            if (ex is ThirdPartyServerException)
                throw ex
            Logger.getLogger(this.javaClass.name).severe("$url GET exception: ${ex.message}")
            throw ThirdPartyServerException("$url GET exception: ${ex.message}")
        }
    }

    fun <T> postRequestSync(
        url: String,
        headers: Map<String, String> = emptyMap(),
        requestBody: List<String> = emptyList(),
        responseClass: Class<T>,
        readTimeOut: Int = 3000
    ): T? {
        try {
            val connection = if (url.contains("https"))
                (URL(url).openConnection() as HttpsURLConnection)
            else
                (URL(url).openConnection() as HttpURLConnection)

            with(connection.apply {
                headers.forEach {
                    setRequestProperty(it.key, it.value)
                }
                setRequestProperty(
                    "Content-Type",
                    "application/json"
                )
                requestMethod = "POST"
                doOutput = true
                readTimeout = readTimeOut

                // Create ObjectMapper and register Kotlin module
                val objectMapper = ObjectMapper().registerModule(KotlinModule())

                // Serialize to JSON
                val out = objectMapper.writeValueAsString(requestBody)

                DataOutputStream(outputStream).use { wr -> wr.write(out.toString().toByteArray()) }
            }) {
                when (responseCode) {
                    in listOf(200, 201) -> {
                        inputStream.bufferedReader().use { bufferReader ->
                            bufferReader.lines().findFirst().get().let { line ->
                                return try {
                                    Gson().fromJson(line, responseClass)
                                } catch (ex: Exception) {
                                    Logger.getLogger(this.javaClass.name)
                                        .warning("$url POST exception: can not read response: $line")
                                    null
                                }
                            }
                        }
                    }

                    else -> {
                        Logger.getLogger(this.javaClass.name)
                            .severe(
                                "$url POST exception: response error $responseCode, msg: ${
                                    errorStream.bufferedReader().lines().toArray().toList().joinToString("")
                                }"
                            )
                        throw ThirdPartyServerException(
                            "$url POST exception: response error $responseCode, msg: ${
                                errorStream.bufferedReader().lines().toArray().toList().joinToString("")
                            }"
                        )
                    }
                }
            }
        } catch (ex: Exception) {
            if (ex is ThirdPartyServerException)
                throw ex
            Logger.getLogger(this.javaClass.name).severe("$url POST exception: ${ex.message}")
            throw ThirdPartyServerException("$url POST exception: ${ex.message}")
        }
    }

    fun <T> deleteRequestSync(
        url: String,
        headers: Map<String, String> = emptyMap(),
        requestBody: List<String> = emptyList(),
        responseClass: Class<T>,
        readTimeOut: Int = 3000
    ): T? {
        try {
            val connection = if (url.contains("https"))
                (URL(url).openConnection() as HttpsURLConnection)
            else
                (URL(url).openConnection() as HttpURLConnection)

            with(connection.apply {
                headers.forEach {
                    setRequestProperty(it.key, it.value)
                }
                setRequestProperty(
                    "Content-Type",
                    "application/json"
                )
                requestMethod = "DELETE"
                doOutput = true
                readTimeout = readTimeOut

                // Create ObjectMapper and register Kotlin module
                val objectMapper = ObjectMapper().registerModule(KotlinModule())

                // Serialize to JSON
                val out = objectMapper.writeValueAsString(requestBody)

                DataOutputStream(outputStream).use { wr -> wr.write(out.toString().toByteArray()) }
            }) {
                when (responseCode) {
                    in listOf(200, 201) -> {
                        inputStream.bufferedReader().use { bufferReader ->
                            bufferReader.lines().findFirst().get().let { line ->
                                return try {
                                    Gson().fromJson(line, responseClass)
                                } catch (ex: Exception) {
                                    Logger.getLogger(this.javaClass.name)
                                        .warning("$url DELETE exception: can not read response: $line")
                                    null
                                }
                            }
                        }
                    }

                    else -> {
                        Logger.getLogger(this.javaClass.name)
                            .severe(
                                "$url DELETE exception: response error $responseCode, msg: ${
                                    errorStream.bufferedReader().lines().toArray().toList().joinToString("")
                                }"
                            )
                        throw ThirdPartyServerException(
                            "$url DELETE exception: response error $responseCode, msg: ${
                                errorStream.bufferedReader().lines().toArray().toList().joinToString("")
                            }"
                        )
                    }
                }
            }
        } catch (ex: Exception) {
            if (ex is ThirdPartyServerException)
                throw ex
            Logger.getLogger(this.javaClass.name).severe("$url DELETE exception: ${ex.message}")
            throw ThirdPartyServerException("$url DELETE exception: ${ex.message}")
        }
    }

    fun <T> postRequestSync(
        url: String,
        headers: Map<String, String> = emptyMap(),
        requestBody: Any,
        responseClass: Class<T>,
        readTimeOut: Int = 3000
    ): T? {
        try {
            val connection = if (url.contains("https"))
                (URL(url).openConnection() as HttpsURLConnection)
            else
                (URL(url).openConnection() as HttpURLConnection)

            with(connection.apply {
                headers.forEach {
                    setRequestProperty(it.key, it.value)
                }
                setRequestProperty(
                    "Content-Type",
                    "application/json"
                )
                requestMethod = "POST"
                doOutput = true
                readTimeout = readTimeOut

                // todo: refactor all POST, PUT methods with this lines
                // Create ObjectMapper and register Kotlin module
                val objectMapper = ObjectMapper().registerModule(KotlinModule())

                // Serialize to JSON
                val out = objectMapper.writeValueAsString(requestBody)

                DataOutputStream(outputStream).use { wr -> wr.write(out.toByteArray()) }
            }) {
                when (responseCode) {
                    in listOf(200, 201) -> {
                        inputStream.bufferedReader().use { bufferReader ->
                            bufferReader.lines().findFirst().get().let { line ->
                                return try {
                                    Gson().fromJson(line, responseClass)
                                } catch (ex: Exception) {
                                    Logger.getLogger(this.javaClass.name)
                                        .warning("$url POST exception: can not read response: $line")
                                    null
                                }
                            }
                        }
                    }

                    else -> {
                        Logger.getLogger(this.javaClass.name)
                            .severe(
                                "$url POST exception: response error $responseCode, msg: ${
                                    errorStream.bufferedReader().lines().toArray().toList().joinToString("")
                                }"
                            )
                        throw ThirdPartyServerException(
                            "$url POST exception: response error $responseCode, msg: ${
                                errorStream.bufferedReader().lines().toArray().toList().joinToString("")
                            }"
                        )
                    }
                }
            }
        } catch (ex: Exception) {
            if (ex is ThirdPartyServerException)
                throw ex
            Logger.getLogger(this.javaClass.name).severe("$url POST exception: ${ex.message}")
            throw ThirdPartyServerException("$url POST exception: ${ex.message}")
        }
    }

    fun multipartSingleFileRequest(
        url: URL,
        authorization: String? = null,
        queries: Map<String, String>? = null,
        file: File,
        fieldName: String
    ): CloseableHttpResponse? {
        try {
            val urlWithQuery = StringBuilder(url.toString())
            queries?.let {
                urlWithQuery.append("?")
                queries.forEach {
                    urlWithQuery.append("${it.key}=${it.value}")
                    urlWithQuery.append("&")
                }
            }
            val post = HttpPost(urlWithQuery.toString())
            post.addHeader(
                "Authorization", authorization
            )
            val builder = MultipartEntityBuilder.create()
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
            builder.addBinaryBody(fieldName, file, ContentType.DEFAULT_BINARY, file.name)
            val entity: HttpEntity = builder.build()
            post.entity = entity
            val client = HttpClientBuilder.create().build();
            return client.execute(post)
        } catch (ex: Exception) {
            Logger.getLogger(this.javaClass.name).severe("error in sending file: ${ex.message}")
            return null
        }
    }
}
