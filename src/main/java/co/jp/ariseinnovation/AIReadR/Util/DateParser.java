package co.jp.ariseinnovation.AIReadR.Util;

import co.jp.ariseinnovation.AIReadEE.AIReadEEUtil;
import co.jp.ariseinnovation.AIReadR.Exception.FormatException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DateTime.Parse を以下の目的で更新。
 * * DateTime.ParseがOS依存なのを避ける。
 * * ブラックボックスになっている仕様をコントロールする。
 * 用語
 * year = 西暦
 * wareki = 和暦
 * nen = yearかwarekiか、どっちかわからないけれども年のこと。
 */
public class DateParser
{
    private static final int MINIMUM_YEAR = 1000;
    private static final int MAXIMUM_YEAR = 9999;
    private static final int DATE_MATSUJITU_SIGN = -1;
    private static final int DATE_NOTEXIST_SIGN = -2, DATE_NOTSET_NOW_SIGN = -2;

    private static Logger logger = LogManager.getLogger(DateParser.class.getName());

    public static final LinkedHashMap<String, String> halfToFullDictionary = new LinkedHashMap() {{
        put("０","0");put("１","1");put("２","2");put("３","3");put("４","4");put("５","5");put("６","6");put("７","7");put("８","8");put("９","9");
        put("二十一","21");put("二十二","22");put("二十三","23");put("二十四","24");put("二十五","25");put("二十六","26");put("二十七","27");
        put("二十八","28");put("二十九","29");put("二十","20");
        put("三十一","31");put("三十","30");
        put("十一","11");put("十二","12");put("十三","13");put("十四","14");put("十五","15");put("十六","16");put("十七","17");put("十八","18");put("十九","19");put("十","10");
        put("〇","0");put("零","0");put("一","1");put("二","2");put("三","3");put("四","4");put("五","5");put("六","6");put("七","7");put("八","8");put("九","9");
        put("元","1");
        put("Ｓ","S");put("Ｈ","H");put("Ｒ","R");put("Ｌ","L");
    }};

    public static final LinkedHashMap<String, String> englishMonthDictionary = new LinkedHashMap() {{
        put("january", "01");
        put("february", "02");
        put("march", "03");
        put("april", "04");
        put("may", "05");
        put("june", "06");
        put("july", "07");
        put("august", "08");
        put("september", "09");
        put("october", "10");
        put("november", "11");
        put("december", "12");
        put("jan", "01");
        put("feb", "02");
        put("mar", "03");
        put("apr", "04");
        put("jun", "06");
        put("jul", "07");
        put("aug", "08");
        put("sep", "09");
        put("oct", "10");
        put("nov", "11");
        put("dec", "12");
    }};

    private DateParser()
    {
    }

    public static DateTime Parse(String src) throws Exception
    {
    	return Parse(src, null);
    }
    
	/**
	 * 日付をパースする。
     * @param src パースする文字列
     * @param basisDate パースする際に参考にされる基準値。この日に近い日付が選択される。省略すると、「今」を基準値とする。
	 * @throws Exception 
	 */
    public static DateTime Parse(String src, DateTime basisDate) throws Exception
    {
        if (src == null)
        {
        	throw new IllegalArgumentException("failed to parse dateString");
        }
        try
        {
            DateTime parsed = ParseInner(src, basisDate);
            if (parsed == null)
            {
                throw new FormatException(src + " is not parsable");
            }
            return (DateTime)parsed;
        }
        catch (Exception e)
        {
            throw new FormatException(src + " is not parsable", e);
        }
    }

    private static DateTime ParseInner(String src, DateTime basisDate)
    {
        // デバッグ追加
        System.out.println("DEBUG: Received input string: " + src);
        if (AIReadEEUtil.isNullOrEmpty(src))
        {
            return null;
        }
        src = ReplaceFullToHalf(src);
        EnglishMonthInfo englishMonthInfo = new EnglishMonthInfo();
        src = ReplaceEnglishMonthToNumber(src, englishMonthInfo);
        GengoYearTable gengo = FindGengo(src);
        boolean hasMatsujitsu = HasMatsujitsu(src);
        NumberToken[] originalNumberTokensBeforeReadonly = TokenizeWithNumber(src);

        NumberToken[] orderedNumberTokens;

        if (originalNumberTokensBeforeReadonly.length == 0)
        {
            return null;
        }
        else if (originalNumberTokensBeforeReadonly.length > 4)
        {
            // maybe has time.
            try
            {
                return DateTimeFormat.forPattern("yyyy/MM/dd HH:mm:ss.SSSSSS").parseDateTime(src);
            }
            catch (Exception e)
            {
                // after word is all null or empty
            	boolean flg = true;
            	for (NumberToken nt : originalNumberTokensBeforeReadonly) 
            	{
            		if (!AIReadEEUtil.isNullOrWhiteSpace(nt.afterWord))
            		{
            		    flg = false; // exist value
                        break;
                    }
            	}
            	if (flg)
            	{
                    String numberWordJoin = "";
                    for (NumberToken nt : originalNumberTokensBeforeReadonly)
                    {
                        numberWordJoin += nt.tokenAsString;
                    }
                    originalNumberTokensBeforeReadonly = new NumberToken[] { new NumberToken(numberWordJoin) };
            	}
            	else
            	{
            		throw e;
            	}
            }
        }
        final NumberToken[] originalNumberTokens = originalNumberTokensBeforeReadonly;
        if (originalNumberTokens.length == 1) // re-tokenize if length is 1
        {
            // pattern
            // Constraint:
            //   * year and month must be.
            //   * year is 2 or 4 length. (I dont know 13year past and 78year later.)
            //   * wareki is 1 or 2 length
            // Length 2 eM
            // Length 3 yyM eMd eeM eMM
            // Length 4 yyMd yyMM eeMd eeMM eMMd eMdd → recognize yyMM or eeMM. caz its complicated.
            // Length 5 yyyyM yyMMd yMMdd eeMMd eMMdd → recognize eMMdd. caz its complicated. if MM is not month. recognize eMMdd
            // Length 6 yyyyMM yyyyMd, yyMMdd, eeMMdd → recognize yyyyMM, yyMMdd, eeMMdd. caz its complicated.
            // Length 7 yyyyMMd yyyyMdd → recognize yyyyMMd
            // Length 8 yyyyMMdd
            String date = originalNumberTokens[0].tokenAsString;

            if (date.length() == 2)
            {
                // mm(英語月の場合0パディングした2桁の数字のみ)
                if (englishMonthInfo.isHasEnglishMonth())
                {
                    int suspectYear = GetNearYear(originalNumberTokens[0], IfNullNowOrAsis(basisDate));
                    orderedNumberTokens = NumberToken.CreateArray(String.valueOf(suspectYear), date, hasMatsujitsu ? -1 : 1);
                }
                // eM
                else
                {
                    orderedNumberTokens = NumberToken.CreateArray(date.substring(0, 1), date.substring(1), hasMatsujitsu ? -1 : 1);
                }
            }
            else if (date.length() == 3)
            {
                //eMM
                String tmpDate = date;
                if (englishMonthInfo.isHasEnglishMonth())
                {
                    //dataから月を除去した部分がeになる(英語月の場合0パディングした2桁の数字が含まれるため)
                    tmpDate = date.replace(englishMonthInfo.getMonth(), "") + englishMonthInfo.getMonth();//月を後ろにつける
                }
                //yyM eMd eeM eMM
                CalcStart3WordNenAndMonthByNearNowData calcStart3WordNenAndMonthByNearNowData = CalcStart3WordNenAndMonthByNearNow(tmpDate, gengo, basisDate);
                GengoYearTable suspectGengo = calcStart3WordNenAndMonthByNearNowData.getSuspectGengo();
                int nen = calcStart3WordNenAndMonthByNearNowData.getNen();
                int month = calcStart3WordNenAndMonthByNearNowData.getMonth();
                gengo = suspectGengo;
                if (!IsCollectMonth(month))
                {
                    return null;
                }
                orderedNumberTokens = NumberToken.CreateArray(nen, month, DATE_NOTEXIST_SIGN);
            }
            else if (date.length() == 4)
            {
                //yyMM or eeMM whtch? judge last sequence.
                if (englishMonthInfo.isHasEnglishMonth())
                {
                    //dataから月を除去した部分がyyと仮定
                    String exceptMonth = deleteStr(date, englishMonthInfo.getNumberCountBeforeMonth(), 2);
                    orderedNumberTokens = NumberToken.CreateArray(exceptMonth, englishMonthInfo.getMonth(), DATE_NOTEXIST_SIGN);
                }
                else
                {
                    orderedNumberTokens = NumberToken.CreateArray(date.substring(0, 2), date.substring(2, 4), DATE_NOTEXIST_SIGN);
                }
            }
            else if (date.length() == 5)
            {
                if (englishMonthInfo.isHasEnglishMonth())
                {
                    //dateから月を除去した部分がydd or yydと仮定
                    String exceptMonth = deleteStr(date, englishMonthInfo.getNumberCountBeforeMonth(), 2);
                    orderedNumberTokens = NumberToken.CreateArray(exceptMonth.substring(0, 1),
                            englishMonthInfo.getMonth(), exceptMonth.substring(1, 3));
                    if (!IsCollectDate(orderedNumberTokens[2], Integer.parseInt(englishMonthInfo.getMonth())))
                    {   //suspect yyMMd
                        orderedNumberTokens = NumberToken.CreateArray(exceptMonth.substring(0, 2),
                                englishMonthInfo.getMonth(), exceptMonth.substring(2, 3));
                        if (!IsCollectDate(orderedNumberTokens[2], Integer.parseInt(englishMonthInfo.getMonth())))
                        {
                            return null;
                        }
                    }
                }
                //eMMdd
                else
                {
                    orderedNumberTokens = NumberToken.CreateArray(date.substring(0, 1), date.substring(1, 3), date.substring(3, 5));
                    if (!IsCollectMonth(orderedNumberTokens[1]))
                    {   //suspect eeMMd
                        orderedNumberTokens = NumberToken.CreateArray(date.substring(0, 2), date.substring(2, 4), date.substring(4, 5));
                        if (!IsCollectMonth(orderedNumberTokens[1]))
                        {
                            return null;
                        }
                    }
                }
            }
            else if (date.length() == 6)
            {
                //yyyyMM, yyMMdd, eeMMdd
                if (gengo != null)
                {
                    //yyMMdd
                    if (englishMonthInfo.isHasEnglishMonth())
                    {
                        //dataから月を除去した部分がyyddと仮定
                        String exceptMonth = deleteStr(date, englishMonthInfo.getNumberCountBeforeMonth(), 2);
                        orderedNumberTokens = NumberToken.CreateArray(exceptMonth.substring(0, 2),
                                englishMonthInfo.getMonth(), exceptMonth.substring(2, 4));
                    }
                    else
                    {
                        orderedNumberTokens = NumberToken.CreateArray(date.substring(0, 2), date.substring(2, 4), date.substring(4, 6));
                    }
                }
                else //(gengo == null)
                {
                    if (hasMatsujitsu)
                    {
                        //yyyyMM
                        if (englishMonthInfo.isHasEnglishMonth())
                        {
                            //dataから月を除去した部分がyyyyと仮定
                            String exceptMonth = deleteStr(date, englishMonthInfo.getNumberCountBeforeMonth(), 2);
                            orderedNumberTokens = NumberToken.CreateArray(exceptMonth, englishMonthInfo.getMonth(), DATE_MATSUJITU_SIGN);
                        }
                        else
                        {
                            orderedNumberTokens = NumberToken.CreateArray(date.substring(0, 4), date.substring(4, 6), DATE_MATSUJITU_SIGN);
                        }
                    }
                    else
                    {
                        if (englishMonthInfo.isHasEnglishMonth())
                        {
                            String exceptMonth = deleteStr(date, englishMonthInfo.getNumberCountBeforeMonth(), 2);
                            int twoWordNen = Integer.parseInt(exceptMonth.substring(0, 2));
                            int fourWordNen = Integer.parseInt(exceptMonth.substring(0, 4));
                            if (IsNearYearNowThan(twoWordNen + 2000, fourWordNen, basisDate) ||
                                    IsNearYearNowThan(twoWordNen + GengoYearTable.REIWA_YEAR_ADDITION, fourWordNen, basisDate))
                            {
                                //yyMMdd
                                orderedNumberTokens = NumberToken.CreateArray(exceptMonth.substring(0, 2),
                                        englishMonthInfo.getMonth(), exceptMonth.substring(2, 4));
                            }
                            else
                            {
                                //yyyyMM
                                orderedNumberTokens = NumberToken.CreateArray(exceptMonth.substring(0, 4),
                                        englishMonthInfo.getMonth(), DATE_NOTEXIST_SIGN);
                            }
                        }
                        else
                        {
                            int twoWordNen = Integer.parseInt(date.substring(0, 2));
                            int fourWordNen = Integer.parseInt(date.substring(0, 4));
                            if (IsNearYearNowThan(twoWordNen + 2000, fourWordNen, basisDate) ||
                                    IsNearYearNowThan(twoWordNen + GengoYearTable.REIWA_YEAR_ADDITION, fourWordNen, basisDate))
                            {
                                //yyMMdd, eeMMdd
                                orderedNumberTokens = NumberToken.CreateArray(date.substring(0, 2),
                                        date.substring(2, 4), date.substring(4, 6));
                            }
                            else
                            {
                                //yyyyMM
                                orderedNumberTokens = NumberToken.CreateArray(date.substring(0, 4),
                                        date.substring(4, 6), DATE_NOTEXIST_SIGN);
                            }
                        }
                    }
                }
            }
            else if (date.length() == 7) // recognize to be yyyyMMd
            {
                if (englishMonthInfo.isHasEnglishMonth())
                {
                    //yyyyMMd or dMMyyyyと仮定 月の前後の文字数から年か日と仮定(4文字が年、1文字が日)
                    if ((englishMonthInfo.getNumberCountBeforeMonth() == 4 && englishMonthInfo.getNumberCountAfterMonth() == 1)
                            || (englishMonthInfo.getNumberCountBeforeMonth() == 1 && englishMonthInfo.getNumberCountAfterMonth() == 4))
                    {
                        if (englishMonthInfo.getNumberCountBeforeMonth() == 4)
                        {
                            orderedNumberTokens = NumberToken.CreateArray(date.substring(0, 4),
                                    englishMonthInfo.getMonth(), date.substring(6));
                        }
                        else
                        {
                            orderedNumberTokens = NumberToken.CreateArray(date.substring(3, 7),
                                    englishMonthInfo.getMonth(), date.substring(0, 1));
                        }
                    }
                    //yyyydMM or MMyyyydと仮定
                    else if (englishMonthInfo.getNumberCountBeforeMonth() == 5 || englishMonthInfo.getNumberCountAfterMonth() == 5)
                    {
                        if (englishMonthInfo.getNumberCountBeforeMonth() == 5)
                        {
                            orderedNumberTokens = NumberToken.CreateArray(date.substring(0, 4),
                                    englishMonthInfo.getMonth(), date.substring(4, 5));
                        }
                        else
                        {
                            orderedNumberTokens = NumberToken.CreateArray(date.substring(2, 6),
                                    englishMonthInfo.getMonth(), date.substring(6, 7));
                        }
                    }
                    else
                    {
                        return null; //認識できない
                    }
                }
                else
                {
                    orderedNumberTokens = NumberToken.CreateArray(date.substring(0, 4), date.substring(4, 6), date.substring(6));
                }
            }
            else if (date.length() == 8) // recognize to be yyyyMMdd
            {
                if (englishMonthInfo.isHasEnglishMonth())
                {
                    //月が間(yyyyMMdd or ddMMyyyy)と仮定
                    if ((englishMonthInfo.getNumberCountBeforeMonth() == 4 && englishMonthInfo.getNumberCountAfterMonth() == 2)
                            || (englishMonthInfo.getNumberCountBeforeMonth() == 2 && englishMonthInfo.getNumberCountAfterMonth() == 4))
                    {
                        if (englishMonthInfo.getNumberCountBeforeMonth() == 4)
                        {
                            orderedNumberTokens = NumberToken.CreateArray(date.substring(0, 4),
                                    englishMonthInfo.getMonth(), date.substring(6, 8));
                        }
                        else
                        {
                            orderedNumberTokens = NumberToken.CreateArray(date.substring(4, 8),
                                    englishMonthInfo.getMonth(), date.substring(0, 2));
                        }
                    }
                    //月が最後(yyyyddMM or ddyyyyMM)、もしくは月が前(MMyyyydd or MMddyyyy)と仮定
                    else if (englishMonthInfo.getNumberCountBeforeMonth() == 6 || englishMonthInfo.getNumberCountAfterMonth() == 6)
                    {
                        if (englishMonthInfo.getNumberCountBeforeMonth() == 6)
                        {
                            orderedNumberTokens = NumberToken.CreateArray(date.substring(0, 4),
                                    englishMonthInfo.getMonth(), date.substring(4, 6));
                        }
                        else
                        {
                            orderedNumberTokens = NumberToken.CreateArray(date.substring(2, 6),
                                    englishMonthInfo.getMonth(), date.substring(6, 8));
                        }
                    }
                    else
                    {
                        return null; //認識できない
                    }
                }
                else
                {
                    orderedNumberTokens = NumberToken.CreateArray(date.substring(0, 4), date.substring(4, 6), date.substring(6));
                }
            }
            else // never. For compiler, show month and day must be initialized.
            {
                return null;
            }
        }
        else if (originalNumberTokens.length == 2) // if has matsujitu, yyyy MM or e MM.[yyyy, MM] or [MM, dd], or joined like [yyyyMM dd], [yyyy MMdd]?
        {
            NumberToken firstNumberToken = originalNumberTokens[0];
            NumberToken secondNumberToken = originalNumberTokens[1];

            if (englishMonthInfo.isHasEnglishMonth())
            {
                //1つ目の配列に月がある場合は最後につける　MM or yyyyMMを想定
                if (englishMonthInfo.getNumberCountBeforeMonth() < firstNumberToken.tokenAsString.length())
                {
                    firstNumberToken = new NumberToken(deleteStr(firstNumberToken.tokenAsString,
                            englishMonthInfo.getNumberCountBeforeMonth(), 2) + englishMonthInfo.getMonth());
                }
                //2つ目の配列に月がある場合は前につける　MM or MMdd
                else
                {
                    secondNumberToken = new NumberToken(englishMonthInfo.getMonth() + deleteStr(secondNumberToken.tokenAsString,
                            englishMonthInfo.getNumberCountBeforeMonth() - firstNumberToken.tokenAsString.length(), 2));
                }
            }

            if (hasMatsujitsu)
            {
                orderedNumberTokens = NumberToken.CreateArray(firstNumberToken, secondNumberToken, DATE_MATSUJITU_SIGN);
            }
            //check length
            else if (gengo == null && firstNumberToken.token >= 10000)// may be year and month is join.
            {
                String yearAndMonthJoined = firstNumberToken.tokenAsString;
                orderedNumberTokens = NumberToken.CreateArray(yearAndMonthJoined.substring(0, 4), yearAndMonthJoined.substring(4), secondNumberToken);
            }
            else if (gengo != null && firstNumberToken.token >= 100)// may be year and month is join.
            {
                String yearAndMonthJoined = firstNumberToken.tokenAsString;
                orderedNumberTokens = NumberToken.CreateArray(yearAndMonthJoined.substring(0, 1), yearAndMonthJoined.substring(1), secondNumberToken);
            }
            else if (secondNumberToken.token >= 100)// MMdd is joined.
            {
                String yearAndMonthJoined = secondNumberToken.tokenAsString;
                orderedNumberTokens = NumberToken.CreateArray(firstNumberToken, yearAndMonthJoined.substring(0, 2), yearAndMonthJoined.substring(2));
            }
            else // may be not join. and recognize "ついたち" is omitted.
            {
                if (gengo != null)// must be yy MM.
                {
                    orderedNumberTokens = NumberToken.CreateArray(firstNumberToken, secondNumberToken, 1);
                }
                else
                {
                    if (IsCollectYear(firstNumberToken) && !src.contains("日"))// must be yyyy MM. if contains 「日」, it must have date.
                    {
                        orderedNumberTokens = NumberToken.CreateArray(firstNumberToken, secondNumberToken, 1);
                    }
                    else if ((firstNumberToken.HasAfter("月") && secondNumberToken.HasAfter("日")) ||
                            (IsCollectMonth(firstNumberToken) && !src.contains("年"))) // may be MM dd. if contains 「年」, it must have year.
                    {
                        int suspectYear = GetNearYear(firstNumberToken, IfNullNowOrAsis(basisDate));
                        orderedNumberTokens = NumberToken.CreateArray(suspectYear, firstNumberToken, secondNumberToken);
                    }
                    else
                    {
                        return null;
                    }
                }
            }
        }
        else if (originalNumberTokens.length == 3)// [yyyy, MM, dd] or [MM, dd, yyyy] or ([ee, MM, dd] with gengo)
        {
            if (englishMonthInfo.isHasEnglishMonth())
            {
                NumberToken year = new NumberToken();
                NumberToken month = new NumberToken();
                NumberToken date = new NumberToken();

                //月が先頭の配列　MM yyyy dd or MM dd yyyyと仮定
                if (englishMonthInfo.getNumberCountBeforeMonth() < originalNumberTokens[0].tokenAsString.length() &&
                        originalNumberTokens[0].tokenAsString.length() == 2)
                {
                    month = originalNumberTokens[0];
                    //残りの2配列で4桁のものを年にする。無ければ先の配列を年にする
                    if (originalNumberTokens[2].tokenAsString.length() == 4)
                    {
                        year = originalNumberTokens[2];
                        date = originalNumberTokens[1];
                    }
                    else
                    {
                        year = originalNumberTokens[1];
                        date = originalNumberTokens[2];
                    }
                }
                //月が2番目　yyyy MM dd or dd MM yyyyと仮定
                else if (englishMonthInfo.getNumberCountBeforeMonth() < (originalNumberTokens[0].tokenAsString.length() +
                        originalNumberTokens[1].tokenAsString.length()) && originalNumberTokens[1].tokenAsString.length() == 2)
                {
                    month = originalNumberTokens[1];
                    //残りの2配列で4桁のものを年にする。無ければ先の配列を年にする
                    if (originalNumberTokens[2].tokenAsString.length() == 4)
                    {
                        year = originalNumberTokens[2];
                        date = originalNumberTokens[0];
                    }
                    else
                    {
                        year = originalNumberTokens[0];
                        date = originalNumberTokens[2];
                    }
                }
                //月が最後 yyyy dd MM or dd yyyy MMと仮定
                else if (originalNumberTokens[2].tokenAsString.length() == 2)
                {
                    month = originalNumberTokens[2];
                    //残りの2配列で4桁のものを年にする。無ければ先の配列を年にする
                    if (originalNumberTokens[1].tokenAsString.length() == 4)
                    {
                        year = originalNumberTokens[1];
                        date = originalNumberTokens[0];
                    }
                    else
                    {
                        year = originalNumberTokens[0];
                        date = originalNumberTokens[1];
                    }
                }
                else
                {
                    return null; ////月が他の数字と結合されており不明
                }
                orderedNumberTokens = NumberToken.CreateArray(year, month, date);
            }
            else
            {
                // recognize which index 0 or index 2 is year.
                if (gengo == null && !IsCollectYear(originalNumberTokens[0]) && IsCollectYear(originalNumberTokens[2]))
                {
                    orderedNumberTokens = NumberToken.CreateArray(originalNumberTokens[2], originalNumberTokens[0], originalNumberTokens[1]);
                }
                else
                {
                    //yyyyMMdd
                    orderedNumberTokens = NumberToken.CreateArray(originalNumberTokens[0], originalNumberTokens[1], originalNumberTokens[2]);
                }
            }
        }
        else if (originalNumberTokens.length == 4)// [yyyy, MM, dd] and somethere separated.
        {
            if (gengo != null)
            {
                // because I have no idea which part are separated.
                return null;
            }
            else
            {
                NumberToken firstNumberToken = originalNumberTokens[0]; //yy
                NumberToken secondNumberToken = originalNumberTokens[1]; //yy
                NumberToken thirdNumberToken = originalNumberTokens[2]; //MM
                NumberToken forthNumberToken = originalNumberTokens[3]; //dd

                // [yy yy MM dd]?
                if (englishMonthInfo.isHasEnglishMonth())
                {
                    //3番目が月かつ2桁
                    if (englishMonthInfo.getNumberCountBeforeMonth() >= (originalNumberTokens[0].tokenAsString.length() +
                            originalNumberTokens[1].tokenAsString.length())
                            && englishMonthInfo.getNumberCountBeforeMonth() < (originalNumberTokens[0].tokenAsString.length() +
                            originalNumberTokens[1].tokenAsString.length() + originalNumberTokens[2].tokenAsString.length())
                            && originalNumberTokens[2].tokenAsString.length() == 2)
                    {
                        //並び通り、何もしない。
                    }
                    //2番目が月かつ2桁
                    else if (englishMonthInfo.getNumberCountBeforeMonth() >= (originalNumberTokens[0].tokenAsString.length())
                            && englishMonthInfo.getNumberCountBeforeMonth() < (originalNumberTokens[0].tokenAsString.length() +
                            originalNumberTokens[1].tokenAsString.length())
                            && originalNumberTokens[1].tokenAsString.length() == 2)
                    {
                        //[dd MM yy yy]と仮定し月と日を入れ替える
                        firstNumberToken = originalNumberTokens[2];
                        secondNumberToken = originalNumberTokens[3];
                        thirdNumberToken = originalNumberTokens[1];
                        forthNumberToken = originalNumberTokens[0];
                    }
                    else
                    {
                        return null;
                    }
                }
                int yearCandidate = Integer.parseInt(firstNumberToken.tokenAsString + "" + secondNumberToken.tokenAsString);
                if (firstNumberToken.afterWord.trim().equals("") && IsCollectYear(yearCandidate))
                {
                    orderedNumberTokens = NumberToken.CreateArray(yearCandidate, thirdNumberToken, forthNumberToken);
                }
                else
                {
                    return null;
                }
            }
        }
        else // Too many tokens. but not come here caz validate before.
        {
            return null;
        }

        // finalize
        if (gengo != null)
        {
            orderedNumberTokens[0] = new NumberToken(orderedNumberTokens[0].token + gengo.yearAdd);
        }
        else if (!IsCollectYear(orderedNumberTokens[0]) && 100 > orderedNumberTokens[0].token)// suspect "令和" is omitted. or YYyy's YY is ommited.
        {
            if (englishMonthInfo.isHasEnglishMonth())
            {
                //英語月が含まれる場合の年の桁不足は西暦として扱う
                orderedNumberTokens[0] = new NumberToken(orderedNumberTokens[0].token + 2000);
            }
            else {
                orderedNumberTokens[0] = new NumberToken(CalcNealyYearFromWarekiOrYear(orderedNumberTokens[0], basisDate));
            }
            if (!IsCollectYear(orderedNumberTokens[0]))// validate.
            {
                return null;
            }
        }
        if (hasMatsujitsu || orderedNumberTokens[2].token == DATE_MATSUJITU_SIGN)
        {
        	YearMonth yearMonthObject = YearMonth.of(orderedNumberTokens[0].token, orderedNumberTokens[1].token);
        	int daysInMonth = yearMonthObject.lengthOfMonth();
            orderedNumberTokens[2] = new NumberToken(daysInMonth);
        }
        else if (orderedNumberTokens[2].token == DATE_NOTEXIST_SIGN || orderedNumberTokens[2].token == DATE_NOTSET_NOW_SIGN)
        {
            orderedNumberTokens[2] = new NumberToken(1);
        }
        DateTime parsedDate = new DateTime(orderedNumberTokens[0].token, orderedNumberTokens[1].token,
                orderedNumberTokens[2].token, 0, 0);

        //validate
        if (!IsCollectYear(orderedNumberTokens[0]) || parsedDate.getYear() != orderedNumberTokens[0].token ||
                parsedDate.getMonthOfYear() != orderedNumberTokens[1].token || parsedDate.getDayOfMonth() != orderedNumberTokens[2].token)
        {
            return null;
        }
        return parsedDate;
    }

    private static int GetNearYear(NumberToken month, DateTime basisTime)
    {
        return GetNearYear(month.token, basisTime);
    }
    
    private static int GetNearYear(int month, DateTime basisTime)
    {
        if (month == 12 && basisTime.getMonthOfYear() == 1)
        {
            return basisTime.getYear() - 1;
        }
        return basisTime.getYear();
    }
    
    private static DateTime IfNullNowOrAsis(DateTime datetime)
    {
        if (datetime == null)
        {
            return DateTime.now();
        }
        return (DateTime)datetime;
    }
    
    private static CalcStart3WordNenAndMonthByNearNowData CalcStart3WordNenAndMonthByNearNow(String src, GengoYearTable knownGengo, DateTime basisDate)
    {
        CalcStart3WordNenAndMonthByNearNowData calcStart3WordNenAndMonthByNearNowData = new CalcStart3WordNenAndMonthByNearNowData();
        int oneWordNen = Integer.parseInt(src.substring(0, 1));
        int twoWordNen = Integer.parseInt(src.substring(0, 2));
        if (knownGengo != null)
        {
            // just judge which 1 or 2 word is wareki.
            if (IsNearYearNowThan(oneWordNen + knownGengo.yearAdd, twoWordNen + knownGengo.yearAdd, basisDate))
            {
                calcStart3WordNenAndMonthByNearNowData.setSuspectGengo(knownGengo);
                calcStart3WordNenAndMonthByNearNowData.setNen(oneWordNen);
                calcStart3WordNenAndMonthByNearNowData.setMonth(Integer.parseInt(src.substring(1, 3)));
            }
            else
            {
                calcStart3WordNenAndMonthByNearNowData.setSuspectGengo(knownGengo);
                calcStart3WordNenAndMonthByNearNowData.setNen(oneWordNen);
                calcStart3WordNenAndMonthByNearNowData.setMonth(Integer.parseInt(src.substring(2, 3)));
            }
            return calcStart3WordNenAndMonthByNearNowData;
        }
        int oneWordNenAsYear = oneWordNen + 2000;
        int oneWordNenAsWareki = oneWordNen + GengoYearTable.REIWA_YEAR_ADDITION;
        int twoWordNenAsYear = oneWordNen + 2000;
        int twoWordNenAsWareki = oneWordNen + GengoYearTable.REIWA_YEAR_ADDITION;

        int nowYear = IfNullNowOrAsis(basisDate).getYear();
        int absOneYear = nowYear - oneWordNenAsYear;
        int absOneWareki = nowYear - oneWordNenAsWareki;
        int absTwoYear = nowYear - twoWordNenAsYear;
        int absTwoWareki = nowYear - twoWordNenAsWareki;
        int mostNear = Math.min(absOneYear, Math.min(absOneWareki, Math.min(absTwoYear, absTwoWareki)));
        if (mostNear == absOneYear)
        {
            calcStart3WordNenAndMonthByNearNowData.setSuspectGengo(null);
            calcStart3WordNenAndMonthByNearNowData.setNen(oneWordNen);
            calcStart3WordNenAndMonthByNearNowData.setMonth(Integer.parseInt(src.substring(1, 3)));
        }
        else if (mostNear == absOneWareki)
        {
            calcStart3WordNenAndMonthByNearNowData.setSuspectGengo(GengoYearTable.REIWA);
            calcStart3WordNenAndMonthByNearNowData.setNen(oneWordNen);
            calcStart3WordNenAndMonthByNearNowData.setMonth(Integer.parseInt(src.substring(1, 3)));
        }
        else if (mostNear == absTwoYear)
        {
            calcStart3WordNenAndMonthByNearNowData.setSuspectGengo(null);
            calcStart3WordNenAndMonthByNearNowData.setNen(oneWordNen);
            calcStart3WordNenAndMonthByNearNowData.setMonth(Integer.parseInt(src.substring(2, 3)));
        }
        else //(mostNear == absTwoWareki)
        {
            calcStart3WordNenAndMonthByNearNowData.setSuspectGengo(GengoYearTable.REIWA);
            calcStart3WordNenAndMonthByNearNowData.setNen(oneWordNen);
            calcStart3WordNenAndMonthByNearNowData.setMonth(Integer.parseInt(src.substring(2, 3)));
        }
        return calcStart3WordNenAndMonthByNearNowData;
    }
    
    private static int CalcNealyYearFromWarekiOrYear(NumberToken src, DateTime basisDate)
    {
        return CalcNealyYearFromWarekiOrYear(src.token, basisDate);
    }
    
    private static int CalcNealyYearFromWarekiOrYear(int src, DateTime basisDate)
    {
        int candidateWareki = src + GengoYearTable.REIWA_YEAR_ADDITION;
        int candidateYear = src + 2000;
        return IsNearYearNowThan(candidateWareki, candidateYear, basisDate) ? candidateWareki : candidateYear;
    }
    
    private static boolean IsNearYearNowThan(int src, int target, DateTime basisDate)
    {
        int thisYear = IfNullNowOrAsis(basisDate).getYear();
        return Math.abs(src - thisYear) < Math.abs(target - thisYear);
    }

    private static String ReplaceFullToHalf(String src)
    {
        src = ToHalfLowerCaseFromFullUpperCaseForAlphabet(src);
    	for (String key : halfToFullDictionary.keySet())
        {
            src = src.replace(key, halfToFullDictionary.get(key));
        }
        return src;
    }

    /**
     * 英語月を数字に変換する。OCR誤読やスペルミスは変換しない
     * @param src
     * @return
     */
    public static String ReplaceEnglishMonthToNumber(String src, EnglishMonthInfo englishMonthInfo) {
        EnglishMonthInfo.init(englishMonthInfo, false, "", 0, 0);
        for (String key : englishMonthDictionary.keySet())
        {
            String regex = "(?<![a-z])" + key + "(?![a-z])";
            Pattern p = Pattern.compile(regex);
            Matcher matcher = p.matcher(src);
            if (matcher.find())
            {
                englishMonthInfo.setHasEnglishMonth(true);
                englishMonthInfo.setMonth(englishMonthDictionary.get(key));
                //英語月の前後の数字の数を保持
                String[] monthSplit = p.split(src, -1);
                englishMonthInfo.setNumberCountBeforeMonth(monthSplit[0].replaceAll("[^0-9]", "").length());
                englishMonthInfo.setNumberCountAfterMonth(monthSplit[1].replaceAll("[^0-9]", "").length());
                src = src.replaceAll(regex, englishMonthDictionary.get(key));
                break;
            }
        }
        return src;
    }

    /**
     * アルファベットを半角小文字に変換
     * @param src
     * @return
     */
    public static String ToHalfLowerCaseFromFullUpperCaseForAlphabet(String src)
    {
        StringBuffer sb = new StringBuffer();
        String p = src.toLowerCase();
        for (int i = 0; i < p.length(); i++)
        {
            Character c = new Character(p.charAt(i));
            // 全角英字の文字コードに含まれる場合
            if (c.compareTo(new Character((char)0xFF41)) >= 0 && c.compareTo(new Character((char)0xFF5A)) <= 0)
            {
                sb.append((char) (p.charAt(i) - 0xFF41 + 0x0061));
            }
            else // 変換なし
            {
                sb.append(p.charAt(i));
            }
        }
        return sb.toString();
    }

    /**
     * find 昭和、平成、令和、昭、平、令、S、H、R、L
     * @param src
     * @return
     */
    private static GengoYearTable FindGengo(String src)
    {
        return GengoYearTable.table.stream().filter(gengoTable -> src.toLowerCase().
                startsWith(gengoTable.gengo.toLowerCase())).findFirst().orElse(null);
    }

    private static boolean HasMatsujitsu(String src)
    {
        String trimed = src.trim();
        return trimed.endsWith("末") || trimed.endsWith("末日");
    }

    private static NumberToken[] TokenizeWithNumber(String src)
    {
    	Matcher matcher = Pattern.compile("(?<number>[0-9]+)(?<after>[^0-9]*)").matcher(src);
    	List<NumberToken> list = new ArrayList<>();
    	while(matcher.find()) {
    		list.add(new NumberToken(matcher.group("number"), matcher.group("after")));
    	}

        return list.toArray(new NumberToken[0]);
    }

	/**
	 * yearToken が範囲内かを確認
	 * @param yearToken
	 * @return true : yearTokenが範囲内(MINIMUM_YEAR～MAXIMUM_YEAR)  false : yearTokenが範囲外
	 */
    private static boolean IsCollectYear(NumberToken yearToken)
    {
        return IsCollectYear(yearToken.token);
    }

	/**
	 * monthToken が範囲内かを確認
	 * @param monthToken
	 * @return true : monthTokenが範囲内(1～12)  false : monthTokenが範囲外
	 */
    private static boolean IsCollectMonth(NumberToken monthToken)
    {
        return IsCollectMonth(monthToken.token);
    }

    /**
     * monthToken が範囲内かを確認
     * @param monthToken
     * @param month
     * @return
     */
    private static boolean IsCollectDate(NumberToken monthToken, int month)
    {
        return IsCollectDate(monthToken.token, month);
    }

	/**
	 * year が範囲内かを確認
	 * @param year
	 * @return true : yearが範囲内(MINIMUM_YEAR～MAXIMUM_YEAR)  false : yearが範囲外
	 */
    private static boolean IsCollectYear(int year)
    {
        return year >= MINIMUM_YEAR && year <= MAXIMUM_YEAR;
    }

	/**
	 * month が範囲内かを確認
	 * @param month
	 * @return true : monthが範囲内(1～12)  false : monthが範囲外
	 */
    private static boolean IsCollectMonth(int month)
    {
        return month >= 1 && month <= 12;
    }

    private static boolean IsCollectDate(int date, int month) {
        if (month == 2)
        {
            //年が不定のため29まで考慮
            return date >= 1 && date <= 29;
        }
        else if (0 <= Arrays.asList(4, 6, 9, 11).indexOf(month))
        {
            return date >= 1 && date <= 30;
        }
        else if(0 <= Arrays.asList(1, 3, 5, 7, 8, 10, 12).indexOf(month))
        {
            return date >= 1 && date <= 31;
        }
        return false;
    }

    /**
     * 文字列の指定位置から指定文字数を削除する
     * @param str 対象文字列
     * @param startIndex 切り取り開始位置
     * @param count 切り取り文字数
     * @return
     */
    public static String deleteStr(String str, int startIndex, int count) {
        StringBuilder sb = new StringBuilder(str);
        return sb.delete(startIndex, startIndex + count).toString();
    }

    private static class CalcStart3WordNenAndMonthByNearNowData {
        GengoYearTable suspectGengo;
        int nen;
        int month;

        public GengoYearTable getSuspectGengo() {
            return suspectGengo;
        }
        public void setSuspectGengo(GengoYearTable suspectGengo) {
            this.suspectGengo = suspectGengo;
        }
        public int getNen() {
            return nen;
        }
        public void setNen(int nen) {
            this.nen = nen;
        }
        public int getMonth() {
            return month;
        }
        public void setMonth(int month) {
            this.month = month;
        }
    }

    public static class EnglishMonthInfo {
        public boolean hasEnglishMonth;
        public String month;
        public int numberCountBeforeMonth;
        public int numberCountAfterMonth;

        public EnglishMonthInfo(){}

        public EnglishMonthInfo(boolean hasEnglishMonth, String month, int numberCountBeforeMonth,
                                int numberCountAfterMonth) {
            this.hasEnglishMonth = hasEnglishMonth;
            this.month = month;
            this.numberCountBeforeMonth = numberCountBeforeMonth;
            this.numberCountAfterMonth = numberCountAfterMonth;
        }

        /**
         *
         * @param emi
         * @param hasEnglishMonth
         * @param month
         * @param numberCountBeforeMonth
         * @param numberCountAfterMonth
         */
        public static void init(EnglishMonthInfo emi, boolean hasEnglishMonth, String month, int numberCountBeforeMonth,
                                int numberCountAfterMonth) {
            emi.setHasEnglishMonth(hasEnglishMonth);
            emi.setMonth(month);
            emi.setNumberCountBeforeMonth(numberCountBeforeMonth);
            emi.setNumberCountAfterMonth(numberCountAfterMonth);
        }

        public boolean isHasEnglishMonth() {
            return hasEnglishMonth;
        }

        public void setHasEnglishMonth(boolean hasEnglishMonth) {
            this.hasEnglishMonth = hasEnglishMonth;
        }

        public String getMonth() {
            return month;
        }

        public void setMonth(String month) {
            this.month = month;
        }

        public int getNumberCountBeforeMonth() {
            return numberCountBeforeMonth;
        }

        public void setNumberCountBeforeMonth(int numberCountBeforeMonth) {
            this.numberCountBeforeMonth = numberCountBeforeMonth;
        }

        public int getNumberCountAfterMonth() {
            return numberCountAfterMonth;
        }

        public void setNumberCountAfterMonth(int numberCountAfterMonth) {
            this.numberCountAfterMonth = numberCountAfterMonth;
        }
    }
}
