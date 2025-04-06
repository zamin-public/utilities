package link.zamin.base.util


import com.google.gson.Gson
import com.google.gson.JsonArray
import link.zamin.base.exceptions.Exceptions
import java.io.DataOutputStream
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

    fun httpsHeadRequestSync(url: URL): HttpsURLConnection {
        val http: HttpsURLConnection = url.openConnection() as HttpsURLConnection
        http.requestMethod = "HEAD"
        http.disconnect()
        return http
    }

    fun <T> httpsGetRequestSync(
        url: String,
        queries: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        responseClass: Class<T>,
        readTimeOut: Int = 3000
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
            with((URL(urlWithQuery.toString()).openConnection() as HttpsURLConnection).apply {
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
                            bufferReader.lines().findFirst().get().let { line ->
                                return Gson().fromJson(line, responseClass)
                            }
                        }
                    }

                    else -> {
                        Logger.getLogger(this.javaClass.name).severe("$url GET exception: response error $responseCode")
                        throw Exceptions("$url GET exception: response error $responseCode")
                    }
                }
            }
        } catch (ex: Exception) {
            if (ex is Exceptions)
                throw ex
            Logger.getLogger(this.javaClass.name).severe("$url GET exception: ${ex.message}")
            throw Exceptions("$url GET exception: ${ex.message}")
        }
    }

    fun httpGetRequestSync(
        url: String,
        queries: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        readTimeOut: Int = 3000
    ): List<String> {
        val urlWithQuery = StringBuilder(url)
        if (queries.isNotEmpty()) {
            urlWithQuery.append("?")
            queries.forEach {
                urlWithQuery.append("${it.key}=${it.value}")
                urlWithQuery.append("&")
            }
        }
        try {
            with((URL(urlWithQuery.toString()).openConnection() as HttpURLConnection).apply {
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
                            return bufferReader.readLines()
                        }
                    }

                    else -> {
                        Logger.getLogger(this.javaClass.name).severe("$url GET exception: response error $responseCode")
                        throw Exceptions("$url GET exception: response error $responseCode")
                    }
                }
            }
        } catch (ex: Exception) {
            if (ex is Exceptions)
                throw ex
            Logger.getLogger(this.javaClass.name).severe("$url GET exception: ${ex.message}")
            throw Exceptions("$url GET exception: ${ex.message}")
        }
    }

    fun <T> httpsPostRequestSync(
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
                val out = JsonArray().apply {
                    requestBody.forEach {
                        add(it)
                    }
                }

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
                            .severe("$url POST exception: response error $responseCode")
                        throw Exceptions("$url POST exception: response error $responseCode")
                    }
                }
            }
        } catch (ex: Exception) {
            if (ex is Exceptions)
                throw ex
            Logger.getLogger(this.javaClass.name).severe("$url POST exception: ${ex.message}")
            throw Exceptions("$url POST exception: ${ex.message}")
        }
    }

    fun <T> httpsDeleteRequestSync(
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
                val out = JsonArray().apply {
                    requestBody.forEach {
                        add(it)
                    }
                }

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
                            .severe("$url DELETE exception: response error $responseCode")
                        throw Exceptions("$url DELETE exception: response error $responseCode")
                    }
                }
            }
        } catch (ex: Exception) {
            if (ex is Exceptions)
                throw ex
            Logger.getLogger(this.javaClass.name).severe("$url DELETE exception: ${ex.message}")
            throw Exceptions("$url DELETE exception: ${ex.message}")
        }
    }
}
