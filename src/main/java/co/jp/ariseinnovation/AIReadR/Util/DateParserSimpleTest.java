package co.jp.ariseinnovation.AIReadR.Util;

import org.joda.time.DateTime;

/**
 * DateParserの動作をテストするためのシンプルなクラス
 */
public class DateParserSimpleTest {
    
    public static void main(String[] args) {
        System.out.println("DateParserの動作テストを開始します...");
        
        // テストする和暦データ
        String[] testDates = {
            "H10.5",    // 平成10年5月
            "H21.6",    // 平成21年6月
            "H10.3",    // 平成10年3月
            "H11.8",    // 平成11年8月
            "H10 5",    // 平成10年5月（スペース区切り）
            "H21. 6",   // 平成21年6月（スペース区切り）
            "H106",     // 平成10年6月
            "令和6.1.1", // 令和6年1月1日
            "令6.1",    // 令和6年1月
            "R61"       // 令和6年1月
        };
        
        for (String testDate : testDates) {
            try {
                System.out.print("テスト: " + testDate + " → ");
                DateTime parsedDate = DateParser.Parse(testDate);
                System.out.println(parsedDate.toString("yyyy-MM-dd"));
            } catch (Exception e) {
                System.out.println("エラー: " + e.getMessage());
            }
        }
        
        System.out.println("テスト完了");
    }
}
