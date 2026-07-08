package com.eyalm.adns.data.nextdns.model

sealed class ListIcon {
    data class Url(val url: String) : ListIcon()
    data class BuiltIn(val key: BuiltInListIcon) : ListIcon()
    data class Text(val text: String) : ListIcon()
    data object None : ListIcon()
}

enum class BuiltInListIcon {
    Shield,
    Computer,
    Smartphone,
    Speaker,
    Devices,
    Block,
    Favorite,
    People,
    PlayCircle,
    SportsEsports,
    Casino,
    ShoppingBag,
    Chat,
    MusicNote,
    Folder,
    SignalCellular,
    Wifi,
}
