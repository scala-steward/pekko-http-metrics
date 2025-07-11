/*
 * Copyright 2019 Michel Davit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.davit.pekko.http.metrics.prometheus.marshalling

import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import fr.davit.pekko.http.metrics.core.{Dimension, StatusGroupLabeler}
import fr.davit.pekko.http.metrics.core.scaladsl.server.HttpMetricsDirectives.metrics
import fr.davit.pekko.http.metrics.prometheus.{PrometheusRegistry, PrometheusSettings}
import io.prometheus.metrics.model.{registry => prometheus}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import org.apache.pekko.http.scaladsl.model.headers.Accept
import org.apache.pekko.stream.scaladsl.StreamConverters

class PrometheusMarshallersSpec extends AnyFlatSpec with Matchers with ScalatestRouteTest with BeforeAndAfterAll {

  trait Fixture extends PrometheusMarshallers {

    val registry = PrometheusRegistry(
      new prometheus.PrometheusRegistry(),
      PrometheusSettings.default.withIncludeStatusDimension(true)
    )

    io.prometheus.metrics.core.metrics.Counter
      .builder()
      .name("other_metric")
      .help("An other metric")
      .register(registry.underlying)

    // register labeled metrics so they appear at least once
    // use metrics so they appear in the report
    val dimensions = Seq(Dimension(StatusGroupLabeler.name, "2xx"))
    registry.requests.inc()
    registry.requestsActive.inc()
    registry.requestsSize.update(10)
    registry.responses.inc(dimensions)
    registry.responsesErrors.inc(dimensions)
    registry.responsesDuration.observe(1.second, dimensions)
    registry.responsesSize.update(10, dimensions)
    registry.connections.inc()
    registry.connectionsActive.inc()
  }

  override def afterAll(): Unit = {
    cleanUp()
    super.afterAll()
  }

  "PrometheusMarshallers" should "expose metrics as text format" in new Fixture {
    Get() ~> metrics(registry) ~> check {
      response.entity.contentType shouldBe PrometheusMarshallers.TextContentType
      val text    = responseAs[String]
      // println(text)
      val metrics = text
        .split('\n')
        .filterNot(_.startsWith("#"))
        .map(_.takeWhile(c => c != ' ' && c != '{'))
        .distinct
      metrics should contain theSameElementsAs Seq(
        "pekko_http_requests_total",
        "pekko_http_requests_active",
        "pekko_http_requests_size_bytes_bucket",
        "pekko_http_requests_size_bytes_count",
        "pekko_http_requests_size_bytes_sum",
        "pekko_http_responses_total",
        "pekko_http_responses_errors_total",
        "pekko_http_responses_duration_seconds_bucket",
        "pekko_http_responses_duration_seconds_count",
        "pekko_http_responses_duration_seconds_sum",
        "pekko_http_responses_size_bytes_bucket",
        "pekko_http_responses_size_bytes_count",
        "pekko_http_responses_size_bytes_sum",
        "pekko_http_connections_total",
        "pekko_http_connections_active",
        "other_metric_total"
      )
    }
  }

  it should "expose metrics as prometheus open-metrics format" in new Fixture {
    Get() ~> Accept(PrometheusMarshallers.OpenMetricsContentType.mediaType) ~> metrics(registry) ~> check {
      response.entity.contentType shouldBe PrometheusMarshallers.OpenMetricsContentType
      val text    = responseAs[String]
      // println(text)
      val metrics = text
        .split('\n')
        .filterNot(_.startsWith("#"))
        .map(_.takeWhile(c => c != ' ' && c != '{'))
        .distinct
      metrics should contain theSameElementsAs Seq(
        "pekko_http_requests_total",
        "pekko_http_requests_active",
        "pekko_http_requests_size_bytes_bucket",
        "pekko_http_requests_size_bytes_count",
        "pekko_http_requests_size_bytes_sum",
        "pekko_http_responses_total",
        "pekko_http_responses_errors_total",
        "pekko_http_responses_duration_seconds_bucket",
        "pekko_http_responses_duration_seconds_count",
        "pekko_http_responses_duration_seconds_sum",
        "pekko_http_responses_size_bytes_bucket",
        "pekko_http_responses_size_bytes_count",
        "pekko_http_responses_size_bytes_sum",
        "pekko_http_connections_total",
        "pekko_http_connections_active",
        "other_metric_total"
      )
    }
  }

  it should "expose metrics as prometheus protobuf format" in new Fixture {
    import io.prometheus.metrics.expositionformats.generated.com_google_protobuf_4_31_0.Metrics

    Get() ~> Accept(PrometheusMarshallers.ProtobufContentType.mediaType) ~> metrics(registry) ~> check {
      response.entity.contentType shouldBe PrometheusMarshallers.ProtobufContentType
      val is = response.entity.dataBytes.runWith(StreamConverters.asInputStream())
      try {
        val metrics = Iterator
          .unfold(is)(is => Option(Metrics.MetricFamily.parseDelimitedFrom(is)).map((_, is)))
          // .tapEach(println)
          .map(_.getName())
          .toList

        metrics should contain theSameElementsAs Seq(
          "pekko_http_requests_total",
          "pekko_http_requests_active",
          "pekko_http_requests_size_bytes",
          "pekko_http_responses_total",
          "pekko_http_responses_errors_total",
          "pekko_http_responses_duration_seconds",
          "pekko_http_responses_size_bytes",
          "pekko_http_connections_total",
          "pekko_http_connections_active",
          "other_metric_total"
        )
      } finally {
        is.close()
      }
    }
  }
}
