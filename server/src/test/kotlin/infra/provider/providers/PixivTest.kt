package infra.provider.providers

import koinExtensions
import infra.web.datasource.WebNovelHttpDataSource
import infra.web.datasource.providers.NovelIdShouldBeReplacedException
import infra.web.datasource.providers.Pixiv
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.koin.test.KoinTest
import org.koin.test.inject

class PixivTest : DescribeSpec(), KoinTest {
    override fun extensions() = koinExtensions()
    private val dataSource by inject<WebNovelHttpDataSource>()
    private val provider get() = dataSource.providers[Pixiv.id]!!

    init {
        describe("getMetadata") {
            it("Normal") {
                // https://www.pixiv.net/novel/series/9406879
                val metadata = provider.getMetadata("9406879")
                metadata.title.shouldBe("メス堕ちシリーズ")
                metadata.authors.first().name.shouldBe("から")
                metadata.authors.first().link.shouldBe("https://www.pixiv.net/users/46135214")
                metadata.introduction.shouldBe(
                    "男子高校生が振った同級生の友達から復讐されるお話です。\n" +
                            "TSF、女装無理な方はブラウザバック推奨です。"
                )
                metadata.toc[0].title.shouldBe("女装→メス堕ち→TSF")
                metadata.toc[0].chapterId.shouldBe("18304868")
                metadata.toc[1].title.shouldBe("女装→メス堕ち→TSF")
                metadata.toc[1].chapterId.shouldBe("18457702")
            }
            it("Normal, chapter tags combined") {
                // https://www.pixiv.net/novel/series/898392
                val metadata = provider.getMetadata("898392")
                metadata.keywords.shouldNotBeEmpty()
            }
            it("Normal, non-R18") {
                // https://www.pixiv.net/novel/series/10539710
                val metadata = provider.getMetadata("10539710")
            }
            it("Normal, friends only") {
                // https://www.pixiv.net/novel/series/642636
                val metadata = provider.getMetadata("642636")
            }
            it("Normal, many pages in table of contents") {
                // https://www.pixiv.net/novel/series/870363
                val metadata = provider.getMetadata("870363")
                metadata.toc[0].title.shouldBe("【番外編】強運少女は夢をかなえた？")
                metadata.toc[0].chapterId.shouldBe("8592479")
            }
            it("Short Story") {
                // https://www.pixiv.net/novel/show.php?id=19776346
                val metadata = provider.getMetadata("s19776346")
                metadata.title.shouldBe("メカクレ青年が全自動矯正機で熟女にとりさんにされる話")
                metadata.authors.first().name.shouldBe("たれ")
                metadata.authors.first().link.shouldBe("https://www.pixiv.net/users/60498514")
                metadata.toc[0].title.shouldBe("Untitled")
                metadata.toc[0].chapterId.shouldBe("19776346")
            }
            it("Short story, but part of a series") {
                // https://www.pixiv.net/novel/show.php?id=18304868
                shouldThrow<NovelIdShouldBeReplacedException> {
                    provider.getMetadata("s18304868")
                }
            }
        }

        describe("getEpisode") {
            it("Normal") {
                // https://www.pixiv.net/novel/show.php?id=18304868
                val chapter = provider.getChapter("9406879", "18304868")
                chapter.paragraphs.first().shouldBe("「私と付き合ってください。」")
                chapter.paragraphs.last().shouldBe("俺はそれに従うしかなかった。")
            }
            it("Normal, multi-page") {
                // https://www.pixiv.net/novel/show.php?id=18199707
                val chapter = provider.getChapter("870363", "18199707")
                chapter.paragraphs.first().shouldBe("　｢ふわぁ～あ～……久々に凄い暇で眠いな～……｣")
                chapter.paragraphs.last().shouldBe("　風香の叫びが青空に響いた。")
            }
            it("Illustration mode 1") {
                // https://www.pixiv.net/novel/show.php?id=10723739
                // [uploadedimage:42286]
                val chapter = provider.getChapter("s10723739", "10723739")
                val line = chapter.paragraphs.find { it.startsWith("<Image>") }
                line.shouldBe("<Image>https://i.pximg.net/novel-cover-original/img/2021/04/29/09/02/19/tei682410579700_b92972c74f71d56a7c436837c4f5b959.jpg")
            }
            it("Illustration mode 2") {
                // https://www.pixiv.net/novel/show.php?id=2894162
                // [pixivimage:38959194]
                val chapter = provider.getChapter("222297", "2894162")
                val line = chapter.paragraphs.find { it.startsWith("<Image>") }
                line.shouldBe("<Image>https://i.pximg.net/img-original/img/2013/10/06/21/04/43/38959194_p0.jpg")
            }
            it("ruby") {
                // https://www.pixiv.net/novel/show.php?id=10618179
                // [[rb:久世彩葉 > くぜ いろは]]
                val chapter = provider.getChapter("s10618179", "10618179")
                println(chapter.paragraphs[2])
                chapter.paragraphs[2].shouldStartWith("　私、久世彩葉がその")
            }
        }
    }
}
