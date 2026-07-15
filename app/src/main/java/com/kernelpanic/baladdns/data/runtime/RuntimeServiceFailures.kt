package com.kernelpanic.baladdns.data.runtime

fun classifyRuntimeServiceFailure(error: RuntimeException): RuntimeServiceFailure = when {
    error is SecurityException -> RuntimeServiceFailure.MissingPermission
    error.javaClass.simpleName.contains("ForegroundServiceStartNotAllowed") ->
        RuntimeServiceFailure.StartNotAllowed
    else -> RuntimeServiceFailure.Unknown
}
