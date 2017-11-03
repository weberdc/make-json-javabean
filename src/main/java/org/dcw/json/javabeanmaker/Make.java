/*
 * Copyright 2017 Derek Weber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    @Parameter(names = {"-j", "--javadoc"}, description = "Generate javadoc (default: false)")
    private boolean javadoc = false;

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
            .map(l -> l.split("#")[0])
            .forEach(line -> {
                String[] parts = line.split("\\s+");
                if (parts.length == 2) {
                    fields.put(parts[0], new Info(parts[1]));
                } else if (parts.length > 2) {
                    fields.put(parts[0], new Info(parts[1], parts[2]));
                }
            });

        packageStatement(pkgName);
        imports();
        classDeclaration(className);
        fields();
        constructor(className);
        gettersAndSetters();
        closeClassDeclaration();

        System.out.println(code.toString());
    }

    private void closeClassDeclaration() {
        if (code.charAt(code.length() - 1) == '\n') {
            // remove a blank line before the closing brace, if necessary
            code.deleteCharAt(code.length() - 1);
        }
        type("}");
    }

    private void gettersAndSetters() {
        forEachField((jsonName, type, fieldName) -> {
            if (getters) {
                final String getMethodName = ! type.equalsIgnoreCase("boolean")
                    ? "get" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName)
                    : (fieldName.startsWith("is")
                        ? fieldName
                        : "is" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName));
                if (javadoc) {
                    type("    /**");
                    type("     * Returns the <code>%s</code> property value from the {@link #%s} field.",
                         jsonName, fieldName);
                    type("     */");
                }
                type("    public %s %s() {", type, getMethodName);
                type("        return %s;", fieldName);
                type("    }");
                newline();
            }
            if (setters) {
                final String setMethodName = "set" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName);
                if (javadoc) {
                    type("    /**");
                    type("     * Sets the <code>%s</code> property value to <code>%s</code>.", jsonName, fieldName);
                    type("     *");
                    type("     * @param %s The new value for the <code>%s</code> property.", fieldName, jsonName);
                    type("     */");
                }
                type("    public void %s(final %s %s) {", setMethodName, type, fieldName);
                type("        this.%s = %s;", fieldName, fieldName);
                type("    }");
                newline();
            }
        });
    }

    private void constructor(String className) {
        if (javadoc) {
            type("    /**");
            type("     * Constructor.");
            type("     *");
            forEachField((jsonName, type, fieldName) -> {
                type("     * @param %s The <code>%s</code> property.", fieldName, jsonName);
            });
            type("     */");
        }
        type("    public %s(", className);
        forEachField((jsonName, type, fieldName) -> {
            type("        @JsonProperty(\"%s\")", jsonName);
            type("        final %s %s,", type, fieldName);
        });
        if (code.charAt(code.length() - 2) == ',') {
            // remove a trailing comma before the newline, if necessary
            code.deleteCharAt(code.length() - 2);
        }
        type("    ) {");
        forEachField((jsonName, type, fieldName) -> {
            type("        this.%s = %s;", fieldName, fieldName);
        });
        type("    }");
        newline();
    }

    private void fields() {
        forEachField((jsonName, type, fieldName) -> {
            type("    @JsonProperty(\"%s\")", jsonName);
            type("    private %s%s %s;", (setters ? "" : "final "), type, fieldName);
        });
        newline();
    }

    private void classDeclaration(String className) {
        if (javadoc) {
            type("/**");
            type(" * Description of %s JavaBean.", className);
            type(" *");
            type(" * @author %s", System.getProperty("user.name"));
            type(" */");
        }
        type("@JsonInclude(JsonInclude.Include.ALWAYS)");
        type("public class %s {", className);
        newline();
    }

    private void imports() {
        type("import com.fasterxml.jackson.annotation.JsonInclude;");
        type("import com.fasterxml.jackson.annotation.JsonProperty;");
        newline();
    }

    private void packageStatement(String pkgName) {
        if (pkgName != null) {
            type("package %s;", pkgName);
            newline();
        }
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
