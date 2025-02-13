/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.fhir.datacapture.validation

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport
import com.google.android.fhir.datacapture.CQF_CALCULATED_EXPRESSION_URL
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.Type
import org.hl7.fhir.r4.utils.FHIRPathEngine

fun Type.valueOrCalculateValue(): Type? {
  return if (this.hasExtension()) {
    this.extension.firstOrNull { it.url == CQF_CALCULATED_EXPRESSION_URL }?.let {
      val expression = (it.value as Expression).expression
      fhirPathEngine.evaluate(this, expression).firstOrNull()?.let { it as Type }
    }
  } else {
    this
  }
}

private val fhirPathEngine: FHIRPathEngine =
  with(FhirContext.forCached(FhirVersionEnum.R4)) {
    FHIRPathEngine(HapiWorkerContext(this, DefaultProfileValidationSupport(this)))
  }
