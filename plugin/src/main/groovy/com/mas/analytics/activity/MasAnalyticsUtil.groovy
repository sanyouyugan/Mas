package com.mas.analytics.activity

import org.objectweb.asm.Opcodes

class MasAnalyticsUtil implements Opcodes {
    private static final HashSet<String> targetFragmentClass = new HashSet()
    private static final HashSet<String> targetMenuMethodDesc = new HashSet()

    static {
        /**
         * Menu
         */
        targetMenuMethodDesc.add("onContextItemSelected(Landroid/view/MenuItem;)Z")
        targetMenuMethodDesc.add("onOptionsItemSelected(Landroid/view/MenuItem;)Z")
        targetMenuMethodDesc.add("onNavigationItemSelected(Landroid/view/MenuItem;)Z")

        /**
         * Fragment
         */
        targetFragmentClass.add('android/support/v4/app/Fragment')
        targetFragmentClass.add('android/support/v4/app/ListFragment')
    }

    static boolean isPrivate(int access) {
        return (access & ACC_PRIVATE) != 0
    }

    static boolean isPublic(int access) {
        return (access & ACC_PUBLIC) != 0
    }

    static boolean isStatic(int access) {
        return (access & ACC_STATIC) != 0
    }

    static boolean isTargetMenuMethodDesc(String nameDesc) {
        return targetMenuMethodDesc.contains(nameDesc)
    }

    static boolean isTargetFragmentClass(String className) {
        return targetFragmentClass.contains(className)
    }

    static boolean isInstanceOfFragment(String superName) {
        return targetFragmentClass.contains(superName)
    }
}
