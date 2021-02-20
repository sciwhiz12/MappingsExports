import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

public class GenerateMappingExport extends DefaultTask {
    @Input File spreadsheet;

    @Input
    @OutputFile
    File output;

    @TaskAction
    public void act() throws IOException {
        if (output.exists() && !output.delete()) {
            getLogger().error("Unable to delete existing output at " + output);
        }
        if (output.getParentFile() != null && !output.getParentFile().exists() && !output.getParentFile().mkdirs()) {
            getLogger().error("Unable to create parent directories for " + output);
        }

        final MappingSpreadsheet mappings = MappingSpreadsheet.read(spreadsheet);
        final MappingExport export = new MappingExport();

        for (MappingSpreadsheet.Entry field : mappings.getFields().values()) {
            export.addField(field.getUnmappedName(), new MappingExport.MemberEntry(field.getUnmappedName(),
                field.getMappedName(), field.getSide(), field.getJavadocComment()));
        }

        for (MappingSpreadsheet.Entry method : mappings.getMethods().values()) {
            export.addMethod(method.getUnmappedName(), new MappingExport.MemberEntry(method.getUnmappedName(),
                method.getMappedName(), method.getSide(), method.getJavadocComment()));
        }

        for (MappingSpreadsheet.Entry param : mappings.getParams().values()) {
            export.addParam(param.getUnmappedName(), new MappingExport.ParamEntry(param.getUnmappedName(),
                param.getMappedName(), param.getSide()));
        }

        export.write(output);
    }
}