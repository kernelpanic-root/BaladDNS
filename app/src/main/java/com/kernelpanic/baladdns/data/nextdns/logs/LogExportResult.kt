package com.kernelpanic.baladdns.data.nextdns.logs

import com.kernelpanic.baladdns.domain.nextdns.ApiResult

sealed interface LogExportResult {
    data object Success : LogExportResult
    data class ApiFailure(val result: ApiResult<Nothing>) : LogExportResult
    data class DestinationFailure(val cause: Throwable) : LogExportResult
}
