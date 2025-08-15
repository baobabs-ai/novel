package infra.provider.providers

import infra.web.WebNovelAttention
import infra.web.WebNovelType
import koinExtensions
import infra.web.datasource.WebNovelHttpDataSource
import infra.web.datasource.providers.Hameln
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.datetime.Instant
import org.koin.test.KoinTest
import org.koin.test.inject

class HamelnTest : DescribeSpec(), KoinTest {
    override fun extensions() = koinExtensions()
    private val dataSource by inject<WebNovelHttpDataSource>()
    private val provider get() = dataSource.providers[Hameln.id]!!

    init {
        describe("getMetadata") {
            it("Normal") {
                // https://syosetu.org/novel/232822
                val metadata = provider.getMetadata("232822")
                metadata.title.shouldBe("和風ファンタジーな鬱エロゲーの名無し戦闘員に転生したんだが周囲の女がヤベー奴ばかりで嫌な予感しかしない件")
                metadata.introduction.shouldStartWith("どうやら和風ファンタジーゲ")
                metadata.type.shouldBe(WebNovelType.Ongoing)
                metadata.attentions.shouldContain(WebNovelAttention.R15)
                metadata.keywords.shouldContain("妖")
                metadata.toc[0].title.shouldBe("Character introduction, short stories, etc.")
                metadata.toc[0].chapterId.shouldBeNull()
                metadata.toc[0].createAt.shouldBeNull()
                metadata.toc[1].title.shouldBe("Character introduction (provisional)")
                metadata.toc[1].chapterId.shouldBe("1")
                metadata.toc[1].createAt.shouldBe(Instant.parse("2021-06-19T22:00:00Z"))
            }
            it("Normal, author has no link") {
                // https://syosetu.org/novel/305149
                val author = provider.getMetadata("305149").authors.first()
                author.name.shouldBe("文章修行僧")
                author.link.shouldBeNull()
            }
            it("Normal, author has a link") {
                // https://syosetu.org/novel/304380
                val author = provider.getMetadata("304380").authors.first()
                author.name.shouldBe("かりん2022")
                author.link.shouldBe("https://syosetu.org/user/347335/")
            }
            it("Normal, R18 redirect") {
                // https://syosetu.org/novel/94938
                val metadata = provider.getMetadata("94938")
                metadata.title.shouldBe("オズの国のドロシー")
            }
            it("Normal, evaluation is empty") {
                // https://syosetu.org/novel/274708
                val metadata = provider.getMetadata("274708")
                metadata.points.shouldBeNull()
            }
            it("Short Story") {
                // https://syosetu.org/novel/303189
                val metadata = provider.getMetadata("303189")
                metadata.type.shouldBe(WebNovelType.ShortStory)
            }
        }

        describe("getChapter") {
            it("Normal") {
                // https://syosetu.org/novel/321515/1.html
                val chapter = provider.getChapter("321515", "1")
                chapter.paragraphs[6].shouldBe("　『お父様』と呼ばれた男性は、十代後半から二十代前半程度の若い青年。『かぐや』と呼ばれた女性は、五歳程度の幼子であった。")
            }
            it("Short Story") {
                // https://syosetu.org/novel/303596
                val chapter = provider.getChapter("303596", "default")
                chapter.paragraphs.size.shouldBe(141)
                chapter.paragraphs.first().shouldStartWith("　特級呪霊花御による、呪術高専東京校への襲撃")
            }
            it("Strange format") {
                // https://syosetu.org/novel/299011/72.html
                val chapter = provider.getChapter("299011", "72")
                chapter.paragraphs.size.shouldBe(323)
            }
        }
    }
}
