package infra.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class NovelFileMode {
    @SerialName("jp")
    Jp,

    @SerialName("en")
    En,

    @SerialName("jp-en")
    JpEn,

    @SerialName("en-jp")
    EnJp,
}

@Serializable
enum class NovelFileTranslationsMode {
    @SerialName("parallel")
    Parallel,

    @SerialName("priority")
    Priority,
}

@Serializable
enum class NovelFileType(val value: String) {
    @SerialName("epub")
    EPUB("epub"),

    @SerialName("txt")
    TXT("txt")
}
