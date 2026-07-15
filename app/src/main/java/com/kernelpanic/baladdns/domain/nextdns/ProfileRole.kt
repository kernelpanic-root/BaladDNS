package com.kernelpanic.baladdns.domain.nextdns

enum class ProfileRole {
    Owner,
    Editor,
    Viewer,
    Unknown,
}

data class ProfileCapabilities(
    val canEditSettings: Boolean,
    val canManageAccess: Boolean,
    val canDelete: Boolean,
    val canLeave: Boolean,
)

fun profileRoleFromWire(value: String?): ProfileRole = when (value?.lowercase()) {
    "owner" -> ProfileRole.Owner
    "editor" -> ProfileRole.Editor
    "viewer" -> ProfileRole.Viewer
    else -> ProfileRole.Unknown
}

fun ProfileRole.capabilities() = when (this) {
    ProfileRole.Owner -> ProfileCapabilities(
        canEditSettings = true,
        canManageAccess = true,
        canDelete = true,
        canLeave = false,
    )

    ProfileRole.Editor -> ProfileCapabilities(
        canEditSettings = true,
        canManageAccess = false,
        canDelete = false,
        canLeave = true,
    )

    ProfileRole.Viewer -> ProfileCapabilities(
        canEditSettings = false,
        canManageAccess = false,
        canDelete = false,
        canLeave = true,
    )

    ProfileRole.Unknown -> ProfileCapabilities(
        canEditSettings = false,
        canManageAccess = false,
        canDelete = false,
        canLeave = false,
    )
}
