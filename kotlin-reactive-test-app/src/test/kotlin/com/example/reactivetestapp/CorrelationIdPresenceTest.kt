package com.example.reactivetestapp

import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.SoftAssertions
import org.junit.Ignore
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Ordering doesn't matter, but parallelism matters in this test,
 * in that I need to reun only one method at a time, otherwise responses get weird from my mockwebserver
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [CorrelationIdTestApp::class]
)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@AutoConfigureObservability
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CorrelationIdPresenceTest {
    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var webTestClient: WebTestClient

    companion object {
        private lateinit var mockWebServer: MockWebServer;

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            mockWebServer = MockWebServer()
            mockWebServer.start();

            //let's use a dispatcher and set that up
            val dispatcher = object : Dispatcher() {
                private val logger = KotlinLogging.logger {}
                override fun dispatch(request: RecordedRequest): MockResponse {
                    logger.debug { "DISPATCHING: ${request.path}" }
                    val path = request.path!!
                    val method = path.split("/").last()

                    return MockResponse().setResponseCode(200)
                        .setHeader("x-derp-header", "LookItsADerp")
                        .setBody("""{"testMethod": "${method}"} """)
                }
            }

            mockWebServer.dispatcher = dispatcher
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            mockWebServer.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        internal fun backendProperties(registry: DynamicPropertyRegistry) {
            registry.add("upstream.url") { mockWebServer.url("/").toString() }
        }
    }

    @Test
    @Order(1)
    fun createsACorrelationIDandOtelTraceHeaderWhenNone() {
        val response = webTestClient.get()
            .uri("/test/createsACorrelationIDandOtelTraceHeaderWhenNone")
            .exchange()

        val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)!!

        //Tracing Header Value
        val tracingHeaderValue = request.getHeader("traceparent")

        //The part of traceparent that is used in the correlation id isthe 32hexdigit, just the trace ID
        // 16 Byte UUID
        //Generate it using a UUID and then to byte array and then to hex
        // https://www.baeldung.com/java-byte-array-to-uuid
        val traceparentHex = tracingHeaderValue!!.split("-")[1];

        logger.debug { "\ntraceparentHex: ${traceparentHex}"}
        logger.debug { "Request Headers Received: \n${request.headers}\n-----" }

        val softly = SoftAssertions()

        logger.debug { request.headers["X-EXAMPLE-CORRELATIONID"] + " ==  ${traceparentHex}"}

        softly.assertThat(request.headers["X-EXAMPLE-CORRELATIONID"]).withFailMessage("X-EXAMPLE-CORRELATIONID is not correct").isEqualTo(traceparentHex)
        softly.assertThat(request.headers["X-CORRELATION-ID"]).withFailMessage("X-CORRELATION-ID is not correct").isEqualTo(traceparentHex)
        softly.assertThat(request.headers["CORRELATION-ID"]).withFailMessage("CORRELATION-ID is not correct").isEqualTo(traceparentHex)

        response
            .expectStatus().is2xxSuccessful
            .expectBody()
            .json("""{"testMethod":  "createsACorrelationIDandOtelTraceHeaderWhenNone"}""")

        softly.assertAll()
    }

    @Test
    @Order(2)
    fun createsACorrelationIDWhenOnlyOtelIsPresent() {
        // Invent my own traceparent thing, including the parent ID
        //Tracing Header Value
        val traceparent =
            "00-${convertUUIDToHexString(UUID.randomUUID())}-${createHalfUUIDHexString(UUID.randomUUID())}-00"
        logger.debug { "TRACE PARENT GENERATED HEADER: ${traceparent}" }

        //The part of traceparent that is used in the correlation id isthe 32hexdigit, just the trace ID
        // 16 Byte UUID
        //Generate it using a UUID and then to byte array and then to hex
        // https://www.baeldung.com/java-byte-array-to-uuid
        val traceparentTracingId = traceparent.split("-")[1];

        val response = webTestClient.get()
            .uri("/test/createsACorrelationIDWhenOnlyOtelIsPresent")
            .header("traceparent", traceparent)
            .exchange()

        val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)!!

        logger.debug { "Request Headers Received: \n${request.headers}\n-----" }

        val softly = SoftAssertions()

        softly.assertThat(request.headers["X-EXAMPLE-CORRELATIONID"]).isEqualTo(traceparentTracingId)
        softly.assertThat(request.headers["X-CORRELATION-ID"]).isEqualTo(traceparentTracingId)
        softly.assertThat(request.headers["CORRELATION-ID"]).isEqualTo(traceparentTracingId)

        response
            .expectStatus().is2xxSuccessful
            .expectBody()
            .json("""{"testMethod":  "createsACorrelationIDWhenOnlyOtelIsPresent"}""")

        softly.assertAll()
    }

    @Test
    @Order(3)
    fun createsAnOtelTraceHeaderWhenOnlyOneCorrelationIdIsPresent() {
        val correlationId = UUID.randomUUID().toString()

        val response = webTestClient.get()
            .uri("/test/createsAnOtelTraceHeaderWhenOnlyOneCorrelationIdIsPresent")
            .header("correlation-id", correlationId)
            .exchange()

        //Why is this throwing a null pointer?
        val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)!!

        logger.debug { "Request Headers Received: \n${request.headers}\n-----" }

        //All the configured correlation ID headers should always be the same, regardless of which is set
        val softly = SoftAssertions()

        softly.assertThat(request.headers["X-EXAMPLE-CORRELATIONID"]).isEqualTo(correlationId)
        softly.assertThat(request.headers["X-CORRELATION-ID"]).isEqualTo(correlationId)
        softly.assertThat(request.headers["CORRELATION-ID"]).isEqualTo(correlationId)

        //It'd be really cool if I could turn the UUID into a traceparent ID
        softly.assertThat(request.headers["traceparent"]).isNotEmpty()

        response
            .expectStatus().is2xxSuccessful
            .expectBody()
            .json("""{"testMethod":  "createsAnOtelTraceHeaderWhenOnlyOneCorrelationIdIsPresent"}""")

        softly.assertAll()
    }

    @Test
    @Order(4)
    fun persistsBothHeadersWhenPresentAndTheSame() {
        // Invent my own traceparent thing, including the parent ID
        //Tracing Header Value
        val traceparent =
            "00-${convertUUIDToHexString(UUID.randomUUID())}-${createHalfUUIDHexString(UUID.randomUUID())}-00"
        logger.debug { "persistsBothHeadersWhenPresentAndTheSame: TRACE PARENT GENERATED HEADER: ${traceparent}" }

        //The part of traceparent that is used in the correlation id isthe 32hexdigit, just the trace ID
        // 16 Byte UUID
        //Generate it using a UUID and then to byte array and then to hex
        // https://www.baeldung.com/java-byte-array-to-uuid
        val traceparentTracingId = traceparent.split("-")[1];

        val response = webTestClient.get()
            .uri("/test/persistsBothHeadersWhenPresentAndTheSame")
            .header("traceparent", traceparent)
            .header("X-example-correlationid", traceparentTracingId)
            .header("X-correlation-id", traceparentTracingId)
            .header("correlation-id", traceparentTracingId)
            .exchange()

        val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)!!

        logger.debug { "Request Headers Received: \n${request.headers}\n-----" }

        val softly = SoftAssertions()

        softly.assertThat(request.headers["X-EXAMPLE-CORRELATIONID"]).isEqualTo(traceparentTracingId)
        softly.assertThat(request.headers["X-CORRELATION-ID"]).isEqualTo(traceparentTracingId)
        softly.assertThat(request.headers["CORRELATION-ID"]).isEqualTo(traceparentTracingId)

        //Only part of the traceparent header is gonna match, the traceID part, not the span ID part
        TraceparentHeaderAssert.assertThat(request.headers["traceparent"]).hasTraceId(traceparentTracingId)

        response
            .expectStatus().is2xxSuccessful
            .expectBody()
            .json("""{"testMethod":  "persistsBothHeadersWhenPresentAndTheSame"}""")

        softly.assertAll()

    }

    @Test
    @Order(5)
    fun persistsBothHeadersWhenPresentAndDifferent() {
        // Invent my own traceparent thing, including the parent ID
        //Tracing Header Value
        val traceparent =
            "00-${convertUUIDToHexString(UUID.randomUUID())}-${createHalfUUIDHexString(UUID.randomUUID())}-00"
        logger.debug { "persistsBothHeadersWhenPresentAndDifferent: TRACE PARENT GENERATED HEADER: ${traceparent}" }

        val correlationId = UUID.randomUUID().toString()

        //The part of traceparent that is used in the correlation id isthe 32hexdigit, just the trace ID
        // 16 Byte UUID
        //Generate it using a UUID and then to byte array and then to hex
        // https://www.baeldung.com/java-byte-array-to-uuid
        val traceparentTracingId = traceparent.split("-")[1];

        val response = webTestClient.get()
            .uri("/test/persistsBothHeadersWhenPresentAndDifferent")
            .header("traceparent", traceparent)
            .header("X-example-correlationid", correlationId)
            .header("X-correlation-id", correlationId)
            .header("correlation-id", correlationId)
            .exchange()

        val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)!!

        logger.debug { "Request Headers Received: \n${request.headers}\n-----" }

        val softly = SoftAssertions()

        softly.assertThat(request.headers["X-EXAMPLE-CORRELATIONID"]).isEqualTo(correlationId)
        softly.assertThat(request.headers["X-CORRELATION-ID"]).isEqualTo(correlationId)
        softly.assertThat(request.headers["CORRELATION-ID"]).isEqualTo(correlationId)

        TraceparentHeaderAssert.assertThat(request.headers["traceparent"]).hasTraceId(traceparentTracingId)

        response
            .expectStatus().is2xxSuccessful
            .expectBody()
            .json("""{"testMethod":  "persistsBothHeadersWhenPresentAndDifferent"}""")

        softly.assertAll()
    }

    @Ignore
    @Order(5)
    fun setsUpTraceParentUsingCorrelationIDIfIsAUUID() {
        //This one is hard, but if the CorrelationID is a UUID, it should be able to consume that
        // and turn it into a tracingID. It's not necessary, but I'd like to try to figure that out.

        val uuid = UUID.randomUUID()
        val correlationId = uuid.toString()
        val tracingId = convertUUIDToHexString(uuid);

        val response = webTestClient.get()
            .uri("/test/setsUpTraceParentUsingCorrelationIDIfIsAUUID")
            .header("X-example-correlationid", correlationId)
            .header("X-correlation-id", correlationId)
            .header("correlation-id", correlationId)
            .exchange()

        val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS)!!

        logger.debug { "Request Headers Received: \n${request.headers}\n-----" }

        val softly = SoftAssertions()

        softly.assertThat(request.headers["X-EXAMPLE-CORRELATIONID"]).isEqualTo(correlationId)
        softly.assertThat(request.headers["X-CORRELATION-ID"]).isEqualTo(correlationId)
        softly.assertThat(request.headers["CORRELATION-ID"]).isEqualTo(correlationId)

        TraceparentHeaderAssert.assertThat(request.headers["traceparent"]).hasTraceId(tracingId)

        response
            .expectStatus().is2xxSuccessful
            .expectBody()
            .json("""{"testMethod":  "setsUpTraceParentUsingCorrelationIDIfIsAUUID"}""")

        softly.assertAll()
    }

    class TraceparentHeaderAssert(actual: String?) :
        AbstractAssert<TraceparentHeaderAssert, String>(
            actual,
            TraceparentHeaderAssert::class.java
        ) {

        companion object {
            @JvmStatic
            fun assertThat(actual: String?): TraceparentHeaderAssert {
                return TraceparentHeaderAssert(actual)
            }
        }

        fun hasTraceId(traceId: String?) : TraceparentHeaderAssert {
            actual.isNotBlank()

            val actualTraceId = actual.split("-")[1]
            if(!Objects.equals(actualTraceId, traceId)) {
                failWithMessage("Expected traceID to be <%s> but was <%s>", traceId, actualTraceId)
            }
            return this
        }
    }


    fun convertUUIDToHexString(uuid: UUID): String {
        val bb: ByteBuffer = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        val sb = StringBuilder()
        for (b in bb.array()) {
            sb.append(String.format("%02x", b))
        }

        return sb.toString()
    }

    fun createHalfUUIDHexString(uuid: UUID): String {
        val bb: ByteBuffer = ByteBuffer.wrap(ByteArray(8))
        bb.putLong(uuid.mostSignificantBits)
        val sb = StringBuilder()
        for (b in bb.array()) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

}