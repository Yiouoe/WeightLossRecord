package com.example.weightloss.util;

import com.example.weightloss.model.WeightRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** CSV 导入导出工具，兼容 BOM 与带引号的字段。 */
public final class CsvUtils {

    private static final String HEADER = "日期,体重(kg),状态,备注";

    private CsvUtils() {
    }

    public static void export(OutputStream outputStream, List<WeightRecord> records)
            throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        writer.write('\uFEFF');
        writer.write(HEADER);
        writer.write("\r\n");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        for (WeightRecord record : records) {
            writer.write(dateFormat.format(new Date(record.getDate())));
            writer.write(',');
            writer.write(HealthUtils.formatWeight(record.getWeight()));
            writer.write(',');
            writer.write(escape(moodLabel(record.getMood())));
            writer.write(',');
            writer.write(escape(record.getNote() == null ? "" : record.getNote()));
            writer.write("\r\n");
        }
        writer.flush();
    }

    public static List<CsvRecord> read(InputStream inputStream) throws IOException {
        List<CsvRecord> result = new ArrayList<>();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String line;
        boolean first = true;
        while ((line = reader.readLine()) != null) {
            if (first) {
                first = false;
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                if (line.contains("日期") && line.contains("体重")) {
                    continue;
                }
            }
            if (line.trim().isEmpty()) {
                continue;
            }
            List<String> columns = parseLine(line);
            if (columns.size() < 2) {
                continue;
            }

            long date;
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
                dateFormat.setLenient(false);
                Date parsed = dateFormat.parse(columns.get(0).trim());
                if (parsed == null) {
                    continue;
                }
                date = DateUtils.normalizeDate(parsed.getTime());
            } catch (ParseException exception) {
                continue;
            }
            if (date > DateUtils.today()) {
                continue;
            }

            double weight;
            try {
                weight = Double.parseDouble(columns.get(1).trim().replace(',', '.'));
            } catch (NumberFormatException exception) {
                continue;
            }
            if (weight < 20 || weight > 300) {
                continue;
            }

            int mood = WeightRecord.MOOD_NORMAL;
            if (columns.size() > 2) {
                mood = parseMood(columns.get(2).trim());
            }
            String note = columns.size() > 3 ? columns.get(3).trim() : "";
            if (note.length() > 100) {
                note = note.substring(0, 100);
            }

            result.add(new CsvRecord(date, weight, note, mood));
        }
        return result;
    }

    private static int parseMood(String value) {
        if (value.equals("轻松")) {
            return WeightRecord.MOOD_RELAXED;
        }
        if (value.equals("疲惫")) {
            return WeightRecord.MOOD_TIRED;
        }
        try {
            int mood = Integer.parseInt(value);
            return MoodUtils.normalize(mood);
        } catch (NumberFormatException ignored) {
            return WeightRecord.MOOD_NORMAL;
        }
    }

    private static String moodLabel(int mood) {
        switch (mood) {
            case WeightRecord.MOOD_RELAXED:
                return "轻松";
            case WeightRecord.MOOD_TIRED:
                return "疲惫";
            default:
                return "正常";
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"")
                || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private static List<String> parseLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                columns.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        columns.add(current.toString());
        return columns;
    }

    public static class CsvRecord {
        public final long date;
        public final double weight;
        public final String note;
        public final int mood;

        public CsvRecord(long date, double weight, String note, int mood) {
            this.date = date;
            this.weight = weight;
            this.note = note;
            this.mood = mood;
        }
    }
}
