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

@Component
public class RaceOddsParser {

    private static final Logger logger = LoggerFactory.getLogger(RaceOddsParser.class);

    /**
     * HTML文字列からオッズ情報を抽出します。
     * ※ 注意: 以下のCSSセレクタ（".odds-table .horse-row" など）は仮のものです。
     * 実際にスクレイピングするサイトのDOM構造に合わせて書き換えてください。
     */
    private static final String SELECTOR_ROW = ".odds-table .horse-row";
    private static final String SELECTOR_HORSE_NUMBER = ".horse-number";
    private static final String SELECTOR_HORSE_NAME = ".horse-name";
    private static final String SELECTOR_WIN_ODDS = ".win-odds";
    private static final String SELECTOR_PLACE_ODDS = ".place-odds";

    public List<OddsData> parse(String html) {
        List<OddsData> oddsList = new ArrayList<>();
        Document doc = Jsoup.parse(html);

        Elements rows = doc.select(SELECTOR_ROW);

        for (Element row : rows) {
            try {
                String horseNumber = row.select(SELECTOR_HORSE_NUMBER).text().trim();
                String horseName = row.select(SELECTOR_HORSE_NAME).text().trim();
                String winOddsStr = row.select(SELECTOR_WIN_ODDS).text().trim();
                String placeOddsStr = row.select(SELECTOR_PLACE_ODDS).text().trim();

                Double winOdds = parseOdds(winOddsStr);
                Double[] placeOdds = parsePlaceOdds(placeOddsStr);

                oddsList.add(new OddsData(horseNumber, horseName, winOdds, placeOdds[0], placeOdds[1]));
            } catch (Exception e) {
                // 一部の行で抽出に失敗しても全体を止めずにスキップ
                logger.warn("Failed to parse row. html fragment: {}", row.outerHtml(), e);
            }
        }
        return oddsList;
    }

    private Double parseOdds(String oddsStr) {
        if (oddsStr == null || oddsStr.isEmpty() || oddsStr.contains("---")) {
            return null;
        }
        try {
            return Double.parseDouble(oddsStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double[] parsePlaceOdds(String placeOddsStr) {
        Double[] result = new Double[]{null, null};
        if (placeOddsStr == null || placeOddsStr.isEmpty()) return result;

        String[] parts = placeOddsStr.split("-");
        if (parts.length >= 1) result[0] = parseOdds(parts[0].trim());
        if (parts.length >= 2) result[1] = parseOdds(parts[1].trim());
        
        return result;
    }
}