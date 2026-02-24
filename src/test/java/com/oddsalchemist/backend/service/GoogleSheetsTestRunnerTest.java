package com.oddsalchemist.backend.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleSheetsTestRunnerTest {

    @Mock
    private Sheets sheetsService;

    @Mock
    private OddsScrapingService scrapingService;

    // Google Sheets API のチェーンメソッド用モック
    @Mock
    private Sheets.Spreadsheets spreadsheets;
    @Mock
    private Sheets.Spreadsheets.Values values;
    @Mock
    private Sheets.Spreadsheets.Values.Append append;

    private GoogleSheetsTestRunner googleSheetsTestRunner;
    private final String spreadsheetId = "test-spreadsheet-id";

    @BeforeEach
    void setUp() {
        // テスト対象クラスのインスタンス化（モックを注入）
        googleSheetsTestRunner = new GoogleSheetsTestRunner(sheetsService, spreadsheetId, scrapingService);
    }

    @Test
    @DisplayName("runメソッド実行時に、Sheets APIへの書き込みとスクレイピング処理が呼び出されること")
    void testRun_Success() throws Exception {
        // --- モックの振る舞い定義 (Stubbing) ---
        
        // sheetsService.spreadsheets().values().append(...).setValueInputOption(...).execute() のチェーンを再現
        when(sheetsService.spreadsheets()).thenReturn(spreadsheets);
        when(spreadsheets.values()).thenReturn(values);
        
        // append メソッドは引数（spreadsheetId, range, body）を取るため、適切なマッチャーを使用
        when(values.append(eq(spreadsheetId), anyString(), any(ValueRange.class))).thenReturn(append);
        
        // setValueInputOption も自分自身(append)を返すビルダーパターン
        when(append.setValueInputOption("USER_ENTERED")).thenReturn(append);
        
        // execute の戻り値（今回は使わないが null でないものを返しておく）
        when(append.execute()).thenReturn(new AppendValuesResponse());

        // --- テスト実行 ---
        googleSheetsTestRunner.run();

        // --- 検証 (Verification) ---
        // 1. Google Sheets API が正しいパラメータで呼び出されたか
        verify(values).append(eq(spreadsheetId), eq("シート1!A:F"), any(ValueRange.class));
        verify(append).setValueInputOption("USER_ENTERED");
        verify(append).execute();

        // 2. スクレイピングサービスが呼び出されたか
        verify(scrapingService, times(1)).testFetch("https://db.netkeiba.com/");
    }
}