package com.kernelpanic.baladdns.data.nextdns.access

import com.kernelpanic.baladdns.domain.nextdns.ApiProblem
import org.junit.Assert.assertEquals
import org.junit.Test

class AccessFormValidationTest {
    @Test
    fun `maps duplicate server errors even without a field pointer`() {
        val errors = AccessFormValidation.serverErrors(
            listOf(ApiProblem(code = "duplicate"))
        )

        assertEquals(AccessError.Duplicate, errors[AccessField.Email])
    }
}
