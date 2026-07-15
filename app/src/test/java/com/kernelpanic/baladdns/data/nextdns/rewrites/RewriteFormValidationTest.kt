package com.kernelpanic.baladdns.data.nextdns.rewrites

import com.kernelpanic.baladdns.domain.nextdns.ApiProblem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RewriteFormValidationTest {

    @Test
    fun `accepts supported rewrite domain and answer shapes`() {
        assertTrue(RewriteFormValidation.localErrors("printer.lan", "192.0.2.10").isEmpty())
        assertTrue(RewriteFormValidation.localErrors("printer", "target.example").isEmpty())
        assertTrue(RewriteFormValidation.localErrors("printer", "2001:db8::1").isEmpty())
    }

    @Test
    fun `rejects required malformed and IP rewrite domains`() {
        assertEquals(
            RewriteError.Required,
            RewriteFormValidation.localErrors("", "1.1.1.1")[RewriteField.Name],
        )
        assertEquals(
            RewriteError.Ip,
            RewriteFormValidation.localErrors("192.0.2.10", "target.example")[RewriteField.Name],
        )
        assertEquals(
            RewriteError.Invalid,
            RewriteFormValidation.localErrors("https://example.com", "target.example")[RewriteField.Name],
        )
        assertEquals(
            RewriteError.Invalid,
            RewriteFormValidation.localErrors("printer", "not/a-domain")[RewriteField.Content],
        )
    }

    @Test
    fun `maps pointer and unscoped server errors to fields`() {
        val errors = RewriteFormValidation.serverErrors(
            listOf(
                ApiProblem(code = "invalid", field = "/name"),
                ApiProblem(code = "duplicate"),
            )
        )

        assertEquals(RewriteError.Taken, errors[RewriteField.Name])
    }
}
