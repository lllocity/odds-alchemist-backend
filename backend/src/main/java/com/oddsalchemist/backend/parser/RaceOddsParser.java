package com.oddsalchemist.backend.parser;

import com.oddsalchemist.backend.dto.OddsData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RaceOddsParser {

    private static final Logger logger = LoggerFactory.getLogger(RaceOddsParser.class);

    // 馬番: 1〜2桁の整数のみ（1〜18）
    private static final Pattern HORSE_NUMBER_PATTERN = Pattern.compile("^\\d{1,2}$");
    // 単勝オッズ: "5.4" 形式
    private static final Pattern WIN_ODDS_PATTERN = Pattern.compile("^\\d+\\.\\d+$");
    // 複勝オッズ: "1.2 - 1.5" または "1.2-1.5" 形式（キャプチャグループで値を取得）
    private static final Pattern PLACE_ODDS_PATTERN = Pattern.compile("(\\d+\\.\\d+)\\s*-\\s*(\\d+\\.\\d+)");

    public List<OddsData> parse(String html) {
        List<OddsData> oddsList = new ArrayList<>();
        Document doc = Jsoup.parse(html);
        Elements rows = doc.select("tr");

        for (Element row : rows) {
            try {
                OddsData data = parseRow(row);
                if (data != null) {
                    oddsList.add(data);
                }
            } catch (Exception e) {
                logger.warn("行のパースに失敗しました。スキップします: {}", e.getMessage());
            }
        }

        logger.info("パース完了: 有効な馬データ {}件 / スキャン総行数 {}行", oddsList.size(), rows.size());
        return oddsList;
    }

    private OddsData parseRow(Element row) {
        Elements cells = row.select("td");
        if (cells.size() < 2) return null;

        // 馬番を最初の3セル内から探す（1〜2桁の整数セル）
        String horseNumber = null;
        int horseNumberIndex = -1;
        for (int i = 0; i < Math.min(cells.size(), 3); i++) {
            String text = cells.get(i).text().trim();
            if (HORSE_NUMBER_PATTERN.matcher(text).matches()) {
                horseNumber = text;
                horseNumberIndex = i;
                break;
            }
        }
        if (horseNumber == null) return null;

        // 馬番の隣のセルを馬名として取得（リンクがあればそのテキストを優先）
        int nameIndex = horseNumberIndex + 1;
        if (nameIndex >= cells.size()) return null;
        Element nameCell = cells.get(nameIndex);
        Element nameLink = nameCell.selectFirst("a");
        String horseName = (nameLink != null ? nameLink.text() : nameCell.text()).trim();
        if (horseName.isEmpty()) return null;

        // 単勝・複勝オッズをパターンマッチで抽出
        Double winOdds = null;
        Double placeMin = null;
        Double placeMax = null;

        for (Element cell : cells) {
            String text = cell.text().trim();

            // 複勝オッズの判定を先に行う（"1.2-1.5" が単勝パターンにマッチしないよう）
            if (placeMin == null) {
                Matcher placeMatcher = PLACE_ODDS_PATTERN.matcher(text);
                if (placeMatcher.find()) {
                    placeMin = parseDouble(placeMatcher.group(1));
                    placeMax = parseDouble(placeMatcher.group(2));
                    continue;
                }
            }

            // 単勝オッズ: "5.4" 形式のセル
            if (winOdds == null && WIN_ODDS_PATTERN.matcher(text).matches()) {
                winOdds = parseDouble(text);
            }
        }

        // 馬番と馬名が揃っていれば、オッズが未定でも行を含める
        return new OddsData(horseNumber, horseName, winOdds, placeMin, placeMax);
    }

    private Double parseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            logger.warn("数値変換に失敗しました: '{}'", s);
            return null;
        }
    }
}
