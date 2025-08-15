package infra.web.datasource.providers

import infra.common.Page
import infra.common.emptyPage
import infra.web.WebNovelAttention
import infra.web.WebNovelAuthor
import infra.web.WebNovelType
import io.ktor.client.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jsoup.nodes.Document

class Syosetu(
    private val client: HttpClient,
) : WebNovelProvider {
    companion object {
        const val id = "syosetu"

        suspend fun addCookies(cookies: CookiesStorage) {
            cookies.addCookie(
                "https://ncode.syosetu.com/",
                Cookie(name = "over18", value = "yes", domain = ".syosetu.com")
            )
        }

        private val rangeIds = mapOf(
            "Daily" to "daily",
            "Weekly" to "weekly",
            "Monthly" to "monthly",
            "Quarterly" to "quarter",
            "Yearly" to "yearly",
            "Total" to "total",
        )
        private val statusIds = mapOf(
            "All" to "total",
            "Short Story" to "t",
            "Serializing" to "r",
            "Finished" to "er",
        )
        private val genreIdsV1 = mapOf(
            "Romance: Isekai" to "101",
            "Romance: Real World" to "102",
            "Fantasy: High Fantasy" to "201",
            "Fantasy: Low Fantasy" to "202",
            "Literature: Pure Literature" to "301",
            "Literature: Human Drama" to "302",
            "Literature: History" to "303",
            "Literature: Mystery" to "304",
            "Literature: Horror" to "305",
            "Literature: Action" to "306",
            "Literature: Comedy" to "307",
            "Sci-Fi: VR Game" to "401",
            "Sci-Fi: Space" to "402",
            "Sci-Fi: Science Fiction" to "403",
            "Sci-Fi: Thriller" to "404",
            "Other: Fairy Tale" to "9901",
            "Other: Poem" to "9902",
            "Other: Essay" to "9903",
            "Other: Other" to "9999",
        )
        private val genreIdsV2 = mapOf(
            "Romance" to "1",
            "Fantasy" to "2",
            "Literature/Sci-Fi/Other" to "o",
        )
    }

    override suspend fun getRank(options: Map<String, String>): Page<RemoteNovelListItem> {
        val genreFilter = options["genre"]
        val rangeFilter = options["range"] ?: return emptyPage()
        val statusFilter = options["status"] ?: return emptyPage()

        val rangeId = rangeIds[rangeFilter] ?: return emptyPage()
        val statusId = statusIds[statusFilter] ?: return emptyPage()

        val page = options["page"]?.toIntOrNull()?.plus(1) ?: 1

        val path = when (options["type"]) {
            "Genre" -> {
                val genreId = genreIdsV1[genreFilter] ?: return emptyPage()
                "genrelist/type/${rangeId}_${genreId}_${statusId}"
            }

            "Comprehensive" -> {
                "list/type/${rangeId}_${statusId}"
            }

            "Isekai Tensei/Ten'i" -> {
                val genreId = genreIdsV2[genreFilter] ?: return emptyPage()
                "isekailist/type/${rangeId}_${genreId}_${statusId}"
            }

            else -> return emptyPage()
        }

        val doc = client.get("https://yomou.syosetu.com/rank/$path/?p=${page}").document()

        val pageNumber = doc
            .getElementsByClass("c-pager")
            .first()!!
            .childrenSize() - 2

        val items = doc
            .getElementsByClass("p-ranklist-item")
            .map { item ->
                val elTitle = item.selectFirst("div.p-ranklist-item__title > a")!!
                val title = elTitle.text()
                val novelId = elTitle.attr("href")
                    .removeSuffix("/")
                    .substringAfterLast("/")

                val attentions = mutableListOf<WebNovelAttention>()
                val keywords = mutableListOf<String>()
                item
                    .selectFirst("div.p-ranklist-item__keyword")
                    ?.getElementsByTag("a")
                    ?.map { it.text() }
                    ?.forEach {
                        when (it) {
                            "R15" -> attentions.add(WebNovelAttention.R15)
                            "残酷な描写あり" -> attentions.add(WebNovelAttention.CruelDescription)
                            else -> keywords.add(it)
                        }
                    }

                val elPoints = item.selectFirst("div.p-ranklist-item__points")!!
                val elInfomation = item.selectFirst("div.p-ranklist-item__infomation")!!
                val extra = (listOf(elPoints) + elInfomation.getElementsByClass("p-ranklist-item__separator"))
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
            pageNumber = pageNumber.toLong(),
        )
    }

    override suspend fun getMetadata(novelId: String): RemoteNovelMetadata {
        val (doc1, doc2) = coroutineScope {
            val url1 = "https://ncode.syosetu.com/$novelId"
            val url2 = "https://ncode.syosetu.com/novelview/infotop/ncode/$novelId"
            return@coroutineScope listOf(
                async { client.get(url1).document() },
                async { client.get(url2).document() },
            ).awaitAll()
        }

        val title = doc2
            .selectFirst("h1")!!
            .text()

        val infodataEl = doc2.getElementsByClass("p-infotop-data").first()!!
        val infotypeEl = doc2.getElementsByClass("p-infotop-type").first()!!

        fun row(label: String) = infodataEl
            .selectFirst("dt:containsOwn(${label})")
            ?.nextElementSibling()

        val author = row("Author Name")!!
            .let { el ->
                WebNovelAuthor(
                    name = el.text(),
                    link = el.selectFirst("a")?.attr("href"),
                )
            }

        val type = infotypeEl.selectFirst(".p-infotop-type__type")!!
            .text()
            .let {
                when (it) {
                    "完結済" -> WebNovelType.Completed
                    "連載中" -> WebNovelType.Ongoing
                    "短編" -> WebNovelType.ShortStory
                    else -> throw RuntimeException("Unable to parse novel type:$it")
                }
            }

        val attentions = mutableSetOf<WebNovelAttention>()
        val keywords = mutableListOf<String>()
        row("Keywords")
            ?.text()
            ?.split(" ")
            ?.forEach {
                when (it) {
                    "R15" -> attentions.add(WebNovelAttention.R15)
                    "残酷な描写あり" -> attentions.add(WebNovelAttention.CruelDescription)
                    else -> keywords.add(it)
                }
            }
        infotypeEl
            .selectFirst(".p-infotop-type__r18")
            ?.text()
            ?.let {
                if (it == "R18") attentions.add(WebNovelAttention.R18)
                else throw RuntimeException("Unable to parse novel tag:$it")
            }

        val points = row("Overall Rating")
            ?.text()
            ?.filter { it.isDigit() }
            ?.toIntOrNull()

        val totalCharacters = row("Character Count")!!
            .text()
            .filter { it.isDigit() }
            .toInt()

        val introduction = row("Synopsis")!!
            .text()

        val toc = if (doc1.selectFirst("div.p-eplist") == null) {
            listOf(
                RemoteNovelMetadata.TocItem(
                    title = "Untitled",
                    chapterId = "default",
                )
            )
        } else {
            val totalPages = doc1
                .getElementsByClass("c-pager__item--last")
                .first()
                ?.attr("href")
                ?.substringAfterLast("/?p=")
                ?.toInt()
                ?: 1

            fun parseToc(doc: Document) = doc
                .selectFirst("div.p-eplist")!!
                .children()
                .map { child ->
                    child.selectFirst("a")?.let { a ->
                        RemoteNovelMetadata.TocItem(
                            title = a.text(),
                            chapterId = a.attr("href")
                                .removeSuffix("/")
                                .substringAfterLast("/"),
                            createAt = parseJapanDateString(
                                "yyyy/MM/dd HH:mm",
                                child.selectFirst("div.p-eplist__update")!!.textNodes()[0].text(),
                            )
                        )
                    } ?: RemoteNovelMetadata.TocItem(
                        title = child.text(),
                    )
                }

            val tocFirstPage = parseToc(doc1)
            val tocRemainingPages =
                (2..totalPages)
                    .map { page ->
                        val url = "https://ncode.syosetu.com/$novelId/?p=${page}"
                        val doc = client.get(url).document()
                        return@map parseToc(doc)
                    }
                    .flatten()
            tocFirstPage + tocRemainingPages
        }
        return RemoteNovelMetadata(
            title = title,
            authors = listOf(author),
            type = type,
            attentions = attentions.toList(),
            keywords = keywords,
            points = points,
            totalCharacters = totalCharacters,
            introduction = introduction,
            toc = toc,
        )
    }

    override suspend fun getChapter(novelId: String, chapterId: String): RemoteChapter {
        val url =
            if (chapterId == "default") "https://ncode.syosetu.com/$novelId"
            else "https://ncode.syosetu.com/$novelId/$chapterId"
        val doc = client.get(url).document()
        doc.select("rp").remove()
        doc.select("rt").remove()
        val paragraphs = doc.select("div.p-novel__body > div > p").map { p ->
            p
                .firstElementChild()
                ?.firstElementChild()
                ?.takeIf { it.tagName() == "img" }
                ?.let { "<Image>https:${it.attr("src")}" }
                ?: p.text()
        }
        return RemoteChapter(paragraphs = paragraphs)
    }
}