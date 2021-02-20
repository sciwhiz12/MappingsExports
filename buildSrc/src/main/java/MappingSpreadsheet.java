import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MappingSpreadsheet {
    private static final String[] HEADER = { "Validated", "Class Name", "Unmapped Name", "Mapped Name",
        "Side(auto, 0 client 2 both)", "JavaDocs Comment" };
    private static final Pattern METHOD_SRG = Pattern.compile("func_\\d+_\\w+_?");
    private static final Pattern FIELD_SRG = Pattern.compile("field_\\d+_\\w+_?");
    private static final Pattern PARAM_SRG = Pattern.compile("p_i?\\d+_\\d+_?");
    private static final Pattern SRG_NUMBER = Pattern.compile("\\w+_i?(\\d+).*");

    private final Map<String, Entry> methods = new LinkedHashMap<>(3000);
    private final Map<String, Entry> fields = new LinkedHashMap<>(3000);
    private final Map<String, Entry> params = new LinkedHashMap<>(3000);

    public MappingSpreadsheet() {
    }

    public void addMethod(String name, Entry newEntry) {
        this.methods.put(name, newEntry);
    }

    public void addField(String name, Entry newEntry) {
        this.fields.put(name, newEntry);
    }

    public void addParam(String name, Entry newEntry) {
        this.params.put(name, newEntry);
    }

    public boolean hasMethod(String name) {
        return this.methods.containsKey(name);
    }

    public boolean hasField(String name) {
        return this.fields.containsKey(name);
    }

    public boolean hasParam(String name) {
        return this.params.containsKey(name);
    }

    public Entry getMethod(String name) {
        return this.methods.get(name);
    }

    public Entry getField(String name) {
        return this.fields.get(name);
    }

    public Entry getParam(String name) {
        return this.params.get(name);
    }

    public Map<String, Entry> getMethods() {
        return methods;
    }

    public Map<String, Entry> getFields() {
        return fields;
    }

    public Map<String, Entry> getParams() {
        return params;
    }

    public static class Entry implements Comparable<Entry> {
        private final boolean validated;
        private final String className;
        private final String unmappedName;
        private final String mappedName;
        private final Side side;
        private final String javadocComment;

        Entry(boolean validated,
            String className,
            String unmappedName,
            String mappedName,
            Side side,
            String javadocComment) {

            this.validated = validated;
            this.className = className;
            this.unmappedName = unmappedName;
            this.mappedName = mappedName;
            this.side = side;
            this.javadocComment = javadocComment;
        }

        public boolean isValidated() {
            return validated;
        }

        public String getClassName() {
            return className;
        }

        public String getUnmappedName() {
            return unmappedName;
        }

        public String getMappedName() {
            return mappedName;
        }

        public Side getSide() {
            return side;
        }

        public String getJavadocComment() {
            return javadocComment;
        }

        public String[] toCSVLine() {
            return new String[] {
                Boolean.toString(validated).toUpperCase(Locale.ROOT),
                className,
                unmappedName,
                mappedName,
                Integer.toString(side.toNumber()),
                javadocComment
            };
        }

        @Override
        public int compareTo(Entry other) {
            Matcher matcher = SRG_NUMBER.matcher(this.unmappedName);
            //noinspection ResultOfMethodCallIgnored
            matcher.find();
            int a1 = Integer.parseInt(matcher.group(1));
            //noinspection ResultOfMethodCallIgnored
            matcher.reset(other.unmappedName).find();
            int a2 = Integer.parseInt(matcher.group(1));
            return a1 - a2;
        }
    }

    public void write(File output) throws IOException {
        List<Entry> entries = new ArrayList<>(this.fields.size() + this.methods.size() + this.params.size());
        entries.addAll(this.fields.values());
        entries.addAll(this.methods.values());
        entries.addAll(this.params.values());

        entries.sort(Comparator.naturalOrder());

        try (CSVWriter writer = new CSVWriter(new FileWriter(output))) {
            writer.writeNext(HEADER);
            for (Entry field : this.fields.values()) {
                writer.writeNext(field.toCSVLine());
            }

            for (Entry methods : this.methods.values()) {
                writer.writeNext(methods.toCSVLine());
            }

            for (Entry params : this.params.values()) {
                writer.writeNext(params.toCSVLine());
            }
        }
    }

    public static MappingSpreadsheet read(File spreadsheet) {
        MappingSpreadsheet mappings = new MappingSpreadsheet();
        if (!spreadsheet.exists()) {
            throw new IllegalArgumentException("Spreadsheet file does not exist");
        }

        List<String[]> lines;
        try (CSVReader reader = new CSVReader(new FileReader(spreadsheet))) {
            lines = reader.readAll();
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Exception while reading spreadsheet file", e);
        }

        Matcher funcMatcher = METHOD_SRG.matcher("");
        Matcher fieldMatcher = FIELD_SRG.matcher("");
        Matcher paramMatcher = PARAM_SRG.matcher("");
        for (int i = 0, linesSize = lines.size(); i < linesSize; i++) {
            String[] line = lines.get(i);

            // Format:
            // "Validated","Class Name","Unmapped Name","Mapped Name","Side(auto, 0 client 2 both)","JavaDocs Comment"

            if (line.length != 6) {
                System.err.println("Invalid line #" + i + ": " + Arrays.toString(line));
                continue;
            }

            if (i == 0) continue; // Skip header

            String validateString = line[0];
            boolean validate = false;
            if (validateString.equals("TRUE")) {
                validate = true;
            } else if (!validateString.equals("FALSE")) {
                System.err.println("Line #" + i + " has invalid 'validate' value: " + validateString + ", defaulting to FALSE");
            }

            String sideString = line[4];
            Side side;
            try {
                side = Side.from(Integer.parseInt(sideString));
            } catch (NumberFormatException e) {
                System.err.println("Line #" + i + " has invalid 'side' value: " + sideString);
                continue;
            }

            String unmapped = line[2];
            if (funcMatcher.reset(unmapped).matches()) {
                mappings.addMethod(unmapped, new MappingSpreadsheet.Entry(validate, line[1], unmapped, line[3], side, line[5]));
            } else if (fieldMatcher.reset(unmapped).matches()) {
                mappings.addField(unmapped, new MappingSpreadsheet.Entry(validate, line[1], unmapped, line[3], side, line[5]));
            } else if (paramMatcher.reset(unmapped).matches()) {
                mappings.addParam(unmapped, new MappingSpreadsheet.Entry(validate, line[1], unmapped, line[3], side, line[5]));
            } else {
                System.err.println("Line #" + i + " has invalid unmapped name: " + unmapped);
            }
        }
        return mappings;
    }
}
