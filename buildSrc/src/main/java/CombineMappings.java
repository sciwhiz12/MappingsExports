import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

public class CombineMappings extends DefaultTask {
    @Input File baseExportZip;
    @Input File spreadsheet;
    @Input String placeholderClassName;
    @Input boolean allowUnvalidated;

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
        final MappingExport baseExport = MappingExport.read(baseExportZip);
        final MappingSpreadsheet outputMappings = new MappingSpreadsheet();

        for (MappingSpreadsheet.Entry field : mappings.getFields().values()) {
            if (field.isValidated() || allowUnvalidated) {
                outputMappings.addField(field.getUnmappedName(), field);
            }
        }

        for (MappingSpreadsheet.Entry method : mappings.getMethods().values()) {
            if (method.isValidated() || allowUnvalidated) {
                outputMappings.addMethod(method.getUnmappedName(), method);
            }
        }

        for (MappingSpreadsheet.Entry param : mappings.getParams().values()) {
            if (param.isValidated() || allowUnvalidated) {
                outputMappings.addParam(param.getUnmappedName(), param);
            }
        }

        for (MappingExport.MemberEntry field : baseExport.getFields().values()) {
            if (!outputMappings.hasField(field.getUnmappedName())) {
                outputMappings.addField(field.getUnmappedName(), new MappingSpreadsheet.Entry(true,
                    placeholderClassName, field.getUnmappedName(), field.getMappedName(), field.getSide(), field.getJavadoc()));
            }
        }

        for (MappingExport.MemberEntry method : baseExport.getMethods().values()) {
            if (!outputMappings.hasMethod(method.getUnmappedName())) {
                outputMappings.addMethod(method.getUnmappedName(), new MappingSpreadsheet.Entry(true,
                    placeholderClassName, method.getUnmappedName(), method.getMappedName(), method.getSide(),
                    method.getJavadoc()));
            }
        }

        for (MappingExport.ParamEntry param : baseExport.getParams().values()) {
            if (!outputMappings.hasParam(param.getUnmappedName())) {
                outputMappings.addParam(param.getUnmappedName(), new MappingSpreadsheet.Entry(true,
                    placeholderClassName, param.getUnmappedName(), param.getMappedName(), param.getSide(), ""));
            }
        }

        outputMappings.write(output);
    }
}
