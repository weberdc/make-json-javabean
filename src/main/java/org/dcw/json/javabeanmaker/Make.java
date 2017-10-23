package org.dcw.json.javabeanmaker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Maps;

public class Make {

    @Parameter(names = {"-f", "--fields-file"}, description = "File of field information")
    private String fieldsFile;
    @Parameter(names = {"-c", "--fqclassname"}, description = "Fully qualified name of class to generate")
    private String fqClassName = "org.dcw.FooBar";
    @Parameter(names = {"-g", "--getters"}, description = "Generate getters (default: false)")
    private boolean getters = false;
    @Parameter(names = {"-s", "--setters"}, description = "Generate setters (default: false)")
    private boolean setters = false;

    @Parameter(names = {"-h", "-?", "--help"}, description = "Help (default: false)")
    private static boolean help = false;

    private final StringBuilder code = new StringBuilder();
    private final Map<String, Info> fields = Maps.newTreeMap();

    public static void main(String[] args) throws IOException {
        Make theApp = new Make();

        // JCommander instance parses args, populates fields of theApp
        JCommander argsParser = JCommander.newBuilder()
                .addObject(theApp)
                .programName("bin/make-json-javabean[.bat]")
                .build();
        try {
            argsParser.parse(args);
        } catch (ParameterException e) {
            System.err.println("Unknown argument parameter:\n  " + e.getMessage());
            help = true;
        }

        if (help) {
            StringBuilder sb = new StringBuilder();
            argsParser.usage(sb);
            System.out.println(sb.toString());
            System.exit(-1);
        }

        theApp.run();
    }

    public void run() throws IOException {

        final String className = fqClassName.substring(fqClassName.lastIndexOf('.') + 1);
        final String pkgName = fqClassName.indexOf('.') != -1
            ? fqClassName.substring(0, fqClassName.lastIndexOf('.'))
            : null;

        final Path fieldsPath = Paths.get(new File(fieldsFile).toURI());
        Files.newBufferedReader(fieldsPath)
            .lines()
            .forEach(line -> {
                String[] parts = line.split("\\s");
                if (parts.length == 2) {
                    fields.put(parts[0], new Info(parts[1]));
                } else if (parts.length > 2) {
                    fields.put(parts[0], new Info(parts[1], parts[2]));
                }
            });

        if (pkgName != null) {
            type("package %s;", pkgName);
            newline();
        }

        type("import com.fasterxml.jackson.annotation.JsonInclude;");
        type("import com.fasterxml.jackson.annotation.JsonProperty;");
        newline();
        type("@JsonInclude(JsonInclude.Include.ALWAYS)");
        type("public class %s {", className);
        newline();
        forEachField((jsonName, type, fieldName) -> {
            type("    @JsonProperty(\"%s\")", jsonName);
            type("    private %s%s %s;", (setters ? "" : "final "), type, fieldName);
        });
        newline();
        type("    public %s(", className);
        forEachField((jsonName, type, fieldName) -> {
            type("        @JsonProperty(\"%s\")", jsonName);
            type("        final %s %s;", type, fieldName);
        });
        type("    ) {");
        forEachField((jsonName, type, fieldName) -> {
            type("        this.%s = %s;", fieldName, fieldName);
        });
        type("    }");
        newline();
        if (getters) {
            forEachField((jsonName, type, fieldName) -> {
                final String getMethodName = ! type.equalsIgnoreCase("boolean")
                    ? "get" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName)
                    : (fieldName.startsWith("is")
                        ? fieldName
                        : "is" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName));
                type("    public %s %s() {", type, getMethodName);
                type("        return %s;", fieldName);
                type("    }");
                newline();
            });
        }
        if (setters) {
            forEachField((jsonName, type, fieldName) -> {
                final String setMethodName = "set" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName);
                type("    public void %s(final %s %s) {", setMethodName, type, fieldName);
                type("        this.%s = %s;", fieldName, fieldName);
                type("    }");
                newline();
            });
        }
        type("}");

        System.out.println(code.toString());
    }

    private void forEachField(Obj3Consumer<String> action) {
        fields.forEach((k, v) -> {
            final String jsonName = k;
            final String type = v.type;
            final String fieldName = v.preferredFieldName != null ? v.preferredFieldName : jsonName;

            action.accept(jsonName, type, fieldName);
        });
    }

    private void newline() {
        type("");
    }

    private void type(String fmt, String... values) {
        code.append(String.format(fmt, (Object[]) values)).append('\n');
    }
}

@FunctionalInterface
interface Obj3Consumer<T> {
    void accept(T t1, T t2, T t3);
}

class Info {
    String type;
    String preferredFieldName;

    public Info(final String t) {
        this(t, null);
    }

    public Info(final String t, final String pfn) {
        this.type = t;
        this.preferredFieldName = pfn;
    }
}
