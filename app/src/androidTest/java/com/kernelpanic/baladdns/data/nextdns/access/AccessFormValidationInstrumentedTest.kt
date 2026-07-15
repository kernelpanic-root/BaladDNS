package com.kernelpanic.baladdns.data.nextdns.access

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessFormValidationInstrumentedTest {

    @Test
    fun validatesRequiredAndMalformedEmailAddresses() {
        assertEquals(
            AccessError.Required,
            AccessFormValidation.localErrors("")[AccessField.Email],
        )
        assertEquals(
            AccessError.Invalid,
            AccessFormValidation.localErrors("not-an-email")[AccessField.Email],
        )
        assertTrue(AccessFormValidation.localErrors("person@example.com").isEmpty())
    }
}
