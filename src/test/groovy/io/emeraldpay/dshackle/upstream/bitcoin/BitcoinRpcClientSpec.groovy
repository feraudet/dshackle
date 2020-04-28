/**
 * Copyright (c) 2020 EmeraldPay, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.emeraldpay.dshackle.upstream.bitcoin

import io.emeraldpay.dshackle.config.AuthConfig
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import org.mockserver.verify.VerificationTimes
import reactor.test.StepVerifier
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration

class BitcoinRpcClientSpec extends Specification {

    ClientAndServer mockServer

    def setup() {
        mockServer = ClientAndServer.startClientAndServer(18332);
    }

    def cleanup() {
        mockServer.stop()
    }

    def "Make request"() {
        setup:
        def client = new BitcoinRpcClient("localhost:18332", null)

        mockServer.when(
                HttpRequest.request()
                        .withMethod("POST")
                        .withBody("ping"),
                Times.exactly(1)
        ).respond(
                HttpResponse.response()
                        .withBody("pong")
        )
        when:
        def act = client.execute("ping".bytes).map { new String(it) }
        then:
        StepVerifier.create(act)
                .expectNext("pong")
                .expectComplete()
                .verify(Duration.ofSeconds(1))
        mockServer.verify(
                HttpRequest.request()
                        .withMethod("POST")
                        .withBody("ping")
                        .withContentType(MediaType.APPLICATION_JSON)
        )

    }

    def "Make request with basic auth"() {
        setup:
        def auth = new AuthConfig.ClientBasicAuth("user", "passwd")
        def client = new BitcoinRpcClient("localhost:18332", auth)

        mockServer.when(
                HttpRequest.request()
                        .withMethod("POST")
                        .withBody("ping")
        ).respond(
                HttpResponse.response()
                        .withBody("pong")
        )
        when:
        def act = client.execute("ping".bytes).map { new String(it) }
        then:
        StepVerifier.create(act)
                .expectNext("pong")
                .expectComplete()
                .verify(Duration.ofSeconds(1))
        mockServer.verify(
                HttpRequest.request()
                        .withMethod("POST")
                        .withBody("ping")
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withHeader("authorization", "Basic dXNlcjpwYXNzd2Q=")
        )
    }

}
