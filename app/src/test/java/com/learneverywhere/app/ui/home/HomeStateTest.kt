package com.learneverywhere.app.ui.home

import com.learneverywhere.app.translation.TranslationException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStateTest {
    @Test fun malformedProviderOutputCanBeRetried() {
        assertTrue(canRetryTranslation(TranslationException.Reason.INVALID_CONTENT))
    }

    @Test fun invalidUserInputDoesNotOfferPointlessRetry() {
        assertFalse(canRetryTranslation(TranslationException.Reason.INVALID_INPUT))
    }
}
