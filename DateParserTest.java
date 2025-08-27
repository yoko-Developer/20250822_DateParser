package co.jp.ariseinnovation.AIReadR.Util;

import co.jp.ariseinnovation.AIReadR.Exception.FormatException;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.context.annotation.Description;

import java.util.LinkedHashMap;

import static org.junit.Assert.*;

public class DateParserTest {
	final static LinkedHashMap<String, String> INPUT_DATE = new LinkedHashMap<String, String>()
    {{
        //帳票を見て想像できる限りのテスト
        put("20180102","2018-01-02");//区切り文字なし8桁
        put("２０１８０３０４","2018-03-04");//区切り文字なし全角
        put("2018.5.6","2018-05-06");//ドット区切り
        put("2018.07.08","2018-07-08");//ドット区切り0付き
        put("２０１８.０９.１０","2018-09-10");//ドット区切り全角
        put("20１８.0１.０2","2018-01-02");//ドット区切り全角半角混合
        put("2018/3/4","2018-03-04");//スラッシュ区切り
        put("2018-5-6","2018-05-06");//ハイフン区切り
        put("2018年7月8日","2018-07-08");//年月日区切り
        put("2021 1 1", "2021-01-01");//スペース区切り
        put("2021年1/1", "2021-01-01");//区切り文字混合
        put("２０１８年９月１０日","2018-09-10");//年月日区切り全角
        put("２０１８年０１月０２日","2018-01-02");//年月日区切り全角０付き
        put("昭和５６年２月３日","1981-02-03");//昭和対応
        put("Ｓ５７年２月３日","1982-02-03");//昭和対応
        put("S５８年２月３日","1983-02-03");//昭和対応
        put("平成２８年７月８日","2016-07-08");//平成対応
        put("Ｈ２９年７月８日","2017-07-08");//平成対応
        put("H３０年７月８日","2018-07-08");//平成対応
        put("令和元年９月１０日","2019-09-10");//令和対応
        put("Ｒ１年５月１０日","2019-05-10");//令和対応
        put("R2年９月１０日","2020-09-10");//令和対応
        put("令和元年一一月二四日","2019-11-24");//漢数字対応
        put("令和十年一〇月二四日","2028-10-24");//漢数字対応
        put("令和二十年十二月三十一日","2038-12-31");//漢数字対応
        put("2020年９月末日","2020-09-30");//末日
        put("2.1", DateTime.now().getYear() + "-02-01");//年なし月日
        put("2021.1", "2021-01-01");//日なし4桁西暦年月
        put("平成30.2", "2018-02-01");//日なし元号付き年月
        put("20.03.01", "2020-03-01");//和暦なし下2桁の年で西暦の方が近い場合
        put("2.3.31",  "2020-03-31");//和暦なし下2桁の年で和暦の方が近い場合
        put("200430", "2020-04-30");//区切り文字なし6桁で西暦の方が近い場合
        put("020531", "2020-05-31");//区切り文字なし6桁で和暦の方が近い場合
        put("2.1.2020", "2020-02-01");//月日年順ドット区切り
        put("20 20 04 15", "2020-04-15");//過剰スペース区切り8桁
        put("yyyy年5月10日", DateTime.now().getYear() + "-05-10");
        put("2000/02/29","2000-02-29");//閏年
        put("令和十年２月２９日","2028-02-29");//閏年
        put("令和六年二月二十九日","2024-02-29");//閏年
        // 以下、ロジックから抽出されるサンプリングテスト.
        // 数字が全て連なっている文字
        put("21", "2020-01-01");//2文字
        put("21末日", "2020-01-31");//2文字末日付き
        put("昭和21末日", "1927-01-31");//2文字末日付き
        put("昭和元年1末日", "1926-01-31");//元年1文字末日付き
        put("平成元年1末日", "1989-01-31");//元年1文字末日付き
        put("11", "2019-01-01");//2文字。正しくないかもしれないけれど、変な動きにならないことの確認。
        put("11末日", "2019-01-31");//2文字末日付き。正しくないかもしれないけれど、変な動きにならないことの確認。
        put("111", "2019-11-01");//3文字。正しくないかもしれないけれど、変な動きにならないことの確認。
        put("1111", "2029-11-01");//4文字末。正しくないかもしれないけれど、変な動きにならないことの確認。
        put("0111", "2019-11-01");//4文字末。正しくないかもしれないけれど、変な動きにならないことの確認。
        put("1111末日", "2029-11-30");//4文字末日付き。正しくないかもしれないけれど、変な動きにならないことの確認。
        put("令和1111末日", "2029-11-30");//4文字元号末日付き。正しくないかもしれないけれど、変な動きにならないことの確認。
        put("令和2112", "2039-12-01");//4文字元号付き。正しくないかもしれないけれど、変な動きにならないことの確認。
        put("40122", "2022-01-22");//5文字付き。年1桁
        put("18102", "2018-10-02");//5文字年2桁
        put("昭和40122", "1929-01-22");//5文字元号付き。
        put("201129", "2020-11-29");//6文字付き。
        put("平成101201", "1998-12-01");//6文字元号付き。
        put("2020106", "2020-10-06");//7文字。正しくないかもしれないけれど、変な動きにならないことの確認。
        put("20201010", "2020-10-10");//8文字。
        //2文字
        put("2020 1021", "2020-10-21");
        put("202010 03", "2020-10-03");
        put("01 10", DateTime.now().getYear() + "-01-10");
        //3文字
        put("2020 12 10", "2020-12-10");
        //4文字
        put("20 20 11 03", "2020-11-03");
        put("202 0 9 10", "2020-09-10");
        //それ以上は日付でなければJoin
        put("2020/04/01 12:13:14.999999", "2020-04-01");
        put("2 02 0 1 1", "2020-11-01");
    }};
    
	@Test
	@Description(value = "期待したとおりに日付変換されること。")
	public void TestParseDate() {
		INPUT_DATE.keySet().forEach(key -> 
			{
				try {
					assertEquals(key + " を " + INPUT_DATE.get(key) + " に変換しようとしたら、 " + DateParser.Parse(key).toString("yyyy-MM-dd") + " だった。", 
						INPUT_DATE.get(key), DateParser.Parse(key).toString("yyyy-MM-dd")
						);
				} catch (Exception e) {
					e.printStackTrace();
					fail(key + "　を変換しようとしたら失敗した。");
				}
			});
	}
	
	@Test
	@Description(value = "基準日によって結果が変更されること。")
	public void TestParseDateReferencesBasisDate() throws Exception {
		assertEquals("2年12月20日 を 2002年より令和2年のほうが近いから和暦として判定してほしかったのに" + DateParser.Parse("2年12月20日", DateTime.parse("2021-01-01")).toString("yyyy-MM-dd") + "だった",
				"2020-12-20", DateParser.Parse("2年12月20日", DateTime.parse("2021-01-01")).toString("yyyy-MM-dd"));
		assertEquals("22年12月20日 を 令和2年より西暦2022年のほうが近いから西暦として判定してほしかったのに" + DateParser.Parse("2年12月20日", DateTime.parse("2021-01-01")).toString("yyyy-MM-dd") + "だった",
				"2022-12-20", DateParser.Parse("22年12月20日", DateTime.parse("2021-01-01")).toString("yyyy-MM-dd"));
	}
	
	@Test
	@Description(value = "月により、先月が選択されるか今月が選択されるか。")
	public void TestParseDateNearYearOfMonth() throws Exception {
		assertEquals("12月20日 を 20210101を基準日として処理したら、前年の請求書として判定してほしかったのに" + DateParser.Parse("12月20日", DateTime.parse("2021-01-01")).toString("yyyy-MM-dd") + "だった",
				"2020-12-20", DateParser.Parse("12月20日", DateTime.parse("2021-01-01")).toString("yyyy-MM-dd"));
	}
	
	@Test
	@Description(value = "パースできないものはExceptionが飛ぶ。")
	public void TestParseIsFail() {
		try {
            DateTime date = DateParser.Parse("2 02 0 1 1 12345");
            fail("変なの(2 02 0 1 1 12345)が" + date.toString("yyyy-MM-dd") + "にパースできた。");
        } catch (FormatException e) {
        	assertTrue(e.getClass().equals(FormatException.class));
        } catch (Exception e) {
        	fail("FormatExceptionではなく、Exceptionでキャッチされた。");
        }
        try {
        	DateTime date = DateParser.Parse("2 02 0 ドラ 1 1");
        	fail("変なの(2 02 0 ドラ 1 1)が" + date.toString("yyyy-MM-dd") + "にパースできた。");
        } catch (FormatException e) {
        	assertTrue(e.getClass().equals(FormatException.class));
        } catch (Exception e) {
        	fail("FormatExceptionではなく、Exceptionでキャッチされた。");
        }
        try {
        	DateTime date = DateParser.Parse("10000/10/10");
        	fail("大きすぎる年が" + date.toString("yyyy-MM-dd") + "にパースできた。");
        } catch (FormatException e) {
        	assertTrue(e.getClass().equals(FormatException.class));
        } catch (Exception e) {
        	fail("FormatExceptionではなく、Exceptionでキャッチされた。");
        }
        try {
        	DateTime date = DateParser.Parse("999/01/01");
        	fail("小さすぎる年が" + date.toString("yyyy-MM-dd") + "にパースできた。");
        } catch (FormatException e) {
        	assertTrue(e.getClass().equals(FormatException.class));
        } catch (Exception e) {
        	fail("FormatExceptionではなく、Exceptionでキャッチされた。");
        }
        try {
            DateParser.Parse(null);
            fail("変なの(null)がなにかにパースできた。");
        } catch (IllegalArgumentException e) {
        	assertTrue(e.getClass().equals(IllegalArgumentException.class));
        } catch (Exception e) {
        	fail("IllegalArgumentExceptionではなく、Exceptionでキャッチされた。");
        }
        try {
            DateParser.Parse("2001-02-29");
            fail("存在しない日付(2001-02-29)がパースできた。");
        } catch (FormatException e) {
        	assertTrue(e.getClass().equals(FormatException.class));
        } catch (Exception e) {
        	fail("FormatExceptionではなく、Exceptionでキャッチされた。");
        }
        try {
            DateParser.Parse("令和九年２月２９日");
            fail("存在しない日付(令和九年２月２９日)がパースできた。");
        } catch (FormatException e) {
        	assertTrue(e.getClass().equals(FormatException.class));
        } catch (Exception e) {
        	fail("FormatExceptionではなく、Exceptionでキャッチされた。");
        }
        try {
            DateParser.Parse("令和五年二月二十九日");
            fail("存在しない日付(令和五年二月二十九日)がパースできた。");
        } catch (FormatException e) {
        	assertTrue(e.getClass().equals(FormatException.class));
        } catch (Exception e) {
        	fail("FormatExceptionではなく、Exceptionでキャッチされた。");
        }
	}

    /**
     * 英語の月を含む日付のパース
     */
    @Test
    public void TestParseDateContainEnglishMonth() {
        LinkedHashMap<String, String> dateListForcontainEnglishMonth = new LinkedHashMap<String, String>()
        {{
            //基本形yyyyMMdd、省略無し
            put("2022/January/12", "2022-01-12");
            put("2022/February/11", "2022-02-11");
            put("2022/March/10", "2022-03-10");
            put("2022/April/09", "2022-04-09");
            put("2022/May/08", "2022-05-08");
            put("2022/June/07", "2022-06-07");
            put("2022/July/06", "2022-07-06");
            put("2022/August/05", "2022-08-05");
            put("2022/September/04", "2022-09-04");
            put("2022/October/03", "2022-10-03");
            put("2022/November/02", "2022-11-02");
            put("2022/December/01", "2022-12-01");
            //基本形yyyyMMdd、省略系
            put("2022/Jan/13", "2022-01-13");
            put("2022/Feb/14", "2022-02-14");
            put("2022/Mar/15", "2022-03-15");
            put("2022/Apr/16", "2022-04-16");
            put("2022/May/17", "2022-05-17");
            put("2022/Jun/18", "2022-06-18");
            put("2022/Jul/19", "2022-07-19");
            put("2022/Aug/20", "2022-08-20");
            put("2022/Sep/21", "2022-09-21");
            put("2022/Oct/22", "2022-10-22");
            put("2022/Nov/23", "2022-11-23");
            put("2022/Dec/24", "2022-12-24");
            //基本形ddMmyyyy、省略無し
            put("12/January/2022", "2022-01-12");
            put("11/February/2022", "2022-02-11");
            put("10/March/2022", "2022-03-10");
            put("09/April/2022", "2022-04-09");
            put("08/May/2022", "2022-05-08");
            put("07/June/2022", "2022-06-07");
            put("06/July/2022", "2022-07-06");
            put("05/August/2022", "2022-08-05");
            put("04/September/2022", "2022-09-04");
            put("03/October/2022", "2022-10-03");
            put("02/November/2022", "2022-11-02");
            put("01/December/2022", "2022-12-01");
            //基本形ddMmyyyy、省略系
            put("13/Jan/2022", "2022-01-13");
            put("14/Feb/2022", "2022-02-14");
            put("15/Mar/2022", "2022-03-15");
            put("16/Apr/2022", "2022-04-16");
            put("17/May/2022", "2022-05-17");
            put("18/Jun/2022", "2022-06-18");
            put("19/Jul/2022", "2022-07-19");
            put("20/Aug/2022", "2022-08-20");
            put("21/Sep/2022", "2022-09-21");
            put("22/Oct/2022", "2022-10-22");
            put("23/Nov/2022", "2022-11-23");
            put("24/Dec/2022", "2022-12-24");
            //デリミタ(スラッシュ、ハイフン、ピリオド、なしを許容)、yyyyMMdd
            put("2022/January/25", "2022-01-25");
            put("2022-Jan-26", "2022-01-26");
            put("2022.February.27", "2022-02-27");
            put("2022Feb28", "2022-02-28");
            //デリミタ(スラッシュ、ハイフン、ピリオド、なしを許容)、ddMMyyyy
            put("25/January/2022", "2022-01-25");
            put("26-Jan-2022", "2022-01-26");
            put("27.February.2022", "2022-02-27");
            put("28Feb2022", "2022-02-28");
            //数字が連なっている文字(正しくない可能性もあるが数字形式の推定と同じ仕様にする)
            put("April", DateTime.now().getYear() + "-04-01");//2文字
            put("April末日", DateTime.now().getYear() + "-04-30");//2文字
            put("昭和元年Jan末日", "1926-01-31");//元年1文字末日付き
            put("平成元年Jan末日", "1989-01-31");//元年1文字末日付き
            put("1January", "2019-01-01");//3文字
            put("Nov1", "2019-11-01");//3文字
            put("平成1Apr", "1989-04-01");//3文字元号付き
            put("平成1Apr末", "1989-04-30");//3文字元号付き、末日付き
            put("Aug11", "2011-08-01");//4文字
            put("11Nov", "2011-11-01");//4文字
            put("01Nov", "2001-11-01");//4文字
            put("01Nov末", "2001-11-30");//4文字末日付き
            put("令和11Nov末日", "2029-11-30");//4文字元号末日付き
            put("令和21Dec", "2039-12-01");//4文字元号付き
            put("4Jan22", "2004-01-22");//5文字年1桁
            put("18Oct2", "2018-10-02");//5文字年2桁
            put("昭和4Jan22", "1929-01-22");//5文字元号付き
            put("20Nov29", "2020-11-29");//6文字
            put("平成10Dec01", "1998-12-01");//6文字元号付き
            put("2020Oct", "2020-10-01");//6文字
            put("Oct2020", "2020-10-01");//6文字
            put("2020Oct末日", "2020-10-31");//6文字末日付き
            put("Oct2020末日", "2020-10-31");//6文字末日付き
            put("2020Oct6", "2020-10-06");//7文字
            put("6Oct2020", "2020-10-06");//7文字
            put("Oct20206", "2020-10-06");//7文字
            put("20206Oct", "2020-10-06");//7文字
            put("202010Oct", "2020-10-10");//8文字
            put("Oct202010", "2020-10-10");//8文字
            put("2020Oct10", "2020-10-10");//8文字
            //2分割
            put("2020 21Oct", "2020-10-21");
            put("2020 Oct21", "2020-10-21");
            put("2020Oct 03", "2020-10-03");
            put("Oct2020 03", "2020-10-03");
            put("Jan 10", DateTime.now().getYear() + "-01-10");
            //3分割
            put("2020 Dec 10", "2020-12-10");//年月日
            put("Dec/2020/10", "2020-12-10");//月年日
            put("Dec/10/2020", "2020-12-10");//月日年
            put("2020/10/Dec", "2020-12-10");//年日月
            put("10/2020/Dec", "2020-12-10");//日年月
            //4分割
            put("20 20 Nov 03", "2020-11-03");
            put("202 0 Sep 10", "2020-09-10");
            put("03 Nov 20 20", "2020-11-03");
            put("10 Sep 202 0", "2020-09-10");
        }};

        dateListForcontainEnglishMonth.keySet().forEach(key ->
        {
            try {
                assertEquals(key + " を " + dateListForcontainEnglishMonth.get(key) + " に変換しようとしたら、 " + DateParser.Parse(key).toString("yyyy-MM-dd") + " だった。",
                        dateListForcontainEnglishMonth.get(key), DateParser.Parse(key).toString("yyyy-MM-dd")
                );
            } catch (Exception e) {
                e.printStackTrace();
                fail(key + "　を変換しようとしたら失敗した。");
            }
        });
    }

    /**
     * 英字の大文字を半角小文字に変換
     */
    @Test
    public void TestFullToHalf() {
        LinkedHashMap<String, String> dateList = new LinkedHashMap<String, String>()
        {{
            //大文字を小文字に
            put("ABCDEFGHIJKLMNOPQRSTUVWXYZ","abcdefghijklmnopqrstuvwxyz");
            //全角を半角に
            put("ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ","abcdefghijklmnopqrstuvwxyz");
            put("ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ","abcdefghijklmnopqrstuvwxyz");
        }};

        dateList.keySet().forEach(key ->
        {
            try {
                    assertEquals(key + " を " + dateList.get(key) + " に変換しようとしたら、 " +
                                    DateParser.ReplaceEnglishMonthToNumber(key, new DateParser.EnglishMonthInfo()) + " だった。",
                            dateList.get(key), DateParser.ToHalfLowerCaseFromFullUpperCaseForAlphabet(key));
            } catch (Exception e) {
                e.printStackTrace();
                fail(key + "　を変換しようとしたら失敗した。");
            }
        });
    }

    /**
     * 英語の月を数字に変換
     */
    @Test
    public void TestReplaceEnglishMonth() {
        LinkedHashMap<String, String> dateList = new LinkedHashMap<String, String>()
        {{
            //January
            put("2022/january/12","2022/01/12");//正常系
            put("2022/janary/11","2022/janary/11");//途中スペルミス(数字に変換しない)
            put("2022/jjanuary/10","2022/jjanuary/10");//単語の前にアルファベットがついている(数字に変換しない)
            put("2022/januaryy/09","2022/januaryy/09");//単語の後にアルファベットがついている(数字に変換しない)
            //February
            put("2022/february/12","2022/02/12");
            put("2022/febryary/11","2022/febryary/11");
            put("2022/ffebruary/10","2022/ffebruary/10");
            put("2022/februaryy/09","2022/februaryy/09");
            //March
            put("2022/march/12","2022/03/12");
            put("2022/marrch/11","2022/marrch/11");
            put("2022/mmarch/10","2022/mmarch/10");
            put("2022/marchh/09","2022/marchh/09");
            //April
            put("2022/april/12","2022/04/12");
            put("2022/apil/11","2022/apil/11");
            put("2022/aapril/10","2022/aapril/10");
            put("2022/aprill/09","2022/aprill/09");
            //May
            put("2022/may/12","2022/05/12");
            put("2022/my/11","2022/my/11");
            put("2022/mmay/10","2022/mmay/10");
            put("2022/mayy/09","2022/mayy/09");
            //June
            put("2022/june/12","2022/06/12");
            put("2022/jne/11","2022/jne/11");
            put("2022/jjune/10","2022/jjune/10");
            put("2022/junee/09","2022/junee/09");
            //July
            put("2022/july/12","2022/07/12");
            put("2022/jury/11","2022/jury/11");
            put("2022/jjuly/10","2022/jjuly/10");
            put("2022/julyy/09","2022/julyy/09");
            //August
            put("2022/august/12","2022/08/12");
            put("2022/augast/11","2022/augast/11");
            put("2022/aaugust/10","2022/aaugust/10");
            put("2022/augustt/09","2022/augustt/09");
            //September
            put("2022/september/12","2022/09/12");
            put("2022/septenber/11","2022/septenber/11");
            put("2022/sseptember/10","2022/sseptember/10");
            put("2022/septemberr/09","2022/septemberr/09");
            //October
            put("2022/october/12","2022/10/12");
            put("2022/octover/11","2022/octover/11");
            put("2022/ooctober/10","2022/ooctober/10");
            put("2022/octoberr/09","2022/octoberr/09");
            //November
            put("2022/november/12","2022/11/12");
            put("2022/novenber/11","2022/novenber/11");
            put("2022/nnovember/10","2022/nnovember/10");
            put("2022/novemberr/09","2022/novemberr/09");
            //December
            put("2022/december/12","2022/12/12");
            put("2022/decenber/11","2022/decenber/11");
            put("2022/ddecember/10","2022/ddecember/10");
            put("2022/decemberr/09","2022/decemberr/09");
        }};

        dateList.keySet().forEach(key ->
        {
            try {
                assertEquals(key + " を " + dateList.get(key) + " に変換しようとしたら、 " + DateParser.ReplaceEnglishMonthToNumber(key, new DateParser.EnglishMonthInfo()) + " だった。",
                        dateList.get(key), DateParser.ReplaceEnglishMonthToNumber(key, new DateParser.EnglishMonthInfo()));
            } catch (Exception e) {
                e.printStackTrace();
                fail(key + "　を変換しようとしたら失敗した。");
            }
        });
    }

}
