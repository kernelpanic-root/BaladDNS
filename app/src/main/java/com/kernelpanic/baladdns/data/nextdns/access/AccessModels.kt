package com.kernelpanic.baladdns.data.nextdns.access

import com.google.gson.annotations.SerializedName

enum class AccessRole {
    @SerializedName("editor")
    Editor,

    @SerializedName("viewer")
    Viewer,
}

data class AccessEntry(
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: AccessRole,
    @SerializedName("pending") val pending: Boolean,
)

data class InviteAccessRequest(
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: AccessRole,
)

data class UpdateAccessRoleRequest(
    @SerializedName("role") val role: AccessRole,
)
