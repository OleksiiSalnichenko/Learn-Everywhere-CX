package com.learneverywhere.app.ui.home

import com.learneverywhere.app.translation.TranslationException
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeStateTest {
    @Test fun everyTranslationFailureHasAnExplicitRetryPolicy() {
        val retryable = setOf(
            TranslationException.Reason.INVALID_CONTENT,
            TranslationException.Reason.QUOTA,
            TranslationException.Reason.NETWORK,
            TranslationException.Reason.UNAVAILABLE,
        )
        assertEquals(retryable, TranslationException.Reason.entries.filter(::canRetryTranslation).toSet())
    }
}
