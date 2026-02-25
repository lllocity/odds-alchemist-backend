package com.oddsalchemist.backend.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class GoogleSheetsService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsService.class);
    private static final String VALUE_INPUT_OPTION = "USER_ENTERED";
    private final Sheets sheetsService;
    private final String spreadsheetId;

    public GoogleSheetsService(
            Sheets sheetsService,
            @Value("${google.sheets.spreadsheet-id}") String spreadsheetId) {
        this.sheetsService = sheetsService;
        this.spreadsheetId = spreadsheetId;
    }

    /**
     * スプレッドシートの指定レンジにデータを追記します。
     */
    public void appendData(String range, List<List<Object>> values) throws IOException {
        ValueRange body = new ValueRange().setValues(values);

        AppendValuesResponse result = sheetsService.spreadsheets().values()
                .append(spreadsheetId, range, body)
                .setValueInputOption(VALUE_INPUT_OPTION)
                .execute();

        if (result != null && result.getUpdates() != null) {
            logger.info("スプレッドシートへの書き込み完了。更新されたセル数: {}",
                    result.getUpdates().getUpdatedCells());
        }
    }
}