package infra.wenku.repository

import infra.TempFileClient
import infra.TempFileType
import infra.common.NovelFileMode
import infra.common.NovelFileTranslationsMode
import infra.common.TranslatorId
import infra.wenku.WenkuNovelVolumeList
import infra.wenku.datasource.WenkuNovelVolumeDiskDataSource
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import util.epub.Epub
import util.serialName
import java.security.MessageDigest
import kotlin.io.path.*

private fun String.escapePath() =
    replace('/', '.')

class WenkuNovelVolumeRepository(
    private val fs: WenkuNovelVolumeDiskDataSource,
    private val temp: TempFileClient,
) {
    private fun volumesDir(novelId: String) =
        Path("./data/files-wenku/${novelId}")

    suspend fun list(novelId: String): WenkuNovelVolumeList =
        fs.listVolumes(volumesDir(novelId))

    suspend fun createVolume(
        novelId: String,
        volumeId: String,
        inputStream: ByteReadChannel,
        unpack: Boolean,
    ) = fs.createVolume(
        volumesDir = volumesDir(novelId),
        volumeId = volumeId,
        inputStream = inputStream,
        unpack = unpack,
    )

    suspend fun deleteVolume(
        novelId: String,
        volumeId: String,
    ) = fs.deleteVolume(
        volumesDir = volumesDir(novelId),
        volumeId = volumeId,
    )

    suspend fun getVolume(
        novelId: String,
        volumeId: String,
    ) = fs.getVolume(
        volumesDir = volumesDir(novelId),
        volumeId = volumeId,
    )

    suspend fun makeTranslationVolumeFile(
        novelId: String,
        volumeId: String,
        mode: NovelFileMode,
        translationsMode: NovelFileTranslationsMode,
        translations: List<TranslatorId>,
    ) = withContext(Dispatchers.IO) {
        val volume = getVolume(novelId, volumeId)
            ?: return@withContext null

        fun md5(input: String): String {
            val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray())
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        }

        val zhFilename = buildString {
            append(novelId)
            append('.')
            append(mode.serialName())
            append('.')
            append(
                when (translationsMode) {
                    NovelFileTranslationsMode.Parallel -> "B"
                    NovelFileTranslationsMode.Priority -> "Y"
                }
            )
            translations.forEach {
                append(it.serialName()[0])
            }
            append('.')
            append(md5(volumeId))
            append('.')
            append(volumeId.substringAfterLast('.', "txt"))
        }

        val enPath = temp.createFile(TempFileType.Wenku, zhFilename)

        suspend fun getEnLinesList(chapterId: String): List<List<String>> {
            return when (translationsMode) {
                NovelFileTranslationsMode.Parallel ->
                    translations.mapNotNull { volume.getTranslation(it, chapterId) }

                NovelFileTranslationsMode.Priority ->
                    translations.firstNotNullOfOrNull { volume.getTranslation(it, chapterId) }
                        ?.let { listOf(it) }
                        ?: emptyList()
            }
        }

        if (volumeId.endsWith(".txt")) {
            enPath.bufferedWriter().use { bf ->
                volume.listChapter().sorted().forEach { chapterId ->
                    val enLinesList = getEnLinesList(chapterId)
                    if (enLinesList.isEmpty()) {
                        bf.appendLine("// This segment of translation is missing.")
                    } else {
                        val jpLines = volume.getChapter(chapterId)!!
                        val linesList = when (mode) {
                            NovelFileMode.Jp -> throw RuntimeException("Japanese download is not allowed for Wenku novels")
                            NovelFileMode.En -> enLinesList
                            NovelFileMode.JpEn -> listOf(jpLines) + enLinesList
                            NovelFileMode.EnJp -> enLinesList + listOf(jpLines)
                        }
                        for (i in jpLines.indices) {
                            linesList.forEach { lines ->
                                bf.appendLine(lines[i])
                            }
                        }
                    }
                }
            }
            return@withContext zhFilename
        } else {
            val jpPath = volume.volumesDir / volumeId

            val chapters = volume.listChapter()
            Epub.modify(srcPath = jpPath, dstPath = enPath) { name, bytesIn ->
                // To be compatible with the old format of ChapterId starting with a slash
                val chapterId = if ("/${name}".escapePath() in chapters) {
                    "/${name}".escapePath()
                } else if (name.escapePath() in chapters) {
                    name.escapePath()
                } else {
                    null
                }

                if (chapterId != null) {
                    // XHtml file, try to generate a translated version
                    val enLinesList = getEnLinesList(chapterId)
                    if (enLinesList.isEmpty()) {
                        bytesIn
                    } else {
                        val doc = Jsoup.parse(bytesIn.decodeToString(), Parser.xmlParser())
                        doc.select("p")
                            .filter { el -> el.text().isNotBlank() }
                            .forEachIndexed { index, el ->
                                when (mode) {
                                    NovelFileMode.Jp -> throw RuntimeException("Japanese download is not allowed for Wenku novels")
                                    NovelFileMode.En -> {
                                        enLinesList.forEach { lines ->
                                            el.before("<p>${lines[index]}</p>")
                                        }
                                        el.remove()
                                    }

                                    NovelFileMode.JpEn -> {
                                        enLinesList.asReversed().forEach { lines ->
                                            el.after("<p>${lines[index]}</p>")
                                        }
                                        el.attr("style", "opacity:0.4;")
                                    }

                                    NovelFileMode.EnJp -> {
                                        enLinesList.forEach { lines ->
                                            el.before("<p>${lines[index]}</p>")
                                        }
                                        el.attr("style", "opacity:0.4;")
                                    }
                                }
                            }
                        doc.outputSettings().prettyPrint(true)
                        doc.html().toByteArray()
                    }
                } else if (name.endsWith("opf")) {
                    val doc = Jsoup.parse(bytesIn.decodeToString(), Parser.xmlParser())

                    val metadataEl = doc.selectFirst("metadata")!!
                    val spineEl = doc.selectFirst("spine")!!

                    // Change EPUB language to English so that iOS iBook reader can use English fonts
                    metadataEl.selectFirst("dc|language")
                        ?.text("en-US")
                        ?: metadataEl.appendChild(
                            Element("dc:language").text("en-US")
                        )

                    // Prevent the reader from using vertical text
                    val metaNode = Element("meta")
                        .attr("name", "primary-writing-mode")
                        .attr("content", "horizontal-lr")
                    metadataEl.selectFirst("meta[name=primary-writing-mode]")
                        ?.replaceWith(metaNode)
                        ?: metadataEl.appendChild(metaNode)

                    spineEl.removeAttr("page-progression-direction")

                    doc.outputSettings().prettyPrint(true)
                    doc.html().toByteArray()
                } else if (name.endsWith("css")) {
                    "".toByteArray()
                } else {
                    bytesIn
                }
            }
            return@withContext zhFilename
        }
    }
}
