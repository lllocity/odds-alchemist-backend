package com.oddsalchemist.backend.parser;

import com.oddsalchemist.backend.dto.OddsData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RaceOddsParserTest {

    private RaceOddsParser parser;

    @BeforeEach
    void setUp() {
        parser = new RaceOddsParser();
    }

    @Test
    void parse_正常なHTMLからオッズ情報を抽出できること() {
        String html = """
            <html>
                <body>
                    <table class="odds-table">
                        <tr class="horse-row">
                            <td class="horse-number">1</td>
                            <td class="horse-name">キタサンブラック</td>
                            <td class="win-odds">2.5</td>
                            <td class="place-odds">1.2 - 1.5</td>
                        </tr>
                        <tr class="horse-row">
                            <td class="horse-number">2</td>
                            <td class="horse-name"><a href="/horse/2">イクイノックス</a></td>
                            <td class="win-odds">1.8</td>
                            <td class="place-odds">1.1-1.3</td>
                        </tr>
                    </table>
                </body>
            </html>
            """;

        List<OddsData> result = parser.parse(html);

        assertThat(result).hasSize(2);

        OddsData horse1 = result.get(0);
        assertThat(horse1.horseNumber()).isEqualTo("1");
        assertThat(horse1.horseName()).isEqualTo("キタサンブラック");
        assertThat(horse1.winOdds()).isEqualTo(2.5);
        assertThat(horse1.placeOddsMin()).isEqualTo(1.2);
        assertThat(horse1.placeOddsMax()).isEqualTo(1.5);

        OddsData horse2 = result.get(1);
        assertThat(horse2.horseNumber()).isEqualTo("2");
        assertThat(horse2.horseName()).isEqualTo("イクイノックス"); // リンクテキストが優先される
        assertThat(horse2.winOdds()).isEqualTo(1.8);
        assertThat(horse2.placeOddsMin()).isEqualTo(1.1);
        assertThat(horse2.placeOddsMax()).isEqualTo(1.3);
    }

    @Test
    void parse_オッズが未定や欠損している場合でもパースを継続できること() {
        String html = """
            <html>
                <body>
                    <table class="odds-table">
                        <tr class="horse-row">
                            <td class="horse-number">3</td>
                            <td class="horse-name">タイトルホルダー</td>
                            <td class="win-odds">---</td>
                            <td class="place-odds"></td>
                        </tr>
                    </table>
                </body>
            </html>
            """;

        List<OddsData> result = parser.parse(html);

        // オッズが未定でも馬番と馬名があれば行を含める
        assertThat(result).hasSize(1);
        OddsData horse3 = result.get(0);
        assertThat(horse3.horseNumber()).isEqualTo("3");
        assertThat(horse3.horseName()).isEqualTo("タイトルホルダー");
        assertThat(horse3.winOdds()).isNull();
        assertThat(horse3.placeOddsMin()).isNull();
        assertThat(horse3.placeOddsMax()).isNull();
    }

    @Test
    void parse_テーブルが空の場合は空リストが返ること() {
        String html = """
            <html>
                <body>
                    <table class="odds-table">
                    </table>
                </body>
            </html>
            """;

        List<OddsData> result = parser.parse(html);

        assertThat(result).isEmpty();
    }

    @Test
    void parse_ヘッダー行など馬番のない行はスキップされること() {
        String html = """
            <html>
                <body>
                    <table class="odds-table">
                        <tr>
                            <th>馬番</th>
                            <th>馬名</th>
                            <th>単勝</th>
                            <th>複勝</th>
                        </tr>
                        <tr class="horse-row">
                            <td>5</td>
                            <td>テスト馬</td>
                            <td>3.2</td>
                            <td>1.4-1.8</td>
                        </tr>
                        <tr>
                            <td>合計</td>
                            <td>-</td>
                            <td>-</td>
                            <td>-</td>
                        </tr>
                    </table>
                </body>
            </html>
            """;

        List<OddsData> result = parser.parse(html);

        // ヘッダー行（th要素のみ）と非数値の行はスキップ、有効な馬行のみ含まれる
        assertThat(result).hasSize(1);
        assertThat(result.get(0).horseNumber()).isEqualTo("5");
        assertThat(result.get(0).horseName()).isEqualTo("テスト馬");
        assertThat(result.get(0).winOdds()).isEqualTo(3.2);
    }
}
