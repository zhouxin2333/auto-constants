package cool.zhouxin.auto.constants;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import lombok.SneakyThrows;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private static String classDocPattern =
            "This is constants class for {@link $L}. It's just to create static final constants\n" +
            "of its field name automatically. So it's not reasonable to modify it manually.\n";

    private static String fieldDocPattern = "This is field name constants for {@link $L#$L}.\n";


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


        List<FieldSpec> fieldElements = this.getFieldElements(element);

        Name typeSimpleName = element.getSimpleName();
        TypeSpec newType = TypeSpec.classBuilder(typeSimpleName.toString() + fieldNameJoiner)
                                   .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                   .addJavadoc(classDocPattern, typeSimpleName)
                                   .addFields(fieldElements)
                                   .build();

        PackageElement packageElement = elementUtils.getPackageOf(element);
        String packageName = packageElement.getQualifiedName().toString();
        JavaFile javaFile = JavaFile.builder(packageName, newType).build();
        return javaFile;
    }


    private List<FieldSpec> getFieldElements(TypeElement typeElement) {
        Name typeSimpleName = typeElement.getSimpleName();
        List<TypeElement> classList = this.getAllTypeElements(typeElement);
        return classList.stream()
                        .map(TypeElement::getEnclosedElements)
                        .flatMap(List::stream)
                        .filter(VariableElement.class::isInstance)
                        .map(VariableElement.class::cast)
                        .map(e -> this.buildFieldElement(e, typeSimpleName))
                        .collect(Collectors.toList());
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

    private FieldSpec buildFieldElement(VariableElement variableElement, Name typeSimpleName) {
        String fieldName = variableElement.getSimpleName().toString();
        return FieldSpec.builder(String.class, this.buildFieldName(fieldName), fieldModifierArray)
                        .initializer("$S", fieldName)
                        .addJavadoc(fieldDocPattern, typeSimpleName, fieldName)
                        .build();
    }

    private String buildFieldName(String fieldName) {
        Matcher matcher = fieldNamePattern.matcher(fieldName);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, fieldNameJoiner + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString().toUpperCase();
    }
}
