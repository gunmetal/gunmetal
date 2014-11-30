package io.gunmetal.compiler;

import com.squareup.javawriter.JavaWriter;

import javax.annotation.processing.Filer;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * @author rees.byars
 */
class ProviderWriter {

    // TODO this is a complete, non-working hack ;)
    private final ProviderNameResolver nameResolver = new ProviderNameResolver();
    private final Filer filer;

    ProviderWriter(
            Filer filer) {
        this.filer = filer;
    }

    void writeProviderFor(Binding binding) throws IOException {

        TypeMirror typeMirror = binding.fulfilledDependency().typeMirror();

        String typeName = nameResolver.getName(typeMirror);

        JavaFileObject javaFileObject = filer.createSourceFile(typeName);
        JavaWriter javaWriter = new JavaWriter(javaFileObject.openWriter());

        int nameIndex = typeName.lastIndexOf(".");
        if (nameIndex > 0) {
            javaWriter.emitPackage(typeName.substring(0, nameIndex));
        }

        // TODO write qualifier annotation?

        javaWriter
                .beginType(typeName, "class", EnumSet.of(PUBLIC, FINAL))
                .beginConstructor(EnumSet.of(PUBLIC))
                .endConstructor()
                .endType()
                .close();
    }

    private static class ProviderNameResolver {

        private final Set<String> fileNames = new HashSet<>();

        private String getName(TypeMirror typeMirror) {
            return getName(typeMirror, 0);
        }

        private String getName(TypeMirror typeMirror, int index) {
            // TODO generics will screw this up
            String typeName = typeMirror.toString() + "_$Provider";
            if (index != 0) {
                typeName += index;
            }
            if (fileNames.contains(typeName)) {
                return getName(typeMirror, index + 1);
            }
            fileNames.add(typeName);
            return typeName;
        }

    }

}
