package com.github.cardforge.maven.plugins.android.asm;

import org.objectweb.asm.*;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Finds classes annotated with a set of annotations.
 *
 * @author secondsun@gmail.com
 */
public class AnnotatedFinder extends ClassVisitor {

    /**
     *
     */
    private static final String TEST_RUNNER = "Lorg/junit/runner/RunWith;";
    /**
     *
     */
    private final AtomicBoolean isDescendantFound = new AtomicBoolean(false);

    /**
     * @param parentPackages Unused. the parent packages to search for.
     */
    public AnnotatedFinder(String[] parentPackages) {
        super(Opcodes.ASM4);
    }

    /**
     * @param version    the class version.
     * @param access     the class's access flags (see {@link Opcodes}). This parameter
     *                   also indicates if the class is deprecated.
     * @param name       the internal name of the class (see
     *                   {@link Type#getInternalName() getInternalName}).
     * @param signature  the signature of this class. May be <code>null</code> if the class
     *                   is not a generic one, and does not extend or implement generic
     *                   classes or interfaces.
     * @param superName  the internal of name of the super class (see
     *                   {@link Type#getInternalName() getInternalName}). For
     *                   interfaces, the super class is {@link Object}. May be
     *                   <code>null</code>, but only for the {@link Object} class.
     * @param interfaces the internal names of the class's interfaces (see
     *                   {@link Type#getInternalName() getInternalName}). May be
     *                   <code>null</code>.
     */
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        // empty
    }

    /**
     * Flags that a match was found.
     *
     * @see #isDescendantFound()
     */
    private void flagAsFound() {
        isDescendantFound.set(true);
    }

    /**
     * Returns whether a match was found.
     *
     * @return <code>true</code> is a match was found, <code>false</code> otherwise.
     */
    public boolean isDescendantFound() {
        return isDescendantFound.get();
    }

    /**
     * @param source the name of the source file from which the class was compiled.
     *               May be <code>null</code>.
     * @param debug  additional debug information to compute the correspondance
     *               between source and compiled elements of the class. May be
     *               <code>null</code>.
     */
    @Override
    public void visitSource(String source, String debug) {
        // Empty
    }

    /**
     * @param owner internal name of the enclosing class of the class.
     * @param name  the name of the method that contains the class, or
     *              <code>null</code> if the class is not enclosed in a method of its
     *              enclosing class.
     * @param desc  the descriptor of the method that contains the class, or
     *              <code>null</code> if the class is not enclosed in a method of its
     *              enclosing class.
     */
    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        // Empty
    }

    /**
     * @param desc    the class descriptor of the annotation class.
     * @param visible <code>true</code> if the annotation is visible at runtime.
     * @return a visitor to visit the annotation values, or <code>null</code> if this
     * visitor is not interested in visiting this annotation.
     */
    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (TEST_RUNNER.equals(desc)) {
            return new AnnotationVisitor(Opcodes.ASM4) {

                /**
                 * @param name  the value name.
                 * @param value the actual value, whose type must be {@link Byte},
                 *              {@link Boolean}, {@link Character}, {@link Short},
                 *              {@link Integer} , {@link Long}, {@link Float}, {@link Double},
                 *              {@link String} or {@link Type} or OBJECT or ARRAY sort. This
                 *              value can also be an array of byte, boolean, short, char, int,
                 *              long, float or double values (this is equivalent to using
                 *              {@link #visitArray visitArray} and visiting each array element
                 *              in turn, but is more convenient).
                 */
                @Override
                public void visit(String name, Object value) {
                    if (value instanceof Type t && (t.getClassName().contains("AndroidJUnit4"))) {
                        flagAsFound();
                    }

                }

            };
        }
        return null;
    }

    /**
     * @param attr an attribute.
     */
    @Override
    public void visitAttribute(Attribute attr) {
        // Empty
    }

    /**
     * Does nothing.
     *
     * @param name      the internal name of an inner class (see
     *                  {@link Type#getInternalName() getInternalName}).
     * @param outerName the internal name of the class to which the inner class
     *                  belongs (see {@link Type#getInternalName() getInternalName}).
     *                  May be <code>null</code> for not member classes.
     * @param innerName the (simple) name of the inner class inside its enclosing
     *                  class. May be <code>null</code> for anonymous inner classes.
     * @param access    the access flags of the inner class as originally declared in
     *                  the enclosing class.
     */
    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        // Empty
    }

    /**
     * @param access    the field's access flags (see {@link Opcodes}). This parameter
     *                  also indicates if the field is synthetic and/or deprecated.
     * @param name      the field's name.
     * @param desc      the field's descriptor (see {@link Type Type}).
     * @param signature the field's signature. May be <code>null</code> if the field's
     *                  type does not use generic types.
     * @param value     the field's initial value. This parameter, which may be
     *                  <code>null</code> if the field does not have an initial value,
     *                  must be an {@link Integer}, a {@link Float}, a {@link Long}, a
     *                  {@link Double} or a {@link String} (for <code>int</code>,
     *                  <code>float</code>, <code>long</code> or <code>String</code> fields
     *                  respectively). <i>This parameter is only used for static
     *                  fields</i>. Its value is ignored for non-static fields, which
     *                  must be initialized through bytecode instructions in
     *                  constructors or methods.
     * @return null, does nothing
     */
    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return null;
    }

    /**
     * @param access     the method's access flags (see {@link Opcodes}). This
     *                   parameter also indicates if the method is synthetic and/or
     *                   deprecated.
     * @param name       the method's name.
     * @param desc       the method's descriptor (see {@link Type Type}).
     * @param signature  the method's signature. May be <code>null</code> if the method
     *                   parameters, return type and exceptions do not use generic
     *                   types.
     * @param exceptions the internal names of the method's exception classes (see
     *                   {@link Type#getInternalName() getInternalName}). May be
     *                   <code>null</code>.
     * @return null, does nothing
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return null;
    }

    /**
     * Does nothing
     */
    @Override
    public void visitEnd() {
        // empty
    }

}
