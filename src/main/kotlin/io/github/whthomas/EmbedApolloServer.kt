package io.github.whthomas

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import okhttp3.mockwebserver.MockWebServer

class EmbedApolloServer(
    val port: Int,
    val mockPropertiesFilePath: String,
    val mockWebServer: MockWebServer = MockWebServer()
) {

    /**
     * 初始化Apollo要读取的配置
     */
    fun initConfig() {

        val objectMapper = ObjectMapper()
            .registerKotlinModule()

        mockWebServer.dispatcher = EmbedApolloServerDispatcher(
            port,
            objectMapper,
            mockPropertiesFilePath
        )

    }

    /**
     * 启动EmbedApolloServer
     */
    fun start() {
        initConfig()
        mockWebServer.start(port = port)
    }

    /**
     * 关闭EmbedApolloServer
     */
    fun stop() = mockWebServer.shutdown()

    /**
     * 重启EmbedApolloServer
     */
    fun restart() {
        start()
        stop()
    }

}