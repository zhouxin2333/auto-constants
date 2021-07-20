package cool.zhouxin.auto.constants;

import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;

@FunctionalInterface
interface GetTypeMirror {

    void execute() throws MirroredTypeException, MirroredTypesException;
}
