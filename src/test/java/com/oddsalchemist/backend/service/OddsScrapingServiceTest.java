package com.oddsalchemist.backend.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OddsScrapingServiceTest {

    private final OddsScrapingService oddsScrapingService = new OddsScrapingService();

    @Test
    @DisplayName("正常系: 指定したURLからHTMLを取得し、タイトルがログ出力されること")
    void fetchHtml_Success() throws IOException {
        String url = "https://db.netkeiba.com/";
        String expectedTitle = "テスト用タイトル";
        // テスト用の静的HTMLを作成し、Documentオブジェクトに変換
        String html = "<html><head><title>" + expectedTitle + "</title></head><body></body></html>";
        Document document = Jsoup.parse(html);

        // Jsoup.connect などの static メソッドをモック化
        try (MockedStatic<Jsoup> jsoupMock = Mockito.mockStatic(Jsoup.class)) {
            Connection connectionMock = mock(Connection.class);

            // Jsoup.connect(url) が呼ばれたら connectionMock を返す
            jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(connectionMock);
            // チェーンメソッドのモック化
            when(connectionMock.userAgent(anyString())).thenReturn(connectionMock);
            when(connectionMock.timeout(anyInt())).thenReturn(connectionMock);
            
            // connectionMock.get() が呼ばれたら用意した document を返す
            when(connectionMock.get()).thenReturn(document);

            // テスト実行
            String result = oddsScrapingService.fetchHtml(url);

            // 検証: Jsoup.connect と .get() が正しく呼び出されたか
            assertEquals(document.html(), result);
            jsoupMock.verify(() -> Jsoup.connect(url), times(1));
            verify(connectionMock).userAgent(anyString());
            verify(connectionMock).timeout(anyInt());
            verify(connectionMock, times(1)).get();
        }
    }

    @Test
    @DisplayName("異常系: 通信エラー発生時にIOExceptionがスローされること")
    void fetchHtml_Failure() throws IOException {
        String url = "https://error-url.com/";

        try (MockedStatic<Jsoup> jsoupMock = Mockito.mockStatic(Jsoup.class)) {
            Connection connectionMock = mock(Connection.class);

            jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(connectionMock);
            when(connectionMock.userAgent(anyString())).thenReturn(connectionMock);
            when(connectionMock.timeout(anyInt())).thenReturn(connectionMock);
            
            // .get() で IOException をスローするように設定
            when(connectionMock.get()).thenThrow(new IOException("Connection timed out"));

            // テスト実行と検証
            assertThrows(IOException.class, () -> oddsScrapingService.fetchHtml(url));
        }
    }
}