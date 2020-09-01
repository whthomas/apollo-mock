package io.github.whthomas

import com.ctrip.framework.apollo.core.dto.ApolloConfig
import com.ctrip.framework.apollo.core.dto.ApolloConfigNotification
import com.ctrip.framework.apollo.core.dto.ServiceDTO
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.util.*

class EmbedApolloServerDispatcher(
    private val serverPort: Int,
    private val objectMapper: ObjectMapper,
    private val propertiesFilePath: String,
    private var apolloConfig: ApolloConfig? = null
) : Dispatcher() {

    override fun dispatch(request: RecordedRequest): MockResponse {

        if (request.path!!.startsWith("/services/config")) {
            return MockResponse().setResponseCode(200).setBody(mockServiceDTO())
        }

        if (request.path!!.startsWith("/notifications/v2")) {
            val notifications = request.requestUrl!!.queryParameter("notifications")
            return MockResponse().setResponseCode(200).setBody(mockNotificationsResponse(notifications!!))
        }

        if (request.path!!.startsWith("/configs")) {
            val pathSegments = request.requestUrl!!.pathSegments

            val appId = pathSegments[1]
            val cluster = pathSegments[2]
            val namespace = pathSegments[3]

            return MockResponse().setResponseCode(200).setBody(mockConfigDataResponse(namespace))
        }

        return MockResponse().setResponseCode(404)

    }

    private fun mockNotificationsResponse(notifications: String): String {

        val constructCollectionType = objectMapper
            .typeFactory
            .constructCollectionType(
                List::class.java,
                ApolloConfigNotification::class.java
            );

        val apolloConfigNotifications =
            objectMapper.readValue(notifications, constructCollectionType) as List<ApolloConfigNotification>

        return apolloConfigNotifications
            .map {
                ApolloConfigNotification(
                    it.namespaceName,
                    it.notificationId + 1
                )
            }
            .run {
                objectMapper.writeValueAsString(this)
            }

    }

    private fun mockServiceDTO(): String {

        ServiceDTO()
            .apply {
                this.appName = "APOLLO-CONFIGSERVICE"
                this.instanceId = "127.0.0.1:apollo-configservice:$serverPort"
                this.homepageUrl = "http://127.0.0.1:$serverPort/"
            }
            .apply {
                return objectMapper.writeValueAsString(listOf(this))
            }


    }

    fun mockConfigDataResponse(namespace: String): String {

        if (Objects.nonNull(apolloConfig)) {
            return objectMapper.writeValueAsString(apolloConfig)
        }

        val prop = Properties()
        val newApolloConfig = ApolloConfig("someAppId", "someCluster", namespace, "someReleaseKey")

        return EmbedApolloServerDispatcher::class
            .java
            .classLoader
            .getResourceAsStream(propertiesFilePath)
            .use { prop.load(it) }
            .run { prop.stringPropertyNames() }
            .map { it to prop.getProperty(it) }
            .toMap()
            .run { newApolloConfig.configurations = this }
            .run { apolloConfig = newApolloConfig }
            .run { objectMapper.writeValueAsString(apolloConfig) }

    }

}