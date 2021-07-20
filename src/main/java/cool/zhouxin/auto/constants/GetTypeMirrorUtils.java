package cool.zhouxin.auto.constants;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
 * @author zhouxin
 * @since 2021/7/20 18:29
 */
class GetTypeMirrorUtils {

    public static List<? extends DeclaredType> getDeclaredList(GetTypeMirror getTypeMirror) {
        return (List<? extends DeclaredType>) getList(getTypeMirror);
    }

    public static List<? extends TypeMirror> getList(GetTypeMirror getTypeMirror) {
        try {
            getTypeMirror.execute();
        } catch(MirroredTypesException ex) {
            return ex.getTypeMirrors();
        }
        return null;
    }

    public static TypeMirror get(GetTypeMirror getTypeMirror) {
        try {
            getTypeMirror.execute();
        } catch(MirroredTypeException ex) {
            return ex.getTypeMirror();
        }
        return null;
    }
}
