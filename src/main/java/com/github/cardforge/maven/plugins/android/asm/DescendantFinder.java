/*
 * Copyright (C) 2009 Jayway AB
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
package com.github.cardforge.maven.plugins.android.asm;

import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.*;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Finds descendants of any class from specific parent packages.
 * Will remember if any match was found, and returns that fact in {@link #isDescendantFound()}.
 *
 * @author hugo.josefson@jayway.com
 */
class DescendantFinder extends ClassVisitor {

    private final String[] parentPackages;
    private final AtomicBoolean isDescendantFound = new AtomicBoolean(false);

    /**
     * Constructs this finder.
     *
     * @param parentPackages Packages to find descendants of. Must be formatted with <code>/</code> (slash) instead of
     *                       <code>.</code> (dot). For example: <code>junit/framework/</code>
     */
    DescendantFinder(String... parentPackages) {
        super(Opcodes.ASM4);
        this.parentPackages = parentPackages;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        for (String testPackage : parentPackages) {
            if (StringUtils.startsWith(superName, testPackage)) {
                flagAsFound();
            }
        }
    }

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

    @Override
    public void visitSource(String source, String debug) {
        // Empty
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        // Empty
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return null;
    }

    @Override
    public void visitAttribute(Attribute attr) {
        // Empty
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        // Empty
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return null;
    }

    @Override
    public void visitEnd() {
        // Empty
    }
}