package com.github.overengineer.gunmetal.compiler;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;

/**
 * @author rees.byars
 */
public abstract class AbstractTypeVisitor<R, P> implements TypeVisitor<R, P> {

    @Override
    public R visit(TypeMirror typeMirror, P p) {
        throw new UnsupportedOperationException("The visitor type [" + this + "] does not support visiting TypeMirror [" + typeMirror + "]");
    }

    @Override
    public R visit(TypeMirror typeMirror) {
        throw new UnsupportedOperationException("The visitor type [" + this + "] does not support visiting TypeMirror [" + typeMirror + "]");
    }

    @Override
    public R visitPrimitive(PrimitiveType primitiveType, P p) {
        throw new UnsupportedOperationException("The visitor type [" + this + "] does not support visiting PrimitiveType [" + primitiveType + "]");
    }

    @Override
    public R visitNull(NullType nullType, P p) {
        throw new UnsupportedOperationException("The visitor type [" + this + "] does not support visiting NullType [" + nullType + "]");
    }

    @Override
    public R visitArray(ArrayType arrayType, P p) {
        throw new UnsupportedOperationException("The visitor type [" + this + "] does not support visiting ArrayType [" + arrayType + "]");
    }

    @Override
    public R visitDeclared(DeclaredType declaredType, P p) {
        throw new UnsupportedOperationException("The visitor type [" + this + "] does not support visiting DeclaredType [" + declaredType + "]");
    }

    @Override
    public R visitError(ErrorType errorType, P p) {
        throw new UnsupportedOperationException("The visitor type [" + this + "] does not support visiting ErrorType [" + errorType + "]");
    }

    @Override
    public R visitTypeVariable(TypeVariable typeVariable, P p) {
        throw new UnsupportedOperationException("The visitor type [" + this + "] does not support visiting TypeVariable [" + typeVariable + "]");
    }

    @Override
    public R visitWildcard(WildcardType wildcardType, P p) {
        throw new UnsupportedOperationException("The visitor type [" + this + "] does not support visiting WildcardType [" + wildcardType + "]");
    }

    @Override
    public R visitExecutable(ExecutableType executableType, P p) {
        throw new UnsupportedOperationException("The visitor type [" + this + "] does not support visiting ExecutableType [" + executableType + "]");
    }

    @Override
    public R visitNoType(NoType noType, P p) {
        throw new UnsupportedOperationException("The visitor type [" + this + "] does not support visiting NoType [" + noType + "]");
    }

    @Override
    public R visitUnknown(TypeMirror typeMirror, P p) {
        throw new UnsupportedOperationException("The visitor type [" + this + "] does not support visiting unknown TypeMirror [" + typeMirror + "]");
    }

    @Override
    public R visitUnion(UnionType t, P p) {
        throw new UnsupportedOperationException("The visitor type [" + this + "] does not support visiting union of type [" + t + "]");
    }

}
