package blackorbs.dev.jetfiledownloader.robots

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput

class Robot(private val context: Context, private val composeRule: ComposeTestRule) {

    fun assertTextDisplayed(
        @StringRes textResId: Int = -1,
        text: String = context.getString(textResId)
    ) = composeRule.onNode(hasText(text)).assertIsDisplayed()

    fun assertTextNotDisplayed(
        @StringRes textResId: Int = -1,
        text: String = context.getString(textResId)
    ) = composeRule.onNode(hasText(text)).assertIsNotDisplayed()

    /**
     * Asserts string resource with id [textResId]
     * and string resource replacement id [textReplacementId] is displayed
     */
    fun assertTextDisplayed(
        @StringRes textResId: Int = -1,
        @StringRes textReplacementId: Int = -1,
        text: String = context.getString(
            textResId, context.getString(textReplacementId)
        )
    ) = composeRule.onNode(hasText(text)).assertIsDisplayed()

    fun assertTextNotDisplayed(
        @StringRes textResId: Int = -1,
        @StringRes textReplacementId: Int = -1,
        text: String = context.getString(
            textResId, context.getString(textReplacementId)
        )
    ) = composeRule.onNode(hasText(text)).assertIsNotDisplayed()

    fun assertIconDisplayed(
        @StringRes contentDescriptionResId: Int = -1,
        desc: String = context.getString(contentDescriptionResId)
    ) = composeRule.onNode(hasContentDescription(desc)).assertIsDisplayed()

    fun assertIconsCount(
        @StringRes contentDescriptionResId: Int = -1,
        desc: String = context.getString(contentDescriptionResId),
        num: Int
    ) = composeRule.onAllNodes(hasContentDescription(desc))
        .assertCountEquals(num)

    fun assertIconNotDisplayed(
        @StringRes contentDescriptionResId: Int = -1,
        desc: String = context.getString(contentDescriptionResId)
    ) = composeRule.onNode(hasContentDescription(desc)).assertIsNotDisplayed()

    fun assertIconReplaced(
        @StringRes contentDescriptionResId: Int = -1,
        @StringRes replacementResId: Int = -1
    ) = assertIconNotDisplayed(contentDescriptionResId)
        .also { assertIconDisplayed(replacementResId) }

    fun enterText(
        @StringRes textFieldTagResId: Int = -1,
        textFieldTag: String = context.getString(textFieldTagResId),
        text: String
    ) = composeRule.onNodeWithTag(textFieldTag).assertIsDisplayed()
        .performTextInput(text).also { composeRule.waitForIdle() }

    fun clickIcon(
        @StringRes contentDescriptionResId: Int = -1,
        desc: String = context.getString(contentDescriptionResId)
    ) = composeRule.onNode(hasContentDescription(desc).and(hasClickAction()))
        .assertIsDisplayed().performClick()
        .also { composeRule.waitForIdle() }

    fun clickText(
        @StringRes textResId: Int = -1,
        text: String = context.getString(textResId)
    ) = composeRule.onNode(hasText(text))
        .assertIsDisplayed().performClick()
        .also { composeRule.waitForIdle() }

    fun longClickText(
        @StringRes textResId: Int = -1,
        text: String = context.getString(textResId)
    ) = composeRule.onNode(hasText(text))
        .assertIsDisplayed().performTouchInput { longClick() }
        .also { composeRule.waitForIdle() }

    fun clickTextWithIcon(
        @StringRes textResId: Int = -1,
        text: String = context.getString(textResId)
    ) = composeRule.onNode(hasText(text).and(hasClickAction()).and(
        hasParent(hasAnyChild(hasContentDescription(text).and(hasClickAction())))
    )).assertIsDisplayed().performClick()
        .also { composeRule.waitForIdle() }
}