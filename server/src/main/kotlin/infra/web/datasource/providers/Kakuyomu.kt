package infra.web.datasource.providers

import infra.common.Page
import infra.common.emptyPage
import infra.web.WebNovelAttention
import infra.web.WebNovelAuthor
import infra.web.WebNovelType
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class Kakuyomu(
    private val client: HttpClient,
) : WebNovelProvider {
    companion object {
        const val id = "kakuyomu"
        private val rangeIds = mapOf(
            "Daily" to "daily",
            "Weekly" to "weekly",
            "Monthly" to "monthly",
            "Yearly" to "yearly",
            "Total" to "entire",
        )
        private val genreIds = mapOf(
            "Comprehensive" to "all",
            "Isekai Fantasy" to "fantasy",
            "Modern Fantasy" to "action",
            "Sci-Fi" to "sf",
            "Romance" to "love_story",
            "Romantic Comedy" to "romance",
            "Modern Drama" to "drama",
            "Horror" to "horror",
            "Mystery" to "mystery",
            "Essay/Non-fiction" to "nonfiction",
            "History/Era/Legend" to "history",
            "Creation Theory/Commentary" to "criticism",
            "Poem/Fairy Tale/Other" to "others",
        )
    }

    override suspend fun getRank(options: Map<String, String>): Page<RemoteNovelListItem> {
        val genre = genreIds[options["genre"]] ?: return emptyPage()
        val range = rangeIds[options["range"]] ?: return emptyPage()
        val url = "https://kakuyomu.jp/rankings/${genre}/${range}"
        val doc = client.get(url).document()
        val items = doc.select("div.widget-media-genresWorkList-right > div.widget-work").map { workCard ->
            val a = workCard.selectFirst("a.bookWalker-work-title")!!
            val novelId = a.attr("href").removePrefix("/works/")
            val title = a.text()

            val attentions = workCard
                .select("b.widget-workCard-flags > span")
                .mapNotNull {
                    when (it.text()) {
                        "残酷描写有り" -> WebNovelAttention.CruelDescription
                        "暴力描写有り" -> WebNovelAttention.ViolenceDescription
                        "性描写有り" -> WebNovelAttention.SexualDescription
                        else -> null
                    }
                }

            val keywords = workCard
                .select("span.widget-workCard-tags > a")
                .map { it.text() }

            val extra = workCard.selectFirst("p.widget-workCard-meta")!!.children()
                .joinToString(" / ") { it.text() }

            RemoteNovelListItem(
                novelId = novelId,
                title = title,
                attentions = attentions,
                keywords = keywords,
                extra = extra,
            )
        }
        return Page(
            items = items,
            pageNumber = 1L,
        )
    }

    override suspend fun getMetadata(novelId: String): RemoteNovelMetadata {
        val url = "https://kakuyomu.jp/works/$novelId"
        val doc = client.get(url).document()
        val script = doc.getElementById("__NEXT_DATA__")!!
        val apollo = Json
            .decodeFromString<JsonObject>(
                script.html()
            )["props"]!!
            .jsonObject["pageProps"]!!
            .jsonObject["__APOLLO_STATE__"]!!
            .jsonObject

        fun JsonObject.unref() = this["__ref"]!!
            .jsonPrimitive.content
            .let { apollo[it]!!.jsonObject }

        val work = apollo["Work:$novelId"]!!.jsonObject

        val title =
            work.stringOrNull("alternateTitle")
                ?: work.string("title")

        val author = work
            .obj("author")
            .unref()
            .let {
                WebNovelAuthor(
                    name = it.string("activityName"),
                    link = "https://kakuyomu.jp/users/${it.string("name")}",
                )
            }

        val type = when (val status = work.string("serialStatus")) {
            "COMPLETED" -> WebNovelType.Finished
            "RUNNING" -> WebNovelType.Ongoing
            else -> throw RuntimeException("Unable to parse novel type:$status")
        }

        val attentions = buildList {
            if (work.boolean("isCruel")) add(WebNovelAttention.CruelDescription)
            if (work.boolean("isViolent")) add(WebNovelAttention.ViolenceDescription)
            if (work.boolean("isSexual")) add(WebNovelAttention.SexualDescription)
        }

        val keywords = work
            .array("tagLabels")
            .map { it.jsonPrimitive.content }

        val points = work.int("totalReviewPoint")
        val totalCharacters = work.int("totalCharacterCount")

        val introduction = work.string("introduction")

        val toc = work
            .array("tableOfContents")
            .map { it.jsonObject.unref() }
            .flatMap { tableOfContentsChapter ->
                val chapter = tableOfContentsChapter
                    .objOrNull("chapter")
                    ?.unref()
                val episodes = tableOfContentsChapter
                    .array("episodeUnions")
                    .map { it.jsonObject.unref() }
                buildList {
                    if (chapter != null) {
                        add(
                            RemoteNovelMetadata.TocItem(
                                title = chapter.string("title"),
                            )
                        )
                    }
                    episodes.forEach {
                        add(
                            RemoteNovelMetadata.TocItem(
                                title = it.string("title"),
                                chapterId = it.string("id"),
                                createAt = Instant.parse(it.string("publishedAt")),
                            )
                        )
                    }
                }
            }

        return RemoteNovelMetadata(
            title = title,
            authors = listOf(author),
            type = type,
            attentions = attentions,
            keywords = keywords,
            points = points,
            totalCharacters = totalCharacters,
            introduction = introduction,
            toc = toc,
        )
    }

    override suspend fun getChapter(novelId: String, chapterId: String): RemoteChapter {
        val url = "https://kakuyomu.jp/works/$novelId/episodes/$chapterId"
        val doc = client.get(url).document()
        doc.select("rp").remove()
        doc.select("rt").remove()
        val paragraphs = doc.select("div.widget-episodeBody > p").map { it.text() }
        if (paragraphs.isEmpty()) {
            throw RuntimeException("Paid chapter, cannot be obtained")
        }
        return RemoteChapter(paragraphs = paragraphs)
    }
}