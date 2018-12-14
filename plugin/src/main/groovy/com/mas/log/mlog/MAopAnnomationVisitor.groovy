package com.mas.log.mlog

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Opcodes

class MAopAnnomationVisitor extends AnnotationVisitor implements Opcodes {
    ArrayList<String> params

    MAopAnnomationVisitor(int api) {
        super(api)
    }

    @Override
    void visit(String name, Object value) {
        if (name.equals("params")) {
            params = value
        }
    }

    @Override
    AnnotationVisitor visitArray(String name) {
        return getArrayVisitor(name)
    }

    AnnotationVisitor getArrayVisitor(String name) {
        return new MAopArrayAnnomationVisitor(ASM5, name)
    }

    class MAopArrayAnnomationVisitor extends AnnotationVisitor implements Opcodes {
        String name

        MAopArrayAnnomationVisitor(int api, String name) {
            super(api)
            this.name = name
        }

        @Override
        void visit(String name, Object value) {
            if (this.name.equals("params")) {
                if (params == null) {
                    params = new ArrayList<>()
                }
                params.add(value)
            }
        }

    }
}