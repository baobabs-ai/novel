package infra.provider.providers

import infra.web.WebNovelAttention
import infra.web.WebNovelType
import koinExtensions
import infra.web.datasource.WebNovelHttpDataSource
import infra.web.datasource.providers.Kakuyomu
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant
import org.koin.test.KoinTest
import org.koin.test.inject


class KakuyomuTest : DescribeSpec(), KoinTest {
    override fun extensions() = koinExtensions()
    private val dataSource by inject<WebNovelHttpDataSource>()
    private val provider get() = dataSource.providers[Kakuyomu.id]!!

    init {
        describe("getRank") {
            it("Normal") {
                provider.getRank(mapOf("genre" to "Comprehensive", "range" to "Weekly"))
            }
        }

        describe("getMetadata") {
            it("Normal") {
                // https://kakuyomu.jp/works/1177354054892870623
                val metadata = provider.getMetadata("1177354054892870623")
                metadata.title.shouldBe("転生七女ではじめる異世界ライフ 〜万能魔力があれば貴族社会も余裕で生きられると聞いたのですが？！〜")
                metadata.authors.first().name.shouldBe("四葉夕卜")
                metadata.authors.first().link.shouldBe("https://kakuyomu.jp/users/yutoyotsuba")
                metadata.type.shouldBe(WebNovelType.Ongoing)
                metadata.attentions.shouldBe(listOf(WebNovelAttention.CruelDescription))
                metadata.keywords.shouldContain("マイペース女主人公")
                metadata.toc[0].title.shouldBe("Chapter 1 Atwood Family")
                metadata.toc[0].chapterId.shouldBeNull()
                metadata.toc[0].createAt.shouldBeNull()
                metadata.toc[1].title.shouldBe("Prologue")
                metadata.toc[1].chapterId.shouldBe("1177354054892870701")
                metadata.toc[1].createAt.shouldBe(Instant.parse("2019-12-10T03:00:12Z"))
            }
            it("Multi-level directory") {
                // https://kakuyomu.jp/works/16816452218603293395
                val metadata = provider.getMetadata("16816452218603293395")
                metadata.toc[0].title.shouldBe("高天原の神々とまつろわぬ者たち")
                metadata.toc[0].chapterId.shouldBeNull()
                metadata.toc[0].createAt.shouldBeNull()
                metadata.toc[1].title.shouldBe("第一章　人形のように美しい少女は舶来品")
                metadata.toc[1].chapterId.shouldBeNull()
                metadata.toc[1].createAt.shouldBeNull()
                metadata.toc[2].title.shouldBe("第1話　陰キャ男と健気な少女の微妙な関係")
                metadata.toc[2].chapterId.shouldBe("16816452218603528179")
                metadata.toc[2].createAt.shouldBe(Instant.parse("2021-02-13T08:47:34Z"))
            }
            it("Strange format, title is gone") {
                // https://kakuyomu.jp/works/1177354054891338293
                val metadata = provider.getMetadata("1177354054891338293")
                metadata.title.shouldBe("君は死ねない灰かぶりの魔女")
            }
        }

        describe("getEpisode") {
            it("Normal") {
                // https://kakuyomu.jp/works/16817139555217983105/episodes/16817139555218610896
                val episode = provider.getChapter("16817139555217983105", "16817139555218610896")
                episode.paragraphs.first().shouldBe("「あ～あ、女の子と甘酸っぱい青春を送りたいなぁ！」")
                episode.paragraphs.last().shouldBe("　俺はしばらく、その痕を見つめて本来の目的を忘れてしまうのだった。")
            }
            it("ruby") {
                // https://books.fishhawk.top/novel/kakuyomu/16816700429176867477/16816700429181729028
                val episode = provider.getChapter("16816700429176867477", "16816700429181729028")
                episode.paragraphs[22].shouldBe("　花咲くような無邪気な笑みを浮かべ、一生懸命に兄を呼ぶ姿。これを見て、心を打たれないものはいるだろうか。いや、いない！")
            }
        }
    }
}
