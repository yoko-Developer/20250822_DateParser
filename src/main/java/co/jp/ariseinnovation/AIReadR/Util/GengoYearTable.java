package co.jp.ariseinnovation.AIReadR.Util;

import java.util.Arrays;
import java.util.List;

/**
 * 和暦の年号情報を管理するクラス
 */
public class GengoYearTable {
    
    public static final int REIWA_YEAR_ADDITION = 2018;
    
    public final String gengo;
    public final int yearAdd;
    
    public GengoYearTable(String gengo, int yearAdd) {
        this.gengo = gengo;
        this.yearAdd = yearAdd;
    }
    
    // 和暦の定義
    public static final GengoYearTable MEIJI = new GengoYearTable("明治", 1867);
    public static final GengoYearTable TAISHO = new GengoYearTable("大正", 1911);
    public static final GengoYearTable SHOWA = new GengoYearTable("昭和", 1925);
    public static final GengoYearTable HEISEI = new GengoYearTable("平成", 1988);
    public static final GengoYearTable REIWA = new GengoYearTable("令和", 2018);
    
    // 略称も含む
    public static final GengoYearTable MEIJI_S = new GengoYearTable("M", 1867);
    public static final GengoYearTable TAISHO_T = new GengoYearTable("T", 1911);
    public static final GengoYearTable SHOWA_S = new GengoYearTable("S", 1925);
    public static final GengoYearTable HEISEI_H = new GengoYearTable("H", 1988);
    public static final GengoYearTable REIWA_R = new GengoYearTable("R", 2018);
    public static final GengoYearTable REIWA_L = new GengoYearTable("L", 2018);
    
    // 全和暦のリスト
    public static final List<GengoYearTable> table = Arrays.asList(
        MEIJI, TAISHO, SHOWA, HEISEI, REIWA,
        MEIJI_S, TAISHO_T, SHOWA_S, HEISEI_H, REIWA_R, REIWA_L
    );
}
