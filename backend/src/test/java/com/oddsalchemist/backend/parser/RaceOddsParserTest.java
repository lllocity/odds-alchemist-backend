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
                            <td class="horse-name">イクイノックス</td>
                            <td class="win-odds">1.8</td>
                            <td class="place-odds">1.1 - 1.3</td>
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

        assertThat(result).hasSize(1);
        OddsData horse3 = result.get(0);
        
        assertThat(horse3.winOdds()).isNull();
        assertThat(horse3.placeOddsMin()).isNull();
        assertThat(horse3.placeOddsMax()).isNull();
    }
}