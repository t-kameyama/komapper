package org.komapper.codegen;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CodeGenerator {

  private final String packageName;
  private final String prefix;
  private final String suffix;
  private final List<Table> tables;

  public CodeGenerator(
      @Nullable String packageName,
      @NotNull String prefix,
      @NotNull String suffix,
      @NotNull List<Table> tables) {
    this.packageName = packageName;
    this.prefix = Objects.requireNonNull(prefix);
    this.suffix = Objects.requireNonNull(suffix);
    this.tables = new ArrayList<>(Objects.requireNonNull(tables));
  }

  @NotNull
  public Writer createNewFile(
      @NotNull Path destinationDir, @NotNull String fileName, boolean overwrite)
      throws IOException {
    Objects.requireNonNull(destinationDir);
    var file = createFilePath(destinationDir, fileName);
    if (Files.exists(file) && !overwrite) {
      return Writer.nullWriter();
    }
    Files.createDirectories(file.getParent());
    return new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8);
  }

  public void generateEntities(
      @NotNull Writer writer, boolean declareAsNullable, @NotNull PropertyTypeResolver resolver) {
    Objects.requireNonNull(writer);
    Objects.requireNonNull(resolver);
    var p = new PrintWriter(writer);
    if (packageName != null) {
      p.println("package " + packageName);
    }
    for (Table table : tables) {
      p.println();
      var className = StringUtil.snakeToUpperCamelCase(table.getName());
      p.println("data class " + prefix + className + suffix + " (");
      for (Column column : table.getColumns()) {
        var propertyName = StringUtil.snakeToLowerCamelCase(column.getName());
        var nullable = (declareAsNullable || column.isNullable()) ? "?" : "";
        var propertyClassName = resolver.resolve(table, column);
        p.println("    val " + propertyName + ": " + propertyClassName + nullable + ",");
      }
      p.println(")");
    }
  }

  public void generateDefinitions(@NotNull Writer writer, boolean useCatalog, boolean useSchema) {
    Objects.requireNonNull(writer);
    var p = new PrintWriter(writer);
    if (packageName != null) {
      p.println("package " + packageName);
      p.println();
    }
    p.println("import org.komapper.annotation.KomapperAutoIncrement");
    p.println("import org.komapper.annotation.KomapperColumn");
    p.println("import org.komapper.annotation.KomapperEntityDef");
    p.println("import org.komapper.annotation.KomapperId");
    p.println("import org.komapper.annotation.KomapperTable");
    for (Table table : tables) {
      p.println();
      var className = StringUtil.snakeToUpperCamelCase(table.getName());
      p.println("@KomapperEntityDef(" + className + "::class)");
      var tableArgs = new StringBuilder();
      tableArgs.append('"').append(table.getName()).append('"');
      if (useCatalog && table.getCatalog() != null) {
        tableArgs.append(", ");
        tableArgs.append('"').append(table.getCatalog()).append('"');
      }
      if (useSchema && table.getSchema() != null) {
        tableArgs.append(", ");
        tableArgs.append('"').append(table.getSchema()).append('"');
      }
      p.println("@KomapperTable(" + tableArgs + ")");
      p.println("data class " + prefix + className + suffix + "Def (");
      for (Column column : table.getColumns()) {
        var propertyName = StringUtil.snakeToLowerCamelCase(column.getName());
        var id = column.isPrimaryKey() ? "@KomapperId " : "";
        var autoIncrement = column.isAutoIncrement() ? "@KomapperAutoIncrement " : "";
        p.println(
            "    "
                + id
                + autoIncrement
                + "@KomapperColumn("
                + '"'
                + column.getName()
                + '"'
                + ") val "
                + propertyName
                + ": Nothing,");
      }
      p.println(")");
    }
  }

  private Path createFilePath(Path destinationDir, String name) {
    Path packageDir;
    if (packageName == null) {
      packageDir = destinationDir;
    } else {
      String path = packageName.replace(".", "/");
      packageDir = destinationDir.resolve(path);
    }
    return packageDir.resolve(name);
  }
}