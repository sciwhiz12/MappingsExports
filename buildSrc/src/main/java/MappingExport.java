import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MappingExport {
    public static final String PARAMS_CSV = "params.csv";
    public static final String METHODS_CSV = "methods.csv";
    public static final String FIELDS_CSV = "fields.csv";

    public static final String[] METHODS_CSV_HEADER = { "searge", "name", "side", "desc" };
    public static final String[] FIELDS_CSV_HEADER = { "searge", "name", "side", "desc" };
    public static final String[] PARAMS_CSV_HEADER = { "func", "name", "side" };

    private static final Map<String, Object> FS_OPTIONS = new HashMap<>();

    static {
        FS_OPTIONS.put("create", "true");
    }

    private final Map<String, MemberEntry> methods = new LinkedHashMap<>(3000);
    private final Map<String, MemberEntry> fields = new LinkedHashMap<>(3000);
    private final Map<String, ParamEntry> params = new LinkedHashMap<>(3000);

    public MappingExport() {
    }

    public void addMethod(String name, MemberEntry newEntry) {
        this.methods.put(name, newEntry);
    }

    public void addField(String name, MemberEntry newEntry) {
        this.fields.put(name, newEntry);
    }

    public void addParam(String name, ParamEntry newEntry) {
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

    public MemberEntry getMethod(String name) {
        return this.methods.get(name);
    }

    public MemberEntry getField(String name) {
        return this.fields.get(name);
    }

    public ParamEntry getParam(String name) {
        return this.params.get(name);
    }

    public Map<String, MemberEntry> getMethods() {
        return methods;
    }

    public Map<String, MemberEntry> getFields() {
        return fields;
    }

    public Map<String, ParamEntry> getParams() {
        return params;
    }

    public interface Entry {
        String getUnmappedName();

        String getMappedName();

        Side getSide();
    }

    public static class MemberEntry implements Entry {
        private final String unmappedName;
        private final String mappedName;
        private final Side side;
        private final String javadoc;

        public MemberEntry(String unmappedName, String mappedName, Side side, String javadoc) {
            this.unmappedName = unmappedName;
            this.mappedName = mappedName;
            this.side = side;
            this.javadoc = javadoc;
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

        public String getJavadoc() {
            return javadoc;
        }
    }

    public static class ParamEntry implements Entry {
        private final String unmappedName;
        private final String mappedName;
        private final Side side;

        public ParamEntry(String unmappedName, String mappedName, Side side) {
            this.unmappedName = unmappedName;
            this.mappedName = mappedName;
            this.side = side;
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
    }

    public void write(File output) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(output.toPath(), FS_OPTIONS)) {

            // functions/methods file
            try (BufferedWriter writer = Files.newBufferedWriter(fs.getPath(METHODS_CSV))) {
                writer.write(String.join(",", METHODS_CSV_HEADER));
                writer.newLine();

                List<MemberEntry> methods = new ArrayList<>(this.methods.values());
                methods.sort(Comparator.comparing(MemberEntry::getUnmappedName));

                for (MemberEntry func : methods) {
                    if (!func.getMappedName().isBlank()) {
                        writer.write(String.join(",", new String[] {
                            func.getUnmappedName(), func.getMappedName(), Integer.toString(func.getSide().toNumber()),
                            func.getJavadoc()
                        }));
                        writer.newLine();
                    }
                }
            }

            // fields file
            try (BufferedWriter writer = Files.newBufferedWriter(fs.getPath(FIELDS_CSV))) {
                writer.write(String.join(",", FIELDS_CSV_HEADER));
                writer.newLine();

                List<MemberEntry> fields = new ArrayList<>(this.fields.values());
                fields.sort(Comparator.comparing(MemberEntry::getUnmappedName));

                for (MemberEntry field : fields) {
                    if (!field.getMappedName().isBlank()) {
                        writer.write(String.join(",", new String[] {
                            field.getUnmappedName(), field.getMappedName(), Integer.toString(field.getSide().toNumber()),
                            field.getJavadoc()
                        }));
                        writer.newLine();
                    }
                }
            }

            // params file
            try (BufferedWriter writer = Files.newBufferedWriter(fs.getPath(PARAMS_CSV))) {
                writer.write(String.join(",", PARAMS_CSV_HEADER));
                writer.newLine();

                List<ParamEntry> params = new ArrayList<>(this.params.values());
                params.sort(Comparator.comparing(ParamEntry::getUnmappedName));

                for (ParamEntry param : params) {
                    if (!param.getMappedName().isBlank()) {
                        writer.write(String.join(",", new String[] {
                            param.getUnmappedName(), param.getMappedName(), Integer.toString(param.getSide().toNumber())
                        }));
                        writer.newLine();
                    }
                }
            }
        }
    }

    public static MappingExport read(File export) {
        MappingExport mappings = new MappingExport();

        try (FileSystem fs = FileSystems.newFileSystem(export.toPath())) {

            // Functions/methods CSV file
            try (CSVReader reader = new CSVReader(Files.newBufferedReader(fs.getPath(METHODS_CSV)))) {
                reader.readNext(); // Skip header

                for (String[] line : reader) {
                    mappings
                        .addMethod(line[0], new MemberEntry(line[0], line[1], Side.from(Integer.parseInt(line[2])), line[3]));
                }
            } catch (CsvValidationException e) {
                throw new RuntimeException("Exception while reading " + METHODS_CSV, e);
            }

            // Fields CSV file
            try (CSVReader reader = new CSVReader(Files.newBufferedReader(fs.getPath(FIELDS_CSV)))) {
                reader.readNext(); // Skip header

                for (String[] line : reader) {
                    mappings
                        .addField(line[0], new MemberEntry(line[0], line[1], Side.from(Integer.parseInt(line[2])), line[3]));
                }
            } catch (CsvValidationException e) {
                throw new RuntimeException("Exception while reading " + FIELDS_CSV, e);
            }

            // Params CSV file
            try (CSVReader reader = new CSVReader(Files.newBufferedReader(fs.getPath(PARAMS_CSV)))) {
                reader.readNext(); // Skip header

                for (String[] line : reader) {
                    mappings.addParam(line[0], new ParamEntry(line[0], line[1], Side.from(Integer.parseInt(line[2]))));
                }
            } catch (CsvValidationException e) {
                throw new RuntimeException("Exception while reading " + PARAMS_CSV, e);
            }

        } catch (IOException e) {
            throw new RuntimeException("Exception while reading mappings export file", e);
        }

        return mappings;
    }
}
