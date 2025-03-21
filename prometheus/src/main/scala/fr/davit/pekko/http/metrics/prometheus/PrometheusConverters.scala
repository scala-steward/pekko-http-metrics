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

package fr.davit.pekko.http.metrics.prometheus

trait PrometheusConverters {

  implicit def convertCounter(counter: io.prometheus.metrics.core.metrics.Counter): PrometheusCounter =
    new PrometheusCounter(counter)

  implicit def convertGauge(gauge: io.prometheus.metrics.core.metrics.Gauge): PrometheusGauge =
    new PrometheusGauge(gauge)

  implicit def convertSummaryTimer(summary: io.prometheus.metrics.core.metrics.Summary): PrometheusSummaryTimer =
    new PrometheusSummaryTimer(summary)

  implicit def convertHistogramTimer(
      histogram: io.prometheus.metrics.core.metrics.Histogram
  ): PrometheusHistogramTimer =
    new PrometheusHistogramTimer(histogram)

  implicit def convertSummary(summary: io.prometheus.metrics.core.metrics.Summary): PrometheusSummary =
    new PrometheusSummary(summary)

  implicit def convertHistogram(histogram: io.prometheus.metrics.core.metrics.Histogram): PrometheusHistogram =
    new PrometheusHistogram(histogram)

}

object PrometheusConverters extends PrometheusConverters
