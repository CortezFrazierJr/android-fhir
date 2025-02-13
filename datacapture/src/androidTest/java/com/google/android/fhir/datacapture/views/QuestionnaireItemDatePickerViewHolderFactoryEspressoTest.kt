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

package com.google.android.fhir.datacapture.views

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.fhir.datacapture.R
import com.google.android.fhir.datacapture.TestActivity
import com.google.android.fhir.datacapture.utilities.clickIcon
import com.google.android.fhir.datacapture.validation.Invalid
import com.google.android.fhir.datacapture.validation.NotValidated
import com.google.android.fhir.datacapture.validation.QuestionnaireResponseItemValidator
import com.google.android.fhir.datacapture.validation.Valid
import com.google.common.truth.Truth.assertThat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.hamcrest.CoreMatchers.allOf
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuestionnaireItemDatePickerViewHolderFactoryEspressoTest {

  @Rule
  @JvmField
  var activityScenarioRule: ActivityScenarioRule<TestActivity> =
    ActivityScenarioRule(TestActivity::class.java)

  private lateinit var parent: FrameLayout
  private lateinit var viewHolder: QuestionnaireItemViewHolder
  @Before
  fun setup() {
    activityScenarioRule.getScenario().onActivity { activity -> parent = FrameLayout(activity) }
    viewHolder = QuestionnaireItemDatePickerViewHolderFactory.create(parent)
    setTestLayout(viewHolder.itemView)
  }

  @Test
  fun validDateTextInput_updateCalenderDialogView() {
    activityScenarioRule.scenario.onActivity { activity -> setLocale(Locale.US, activity) }
    val questionnaireItemView =
      QuestionnaireItemViewItem(
        Questionnaire.QuestionnaireItemComponent().apply { text = "Question?" },
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _ -> },
      )

    runOnUI {
      viewHolder.bind(questionnaireItemView)
      viewHolder.itemView.findViewById<TextView>(R.id.text_input_edit_text).text = "11/19/20"
    }

    onView(withId(R.id.text_input_layout)).perform(clickIcon(true))
    onView(allOf(withText("Nov 19, 2020")))
      .inRoot(RootMatchers.isDialog())
      .check(matches(ViewMatchers.isDisplayed()))
  }

  @Test
  fun shouldSetDateInput() {
    val questionnaireItemView =
      QuestionnaireItemViewItem(
        Questionnaire.QuestionnaireItemComponent(),
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _ -> },
      )

    runOnUI { viewHolder.bind(questionnaireItemView) }

    onView(withId(R.id.text_input_layout)).perform(clickIcon(true))
    onView(allOf(withText("OK")))
      .inRoot(isDialog())
      .check(matches(isDisplayed()))
      .perform(ViewActions.click())

    val today = DateTimeType.today().valueAsString

    assertThat(questionnaireItemView.answers.singleOrNull()?.valueDateType?.valueAsString)
      .isEqualTo(today)
  }

  @Test
  fun shouldSetDateInput_withinRange() {
    val questionnaireItemView =
      QuestionnaireItemViewItem(
        Questionnaire.QuestionnaireItemComponent().apply {
          addExtension().apply {
            url = "http://hl7.org/fhir/StructureDefinition/minValue"
            val minDate = DateType(Date()).apply { add(Calendar.YEAR, -1) }
            setValue(minDate)
          }
          addExtension().apply {
            url = "http://hl7.org/fhir/StructureDefinition/maxValue"
            val maxDate = DateType(Date()).apply { add(Calendar.YEAR, 4) }
            setValue(maxDate)
          }
        },
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _ -> },
      )

    runOnUI { viewHolder.bind(questionnaireItemView) }

    onView(withId(R.id.text_input_layout)).perform(clickIcon(true))
    onView(allOf(withText("OK")))
      .inRoot(isDialog())
      .check(matches(isDisplayed()))
      .perform(ViewActions.click())

    val today = DateTimeType.today().valueAsString
    val validationResult =
      QuestionnaireResponseItemValidator.validate(
        questionnaireItemView.questionnaireItem,
        questionnaireItemView.answers,
        viewHolder.itemView.context
      )

    assertThat(questionnaireItemView.answers.singleOrNull()?.valueDateType?.valueAsString)
      .isEqualTo(today)
    assertThat(validationResult).isEqualTo(Valid)
  }

  @Test
  fun shouldNotSetDateInput_outsideMaxRange() {
    val maxDate = DateType(Date()).apply { add(Calendar.YEAR, -2) }
    val questionnaireItemView =
      QuestionnaireItemViewItem(
        Questionnaire.QuestionnaireItemComponent().apply {
          addExtension().apply {
            url = "http://hl7.org/fhir/StructureDefinition/minValue"
            val minDate = DateType(Date()).apply { add(Calendar.YEAR, -4) }
            setValue(minDate)
          }
          addExtension().apply {
            url = "http://hl7.org/fhir/StructureDefinition/maxValue"
            setValue(maxDate)
          }
        },
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _ -> },
      )

    runOnUI { viewHolder.bind(questionnaireItemView) }

    onView(withId(R.id.text_input_layout)).perform(clickIcon(true))
    onView(allOf(withText("OK")))
      .inRoot(isDialog())
      .check(matches(isDisplayed()))
      .perform(ViewActions.click())

    val maxDateAllowed = maxDate.valueAsString
    val validationResult =
      QuestionnaireResponseItemValidator.validate(
        questionnaireItemView.questionnaireItem,
        questionnaireItemView.answers,
        viewHolder.itemView.context
      )

    assertThat((validationResult as Invalid).getSingleStringValidationMessage())
      .isEqualTo("Maximum value allowed is:$maxDateAllowed")
  }

  @Test
  fun shouldNotSetDateInput_outsideMinRange() {
    val minDate = DateType(Date()).apply { add(Calendar.YEAR, 1) }
    val questionnaireItemView =
      QuestionnaireItemViewItem(
        Questionnaire.QuestionnaireItemComponent().apply {
          addExtension().apply {
            url = "http://hl7.org/fhir/StructureDefinition/minValue"
            setValue(minDate)
          }
          addExtension().apply {
            url = "http://hl7.org/fhir/StructureDefinition/maxValue"
            val maxDate = DateType(Date()).apply { add(Calendar.YEAR, 2) }
            setValue(maxDate)
          }
        },
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _ -> },
      )

    runOnUI { viewHolder.bind(questionnaireItemView) }

    onView(withId(R.id.text_input_layout)).perform(clickIcon(true))
    onView(allOf(withText("OK")))
      .inRoot(isDialog())
      .check(matches(isDisplayed()))
      .perform(ViewActions.click())

    val minDateAllowed = minDate.valueAsString
    val validationResult =
      QuestionnaireResponseItemValidator.validate(
        questionnaireItemView.questionnaireItem,
        questionnaireItemView.answers,
        viewHolder.itemView.context
      )

    assertThat((validationResult as Invalid).getSingleStringValidationMessage())
      .isEqualTo("Minimum value allowed is:$minDateAllowed")
  }

  @Test
  fun shouldThrowException_whenMinValueRangeIsGreaterThanMaxValueRange() {
    val questionnaireItemView =
      QuestionnaireItemViewItem(
        Questionnaire.QuestionnaireItemComponent().apply {
          addExtension().apply {
            url = "http://hl7.org/fhir/StructureDefinition/minValue"
            val minDate = DateType(Date()).apply { add(Calendar.YEAR, 1) }
            setValue(minDate)
          }
          addExtension().apply {
            url = "http://hl7.org/fhir/StructureDefinition/maxValue"
            val maxDate = DateType(Date()).apply { add(Calendar.YEAR, -1) }
            setValue(maxDate)
          }
        },
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _ -> },
      )

    runOnUI { viewHolder.bind(questionnaireItemView) }

    val exception =
      assertThrows(IllegalArgumentException::class.java) {
        onView(withId(R.id.text_input_layout)).perform(clickIcon(true))
        onView(allOf(withText("OK")))
          .inRoot(isDialog())
          .check(matches(isDisplayed()))
          .perform(ViewActions.click())
      }
    assertThat(exception.message).isEqualTo("minValue cannot be greater than maxValue")
  }

  /** Runs code snippet on UI/main thread */
  private fun runOnUI(action: () -> Unit) {
    activityScenarioRule.scenario.onActivity { activity -> action() }
  }

  /** Sets content view for test activity */
  private fun setTestLayout(view: View) {
    activityScenarioRule.scenario.onActivity { activity -> activity.setContentView(view) }
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
  }

  private fun setLocale(locale: Locale, context: Context) {
    Locale.setDefault(locale)
    context.resources.configuration.setLocale(locale)
  }
}
