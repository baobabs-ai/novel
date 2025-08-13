package infra.wenku.datasource

import com.jillesvangurp.ktsearch.*
import com.jillesvangurp.searchdsls.querydsl.*
import infra.*
import infra.wenku.WenkuNovelFilter
import infra.wenku.WenkuNovelLevel
import infra.wenku.WenkuNovel
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import util.serialName

class WenkuNovelEsDataSource(
    private val es: ElasticSearchClient,
) {
    private val target = ElasticSearchIndexNames.WENKU_NOVEL

    @Serializable
    data class WenkuNovelMetadataEsModel(
        val id: String,
        val title: String,
        val titleEn: String,
        val cover: String?,
        val authors: List<String>,
        val artists: List<String>,
        val keywords: List<String>,
        val publisher: String?,
        val imprint: String?,
        @Contextual val latestPublishAt: Instant?,
        val level: WenkuNovelLevel,
        @Contextual val updateAt: Instant,
    )

    suspend fun searchNovel(
        userQuery: String?,
        page: Int,
        pageSize: Int,
        filterLevel: WenkuNovelFilter.Level,
    ): Pair<List<WenkuNovelMetadataEsModel>, Long> {
        val response = es.search(
            target = target,
            from = page * pageSize,
            size = pageSize
        ) {
            query = bool {
                val mustQueries = mutableListOf<ESQuery>()
                val mustNotQueries = mutableListOf<ESQuery>()

                // Filter level
                when (filterLevel) {
                    WenkuNovelFilter.Level.All -> null
                    WenkuNovelFilter.Level.ForAllAges -> WenkuNovelLevel.ForAllAges
                    WenkuNovelFilter.Level.ForAdults -> WenkuNovelLevel.ForAdults
                    WenkuNovelFilter.Level.Serious -> WenkuNovelLevel.Serious
                }?.let {
                    mustQueries.add(term(WenkuNovelMetadataEsModel::level, it.serialName()))
                }

                // Parse query
                val queryWords = mutableListOf<String>()
                userQuery
                    ?.split(" ")
                    ?.forEach { token ->
                        if (token.endsWith('$')) {
                            val rawToken = token.removePrefix("-").removeSuffix("$")
                            val queries =
                                if (token.startsWith("-")) mustNotQueries
                                else mustQueries
                            val field = WenkuNovelMetadataEsModel::keywords
                            queries.add(term(field, rawToken))
                        } else {
                            queryWords.add(token)
                        }
                    }

                filter(mustQueries)
                mustNot(mustNotQueries)

                if (queryWords.isNotEmpty()) {
                    must(
                        simpleQueryString(
                            queryWords.joinToString(" "),
                            WenkuNovelMetadataEsModel::title,
                            WenkuNovelMetadataEsModel::titleEn,
                            WenkuNovelMetadataEsModel::authors,
                            WenkuNovelMetadataEsModel::artists,
                            WenkuNovelMetadataEsModel::keywords,
                            WenkuNovelMetadataEsModel::publisher,
                            WenkuNovelMetadataEsModel::imprint,
                        ) {
                            defaultOperator = MatchOperator.AND
                        }
                    )
                } else {
                    sort {
                        add(WenkuNovelMetadataEsModel::updateAt)
                    }
                }
            }
        }
        val items = response.hits?.hits
            ?.map { hit -> hit.parseHit<WenkuNovelMetadataEsModel>() }
            ?: emptyList()
        val total = response.total
        return Pair(items, total)
    }

    suspend fun syncNovel(
        novel: WenkuNovel,
    ) {
        es.indexDocument(
            id = novel.id.toHexString(),
            target = target,
            document = WenkuNovelMetadataEsModel(
                id = novel.id.toHexString(),
                title = novel.title,
                titleEn = novel.titleEn,
                cover = novel.cover,
                authors = novel.authors,
                artists = novel.artists,
                keywords = novel.keywords,
                publisher = novel.publisher,
                imprint = novel.imprint,
                latestPublishAt = novel.latestPublishAt,
                level = novel.level,
                updateAt = novel.updateAt,
            ),
            refresh = Refresh.WaitFor,
        )
    }

    suspend fun deleteNovel(id: String) {
        es.deleteDocument(id = id, target = target)
    }
}