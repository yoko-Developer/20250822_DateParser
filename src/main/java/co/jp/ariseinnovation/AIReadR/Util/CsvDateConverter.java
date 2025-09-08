package co.jp.ariseinnovation.AIReadR.Util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * CSVファイルの和暦データを西暦に変換するユーティリティクラス
 */
public class CsvDateConverter {

    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormat.forPattern("yyyyMM");

    /**
     * CSVファイルの指定された列(名称)の和暦データを西暦に変換する
     * @param inputFilePath 入力CSVファイルのパス
     * @param outputFilePath 出力CSVファイルのパス
     * @param targetColumns 変換対象の列名のリスト (例: 取得, 使用, 事業共用日 など)
     * @throws Exception 変換処理でエラーが発生した場合
     */
    public static void convertCsvDates(String inputFilePath, String outputFilePath, List<String> targetColumns) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFilePath), Charset.forName("Shift_JIS")));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilePath), StandardCharsets.UTF_8))) {

            String line = reader.readLine();
            if (line == null) {
                throw new IllegalArgumentException("CSVファイルが空です");
            }

            // ヘッダー行を解析して列のインデックスを取得
            String[] headers = parseCsvLine(line);
            int itemNameIndex = -1;
            int keyWordIndex = -1;
            int valueIndex = -1;

            for (int i = 0; i < headers.length; i++) {
                if ("ItemName".equals(headers[i])) {
                    itemNameIndex = i;
                } else if ("KeyWord".equals(headers[i])) {
                    keyWordIndex = i;
                } else if ("Value".equals(headers[i])) {
                    valueIndex = i;
                }
            }

            if (valueIndex == -1) {
                throw new IllegalArgumentException("Value列が見つかりません");
            }
            if (itemNameIndex == -1 && keyWordIndex == -1) {
                throw new IllegalArgumentException("ItemName列またはKeyWord列が見つかりません");
            }

            // ヘッダー行を出力
            writer.write(line);
            writer.newLine();

            // データ行を処理
            while ((line = reader.readLine()) != null) {
                String[] fields = parseCsvLine(line);
                String[] convertedFields = Arrays.copyOf(fields, headers.length);

                String label = null;
                if (keyWordIndex != -1 && keyWordIndex < fields.length) {
                    label = fields[keyWordIndex];
                }
                // "null"(文字列)や空なら無効とみなす
                if (label == null || label.trim().isEmpty() || label.equalsIgnoreCase("null")) {
                    if (itemNameIndex != -1 && itemNameIndex < fields.length) {
                        label = fields[itemNameIndex];
                    }
                }

                // ラベル正規化
                String normalizedLabel = normalizeLabel(label);

                // ターゲット名と正規化・厳密一致で判定
                boolean isTarget = false;
                for (String t : targetColumns) {
                    String nt = normalizeLabel(t);
                    if (normalizedLabel.equals(nt)) {
                        isTarget = true;
                        break;
                    }
                }

                if (isTarget && valueIndex < fields.length) {
                    String original = fields[valueIndex] == null ? "" : fields[valueIndex];
                    String normalized = normalizeOcrDateString(original);

                    if (!normalized.isEmpty()) {
                        try {
                            String convertedDate = convertWarekiToSeireki(normalized);

                            if (convertedDate != null) {
                                // 成功した場合だけ変換
                                convertedFields[valueIndex] = convertedDate;
                                System.out.println("変換: " + normalizedLabel + " = '" + original + "' → '" + convertedDate + "'");
                            } else {
                                // 変換できない場合は元の値をそのまま残す
                                convertedFields[valueIndex] = original;
                                System.out.println("未変換: " + normalizedLabel + " = '" + original + "'（元の値を保持）");
                            }

                        } catch (Exception e) {
                            // エラー時も元の値を保持
                            convertedFields[valueIndex] = original;
                            System.err.println("警告: '" + original + "' を変換できませんでした: " + e.getMessage());
                        }
                    }
                }

                writer.write(convertToCsvLine(convertedFields));
                writer.newLine();
            }

            System.out.println("変換が完了しました。出力ファイル: " + outputFilePath);

        } catch (IOException e) {
            throw new Exception("CSVファイルの読み書きでエラーが発生しました", e);
        }
    }

    /**
     * 和暦の日付文字列を西暦のYYYYMM形式に変換する
     * - 元号が見つからなければ **変換せず元の文字列をそのまま返す**
     * - 元号が文字列中にある場合は、その位置以降を切り出して正規化して DateParser に渡す
     */
    public static String convertWarekiToSeireki(String warekiDate) {
        if (warekiDate == null) {
            return null; // 呼び出し側の既存コードが null を特別扱いしている場合を考慮（ただし後述の呼び出し変更でnullはほぼ使わなくする）
        }
        String original = warekiDate;
        String s = warekiDate.trim();
        if (s.isEmpty()) {
            return original;
        }

        // --- 元号を本文中から探す（大文字小文字問わず、漢字・アルファベット両対応）
        Pattern gengoPat = Pattern.compile("(昭和|平成|令和|昭|平|令|S|H|R|L)", Pattern.CASE_INSENSITIVE);
        Matcher m = gengoPat.matcher(s);
        if (!m.find()) {
            // 元号が見つからない -> 変換しない（元の値をそのまま返す）
            return original;
        }

        // 元号位置以降だけ切り出す（DateParserは先頭に元号があることを期待する）
        String sub = s.substring(m.start());

        // --- 簡易正規化（OCRゆらぎ対応）
        sub = sub.replace('　', ' '); // 全角スペース
        sub = sub.replaceAll("\\s+", ""); // 空白除去
        // 全角数字を半角に
        sub = sub.replace("０","0").replace("１","1").replace("２","2").replace("３","3").replace("４","4")
                .replace("５","5").replace("６","6").replace("７","7").replace("８","8").replace("９","9");
        // 元 -> 1
        sub = sub.replace("元", "1");
        // 区切り文字を統一（カンマやスラッシュやハイフンをピリオドに）
        sub = sub.replace("，", ".").replace(",", ".").replace("/", ".").replace("-", ".").replace("／", ".");
        // 連続ピリオドや先頭・末尾のピリオドを整理
        sub = sub.replaceAll("\\.{2,}", ".").replaceAll("^\\.|\\.$", "");

        // ここで sub は例えば "平3.4.30" や "S53.2.28" のようになっているはず
        try {
            DateTime parsedDate = DateParser.Parse(sub);
            return parsedDate.toString(OUTPUT_FORMATTER); // "yyyyMM"
        } catch (Exception e) {
            // パース失敗 → 変換しない（元の値を返す）
            return original;
        }
    }

    /**
     * OCR表記ゆれの正規化
     */
    private static String normalizeOcrDateString(String src) {
        if (src == null) return "";
        String s = src.trim();
        if (s.isEmpty()) return s;

        // 年号のアルファベットまたは漢字で始まらない場合は、元の文字列を返す
        if (!s.matches("^[RrHhSs昭平令].*")) {
            return src;
        }

        // 全角スペースを半角スペースに変換
        s = s.replace('　', ' ');
        s = s.replace(" ", "");

        // 漢字の年号をアルファベットに変換
        s = s.replace("昭和", "S");
        s = s.replace("平成", "H");
        s = s.replace("令和", "R");
        s = s.replace("昭", "S");
        s = s.replace("平", "H");
        s = s.replace("令", "R");
        s = s.replace("元", "1");

        // 全角数字を半角に変換
        s = s.replace("０", "0").replace("１", "1").replace("２", "2").replace("３", "3").replace("４", "4")
                .replace("５", "5").replace("６", "6").replace("７", "7").replace("８", "8").replace("９", "9");

        // デリミタを統一的な「.」に変換
        s = s.replace("年", ".").replace("月", ".").replace("日", "");
        s = s.replace("/", ".").replace("-", ".");

        // 連続したピリオドや前後のピリオドを整理
        s = s.replaceAll("\\.{2,}", ".");
        s = s.replaceAll("^\\.|\\.$", "");

        return s;
    }

    /**
     * ラベルの正規化
     */
    private static String normalizeLabel(String s) {
        if (s == null) return "";
        String x = s.trim();
        if (x.equalsIgnoreCase("null")) return "";
        x = x.replace(" ", "");
        // 全角スペース除去のみ
        x = x.replace('　', ' ');
        x = x.replace("　", "");
        return x;
    }

    private static String[] parseCsvLine(String csvLine) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < csvLine.length(); i++) {
            char c = csvLine.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
        }
        fields.add(currentField.toString());
        return fields.toArray(new String[0]);
    }

    private static String convertToCsvLine(String[] fields) {
        StringBuilder csvLine = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) csvLine.append(',');
            String field = (fields[i] == null) ? "" : fields[i];
            if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
                csvLine.append('"').append(field.replace("\"", "\"\"")).append('"');
            } else {
                csvLine.append(field);
            }
        }
        return csvLine.toString();
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("使用方法: java CsvDateConverter <入力ファイル> <出力ファイル> <列名1> [列名2] ...");
            return;
        }

        // ファイルパスを修正
        String inputFilePath = "固定資産\\input\\" + args[0];
        String outputFilePath = "固定資産\\output\\" + args[1];

        List<String> targetColumns = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            targetColumns.add(args[i]);
        }
        try {
            convertCsvDates(inputFilePath, outputFilePath, targetColumns);
        } catch (Exception e) {
            System.err.println("エラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
