package cool.zhouxin.auto.constants;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import lombok.SneakyThrows;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Table;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zhouxin
 * @since 2019/12/10 15:08
 */
@AutoService(Processor.class)
public class AutoConstantsProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;

    private static final Modifier[] fieldModifierArray = new Modifier[]{Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL};
    private static Pattern fieldNamePattern = Pattern.compile("[A-Z]");
    private static String fieldNameJoiner = "_";
    private static String nestedClassNameJoiner = "$";
    private static String classDocPattern =
            "This is constants class for {@link $T}. It's just to create static final constants\n" +
                    "of its field name automatically. So it's not reasonable to modify it manually.\n";

    private static String fieldDocPattern = "This is field name constants for {@link $T#$L}.\n";
    private static String fieldColumnDocPattern = "This is field column name constants for \n {@link $T#name()} of {@link $T#$L}.\n";
    private static String fieldTableDocPattern = "This is field column name constants for \n @link $T#name()} of {@link $T}.\n";
    private static String fieldJoinTableDocPattern = "This is field column name constants for \n {@link $T#name()} of {@link $T#$L}.\n";
    private static String fieldJoinTableJoinColumnDocPattern = "This is field column name constants for \n {@link $T#joinColumns()} of {@link $T#$L}.\n";
    private static String fieldJoinTableInverseJoinColumnDocPattern = "This is field column name constants for \n {@link $T#inverseJoinColumns()} of {@link $T#$L}.\n";


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(AutoConstants.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.typeUtils = processingEnv.getTypeUtils();
        this.elementUtils = processingEnv.getElementUtils();
    }

    @SneakyThrows
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<TypeElement> elements = (Set<TypeElement>) roundEnv.getElementsAnnotatedWith(AutoConstants.class);
        for (TypeElement element : elements) {
            JavaFile javaFile = this.createJavaFile(element);
            javaFile.writeTo(this.filer);
        }
        return true;
    }

    private JavaFile createJavaFile(TypeElement element) {

        ClassName className = ClassName.get(element);
        List<FieldSpec> fieldElements = this.getFieldElements(element, className);

        String typeSimpleName = this.buildTypeName(element);
        TypeSpec newType = TypeSpec.classBuilder(typeSimpleName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc(classDocPattern, className)
                .addFields(fieldElements)
                .build();

        PackageElement packageElement = elementUtils.getPackageOf(element);

        // 加一个下划线作为结尾
        String packageName = packageElement.getQualifiedName().toString() + "_";
        JavaFile javaFile = JavaFile.builder(packageName, newType).build();
        return javaFile;
    }

    private String buildTypeName(TypeElement element) {

        Element enclosingElement = element.getEnclosingElement();
        ElementKind kind = enclosingElement.getKind();

        StringBuffer stringBuffer = new StringBuffer();
        // 如果等于class，说明此时的element是一个内部类，不能用简单的element.getSimpleName()
        // 名字应该类似于OuterName$InnerName
        if (kind.equals(ElementKind.CLASS)) {
            Name enclosingElementName = enclosingElement.getSimpleName();
            stringBuffer.append(enclosingElementName.toString())
                    .append(nestedClassNameJoiner)
                    .append(element.getSimpleName());
        }else {
            stringBuffer.append(element.getSimpleName().toString());
        }

        String name = stringBuffer.append(fieldNameJoiner).toString();
        return name;
    }

    private List<FieldSpec> getFieldElements(TypeElement typeElement, ClassName className) {
        Stream<FieldSpec> tableFieldElement = this.buildTableFieldElement(typeElement, className);
        Stream<FieldSpec> fieldElements = this.buildFieldElements(typeElement, className);
        return Stream.concat(tableFieldElement, fieldElements).collect(Collectors.toList());
    }

    private Stream<FieldSpec> buildFieldElements(TypeElement typeElement, ClassName className) {
        List<TypeElement> classList = this.getAllTypeElements(typeElement);
        return classList.stream()
                .map(TypeElement::getEnclosedElements)
                .flatMap(List::stream)
                .filter(VariableElement.class::isInstance)
                .map(VariableElement.class::cast)
                // 不需要静态的属性名字
                .filter(variableElement -> !variableElement.getModifiers().contains(Modifier.STATIC))
                .flatMap(e -> this.buildFieldElements(e, className));
    }
    private Stream<FieldSpec> buildTableFieldElement(TypeElement typeElement, ClassName className) {

        Table table = typeElement.getAnnotation(Table.class);
        if (table == null) return Stream.empty();

        String tableName = table.name();
        if (tableName.isEmpty()) {
            tableName = this.buildTableNameValue(className);
        }

        String typeElementName = typeElement.getSimpleName().toString();
        FieldSpec fieldTableSpec = FieldSpec.builder(String.class, this.buildFieldTableName(typeElementName), fieldModifierArray)
                .initializer("$S", tableName)
                .addJavadoc(fieldTableDocPattern, Table.class, className)
                .build();
        return Stream.of(fieldTableSpec);
    }

    @SneakyThrows
    private List<TypeElement> getAllTypeElements(TypeElement typeElement) {
        String typeElementName = typeElement.getQualifiedName().toString();
        List<TypeElement> typeElementList = new ArrayList<>();
        if (!typeElementName.equals(Object.class.getName())) {
            TypeElement supperElement = (TypeElement) typeUtils.asElement(typeElement.getSuperclass());
            List<TypeElement> supperElementClassList = this.getAllTypeElements(supperElement);
            typeElementList.addAll(supperElementClassList);
            typeElementList.add(typeElement);
        }
        return typeElementList;
    }

    private String buildTableNameValue(ClassName className) {
        StringBuilder builder = new StringBuilder(className.simpleName().replace('.', '_'));
        for (int i = 1; i < builder.length() - 1; i++) {
            if (isUnderscoreRequired(builder.charAt(i - 1), builder.charAt(i), builder.charAt(i + 1))) {
                builder.insert(i++, '_');
            }
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private boolean isUnderscoreRequired(char before, char current, char after) {
        return Character.isLowerCase(before) && Character.isUpperCase(current) && Character.isLowerCase(after);
    }

    private Stream<FieldSpec> buildFieldElements(VariableElement variableElement, ClassName className) {
        String fieldName = variableElement.getSimpleName().toString();
        FieldSpec fieldSpec = this.buildFieldElement(className, fieldName);

        List<FieldSpec> fieldSpecs = new ArrayList<>();
        fieldSpecs.add(fieldSpec);

        Optional<FieldSpec> fieldColumnSpecOptional = this.buildFieldColumnElement(variableElement, className, fieldName);
        fieldColumnSpecOptional.ifPresent(fieldSpecs::add);

        List<FieldSpec> fieldJoinTableSpecs = this.buildFieldJoinTableElement(variableElement, className, fieldName);
        fieldSpecs.addAll(fieldJoinTableSpecs);

        return fieldSpecs.stream();
    }

    private List<FieldSpec> buildFieldJoinTableElement(VariableElement variableElement, ClassName className, String fieldName) {
        List<FieldSpec> fieldSpecs = new ArrayList<>();
        JoinTable joinTable = variableElement.getAnnotation(JoinTable.class);
        if (joinTable != null) {
            String joinTableName = joinTable.name();

            FieldSpec fieldJoinTableSpec = FieldSpec.builder(String.class,
                    this.buildJoinTableName(joinTableName), fieldModifierArray)
                    .initializer("$S", joinTableName)
                    .addJavadoc(fieldJoinTableDocPattern, JoinTable.class, className, fieldName)
                    .build();
            fieldSpecs.add(fieldJoinTableSpec);

            JoinColumn[] joinColumns = joinTable.joinColumns();
            if (joinColumns != null && joinColumns.length > 0) {
                for (JoinColumn joinColumn : joinColumns) {
                    String joinColumnName = joinColumn.name();
                    FieldSpec fieldJoinTableColumnSpec = FieldSpec.builder(String.class,
                            this.buildJoinTableColumnName(joinTableName, joinColumnName), fieldModifierArray)
                            .initializer("$S", joinColumnName)
                            .addJavadoc(fieldJoinTableJoinColumnDocPattern, JoinTable.class, className, fieldName)
                            .build();
                    fieldSpecs.add(fieldJoinTableColumnSpec);
                }
            }
            JoinColumn[] inverseJoinColumns = joinTable.inverseJoinColumns();
            if (inverseJoinColumns != null && inverseJoinColumns.length > 0) {
                for (JoinColumn joinColumn : inverseJoinColumns) {
                    String joinColumnName = joinColumn.name();
                    FieldSpec fieldJoinTableColumnSpec = FieldSpec.builder(String.class,
                            this.buildJoinTableColumnName(joinTableName, joinColumnName), fieldModifierArray)
                            .initializer("$S", joinColumnName)
                            .addJavadoc(fieldJoinTableInverseJoinColumnDocPattern, JoinTable.class, className, fieldName)
                            .build();
                    fieldSpecs.add(fieldJoinTableColumnSpec);
                }
            }
        }
        return fieldSpecs;
    }



    private Optional<FieldSpec> buildFieldColumnElement(VariableElement variableElement, ClassName className, String fieldName) {
        Column column = variableElement.getAnnotation(Column.class);
        if (column != null) {
            String columnName = column.name();
            columnName = columnName.isEmpty() ? fieldName : columnName;
            FieldSpec fieldColumnSpec = FieldSpec.builder(String.class, this.buildFieldColumnName(fieldName), fieldModifierArray)
                    .initializer("$S", columnName)
                    .addJavadoc(fieldColumnDocPattern, Column.class, className, fieldName)
                    .build();
            return Optional.ofNullable(fieldColumnSpec);
        }
        return Optional.empty();
    }


    private FieldSpec buildFieldElement(ClassName className, String fieldName) {
        return FieldSpec.builder(String.class, this.buildFieldName(fieldName), fieldModifierArray)
                .initializer("$S", fieldName)
                .addJavadoc(fieldDocPattern, className, fieldName)
                .build();
    }

    private String buildJoinTableColumnName(String joinTableName, String fieldName) {
        return "JOINTABLE_" + joinTableName + "_COLUMN_" + fieldName;
    }

    private String buildJoinTableName(String joinTableName) {
        return "JOINTABLE_" + joinTableName;
    }

    private String buildFieldTableName(String typeName) {
        return "TABLE";
    }

    private String buildFieldColumnName(String fieldName) {
        return "COLUMN_" + fieldName;
    }

    private String buildFieldName(String fieldName) {
        return fieldName;
    }

    /**
     * nickName -> NICK_NAME的模式
     * @param fieldName
     * @return
     */
//    private String buildFieldName(String fieldName) {
//        Matcher matcher = fieldNamePattern.matcher(fieldName);
//        StringBuffer sb = new StringBuffer();
//        while (matcher.find()) {
//            matcher.appendReplacement(sb, fieldNameJoiner + matcher.group(0).toLowerCase());
//        }
//        matcher.appendTail(sb);
//        return sb.toString().toUpperCase();
//    }
}
