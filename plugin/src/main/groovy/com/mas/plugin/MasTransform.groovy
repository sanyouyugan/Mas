package com.mas.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.mas.analytics.activity.MasAnalyticsClassVisitor
import com.mas.log.dynamiclog.LogClassVisitor
import com.mas.log.mlog.Aops
import groovy.io.FileType
import groovy.json.JsonSlurper
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.util.CheckClassAdapter

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 *
 */
class MasTransform extends Transform {
    private static Project project
    private
    static HashSet<String> exclude = ['.R\$',
                                      '.R',
                                      'android.support',
                                      "cc.org",
                                      "cc.chenhe.lib.androidlua",
                                      "com.zmsoft.missile.missileservice",
                                      "com.android.support",
                                      "com.google.protobuf",
                                      "cc.chenhe:android-lua",
                                      "com.dfire.mobile.component:missile-wrapper",
                                      "org.ow2.asm:asm"]
    private static HashSet<String> include = [
            "com.squareup.okhttp3:okhttp"
    ]

    private static HashMap<String, Aops> aopsMap = new HashMap<>()

    protected static boolean disableJar

    MasTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "MasAop"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    /**
     * 打印提示信息
     */
    static void printCopyRight() {
        println()
        println("####################################################################")
        println("########                                                    ########")
        println("########                                                    ########")
        println("########                   欢迎使用Mas编译插件                 ########")
        println("########                                                    ########")
        println("########                                                    ########")
        println("####################################################################")
        println()
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        /**
         * 打印提示信息
         */
        printCopyRight()


        if (!incremental) {
            outputProvider.deleteAll()
        }

        disableJar = project.monitor.disableJar
        HashSet<String> excludePackages = project.monitor.exclude
        if (excludePackages != null) {
            exclude.addAll(excludePackages)
        }

        HashSet<String> includePackages = project.monitor.include
        if (includePackages != null) {
            include.addAll(includePackages)
        }
        //文件中的aop
        handleAopsFiles(project.monitor.maops)

        //处理根Aops
        handleAopsString(Aops.rootAops)

        /**
         * 遍历输入文件
         */
        inputs.each { TransformInput input ->
            /**
             * 遍历 jar
             */
            input.jarInputs.each { JarInput jarInput ->
                String destName = jarInput.file.name
                Logger.info("开始遍历 jar：" + jarInput.file.absolutePath)

                /**
                 * 截取文件路径的md5值重命名输出文件,因为可能同名,会覆盖
                 */
                def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath).substring(0, 8)
                if (destName.endsWith(".jar")) {
                    destName = destName.substring(0, destName.length() - 4)
                }
                /** 获得输出文件*/
                File dest = outputProvider.getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR)

                def modifiedJar = null
                if (!project.monitor.disableJar) {
                    modifiedJar = modifyJarFile(jarInput, context.getTemporaryDir())
                }
                if (modifiedJar == null) {
                    modifiedJar = jarInput.file
                }
                FileUtils.copyFile(modifiedJar, dest)

                Logger.info("结束遍历 jar：" + jarInput.file.absolutePath)
            }

            /**
             * 遍历目录
             */
            input.directoryInputs.each { DirectoryInput directoryInput ->
                File dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                //Logger.info("||-->开始遍历特定目录  ${dest.absolutePath}")
                File dir = directoryInput.file
                if (dir) {
                    HashMap<String, File> modifyMap = new HashMap<>()
                    dir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) {
                        File classFile ->
                            File modified = modifyClassFile(dir, classFile, context.getTemporaryDir())
                            if (modified != null) {
                                //key为相对路径
                                modifyMap.put(classFile.absolutePath.replace(dir.absolutePath, ""), modified)
                            }
                    }
                    FileUtils.copyDirectory(directoryInput.file, dest)
                    modifyMap.entrySet().each {
                        Map.Entry<String, File> en ->
                            File target = new File(dest.absolutePath + en.getKey())
                            if (target.exists()) {
                                target.delete()
                            }
                            FileUtils.copyFile(en.getValue(), target)
                            en.getValue().delete()
                    }
                }
            }
        }
    }

/**
 * 处理aops
 * @param aopPath
 */
    void handleAopsFiles(String aopPath) {
        Logger.info("处理aops文件：" + aopPath)
        if (aopPath) {
            try {
                File jsonFile = project.file(aopPath)
                if (jsonFile.exists() && jsonFile.text) {
                    List states = new JsonSlurper().parseText(jsonFile.text)
                    states.each {
                        if (it.clazz && it.name) {
                            String index = it.clazz + it.name
                            aopsMap.put(index.replace(".", "/"), new Aops(it.clazz, it.name, it.params, it.dependent))
                            if (it.dependent) {
                                include.add(it.dependent)
                            }
                            //aops中的类需要扫描
                            include.add(it.clazz)
                        }
                    }
                }
            } catch (Exception e) {
                Logger.info("处理aops文件：" + aopPath + " 失败")
                Logger.info(e)
            }
        }
    }

    /**
     * 处理aops
     * @param aopPath
     */
    void handleAopsString(String content) {
        Logger.info("处理aops String：" + content)
        try {
            if (content) {
                List states = new JsonSlurper().parseText(content.trim())
                states.each {
                    if (it.clazz && it.name) {
                        String index = it.clazz + it.name
                        aopsMap.put(index.replace(".", "/"), new Aops(it.clazz, it.name, it.params, it.dependent))
                        if (it.dependent) {
                            include.add(it.dependent)
                        }
                        //aops中的类需要扫描
                        include.add(it.clazz)
                    }
                }
            }
        } catch (Exception e) {
            Logger.info("处理aops String：" + content + " 失败")
            Logger.info(e)
        }
    }
/**
 *
 * 是否需要扫描引用的jar文件
 *
 * @param jarName
 * @return
 */
    private static boolean isShouldModifyJar(String jarName) {

        String name = jarName


        if (jarName.contains(":")) {
            //jar是依赖 类似com.squareup.okhttp3:okhttp:
            //或者是android.local.jars:commons-codec-1.8.jar
            String[] names = jarName.split(":")
            if (names != null && names.length > 0 && names.length >= 2) {
                if (names[0] != "android.local.jars") {
                    name = names[0] + ":"
                } else {
                    name = ""
                }
                name = name + names[1]
            }
        }

        Iterator<String> iterator = include.iterator()
        while (iterator.hasNext()) {
            String packageName = iterator.next()
            if (name == packageName) {
                return true
            }
        }

        //如果在exlude里面
        Iterator<String> iterator1 = exclude.iterator()
        while (iterator1.hasNext()) {
            String packageName = iterator1.next()
            if (name.contains(packageName)) {
                return false
            }
        }
        return false
    }

/**
 *
 * @param className
 * @return
 */
    private static boolean isShouldModifyClass(String className) {

        //如果在include里面
        Iterator<String> iteratorIn = include.iterator()
        while (iteratorIn.hasNext()) {
            String packageName = iteratorIn.next()
            if (className.contains(packageName)) {
                return true
            }
        }

        //如果在exlude里面
        Iterator<String> iterator = exclude.iterator()
        while (iterator.hasNext()) {
            String packageName = iterator.next()
            if (className.contains(packageName)) {
                return false
            }
        }

        return true
    }

    /**
     *
     * @param className
     * @return
     */
    private static boolean isShouldModifyJarClass(String className) {

        //如果在include里面
        Iterator<String> iteratorIn = include.iterator()
        while (iteratorIn.hasNext()) {
            String packageName = iteratorIn.next()
            if (className.contains(packageName)) {
                return true
            }
        }

        //如果在exlude里面
        Iterator<String> iterator = exclude.iterator()
        while (iterator.hasNext()) {
            String packageName = iterator.next()
            if (className.contains(packageName)) {
                return false
            }
        }

        return false
    }

    /**
     * 修改 jar 文件中对应字节码
     */
    private static File modifyJarFile(JarInput jarInput, File tempDir) {
        if (jarInput && jarInput.file) {
            if (isShouldModifyJar(jarInput.getName())) {
                return modifyJar(jarInput.file, tempDir, true)
            }
        }
        return null
    }

    static File modifyJar(File jarFile, File tempDir, boolean nameHex) {
        /**
         * 读取原 jar
         */
        def file = new JarFile(jarFile)

        /**
         * 设置输出到的 jar
         */
        def hexName = ""
        if (nameHex) {
            hexName = DigestUtils.md5Hex(jarFile.absolutePath).substring(0, 8)
        }
        def outputJar = new File(tempDir, hexName + jarFile.name)
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJar))
        Enumeration enumeration = file.entries()

        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement()
            InputStream inputStream = file.getInputStream(jarEntry)

            String entryName = jarEntry.getName()
            String className

            ZipEntry zipEntry = new ZipEntry(entryName)

            jarOutputStream.putNextEntry(zipEntry)

            byte[] modifiedClassBytes = null
            byte[] sourceClassBytes = IOUtils.toByteArray(inputStream)
            if (entryName.endsWith(".class")) {
                className = entryName.replace("/", ".").replace(".class", "")
                if (isShouldModifyJarClass(className)) {
                    modifiedClassBytes = modifyClasses(className, sourceClassBytes)
                }
            }
            if (modifiedClassBytes == null) {
                jarOutputStream.write(sourceClassBytes)
            } else {
                jarOutputStream.write(modifiedClassBytes)
            }
            jarOutputStream.closeEntry()
        }
        jarOutputStream.close()
        file.close()
        return outputJar
    }

    static byte[] modifyClasses(String className, byte[] srcByteCode) {
        byte[] classBytesCode
        try {
            classBytesCode = modifyClass(srcByteCode)
            return classBytesCode
        } catch (Exception e) {
            throw e
        }
        if (classBytesCode == null) {
            classBytesCode = srcByteCode
        }
        return classBytesCode
    }
    /**
     * 真正修改类中方法字节码
     */
    private static byte[] modifyClass(byte[] srcClass) throws Exception {


        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES)

        ClassVisitor dlog = new LogClassVisitor(classWriter, aopsMap, project.monitor.dylog)

        ClassVisitor classVisitor = new MasAnalyticsClassVisitor(dlog)

//      ClassVisitor mAopVisitor = new MAopClassVisitor(classVisitor, aopsMap)

        ClassReader cr = new ClassReader(srcClass)

        cr.accept(classVisitor, ClassReader.EXPAND_FRAMES)

        byte[] bytes = classWriter.toByteArray()

        return bytes
    }

    /**
     * 目录文件中修改对应字节码
     */
    private static File modifyClassFile(File dir, File classFile, File tempDir) {
        File modified = null
        FileOutputStream outputStream = null
        try {
            String className = path2ClassName(classFile.absolutePath.replace(dir.absolutePath + File.separator, ""))
            if (isShouldModifyClass(className)) {
                byte[] sourceClassBytes = IOUtils.toByteArray(new FileInputStream(classFile))
                byte[] modifiedClassBytes = modifyClasses(className, sourceClassBytes)
                if (modifiedClassBytes) {
                    modified = new File(tempDir, className.replace('.', '') + '.class')
                    if (modified.exists()) {
                        modified.delete()
                    }
                    modified.createNewFile()
                    outputStream = new FileOutputStream(modified)
                    outputStream.write(modifiedClassBytes)
                }
            } else {
                return classFile
            }
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close()
                }
            } catch (Exception e) {
                //ignore
            }
        }
        return modified
    }

    static String path2ClassName(String pathName) {
        pathName.replace(File.separator, ".").replace(".class", "")
    }
}