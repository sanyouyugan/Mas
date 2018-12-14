package com.mas.log.dynamiclog

import com.mas.log.mlog.Aops
import com.mas.log.mlog.MAopAnnomationVisitor
import com.mas.plugin.Logger
import org.gradle.util.CollectionUtils
import org.objectweb.asm.*
import org.objectweb.asm.commons.LocalVariablesSorter

import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger

/**
 * 添加了代码的方法的static索引方法
 *
 */


class LogClassVisitor extends ClassVisitor implements Opcodes {

    private static final int LOG_TYPE_MANUAL = 1
    private static final int LOG_TYPE_DYNAMIC = 2
    private static final int LOG_TYPE_MANUAL_AND_DYNAMIC = 3

    String className
    String superName
    String[] interfaces
    AtomicInteger integer
    List<MethodInfo> list
    boolean openDylog

    //忽略这个类
    boolean ignoredClass
    private HashMap<String, Aops> aopsHashMap

    LogClassVisitor(ClassVisitor cv, HashMap<String, Aops> aopsHashMap, boolean openDylog) {
        super(Opcodes.ASM5, cv)
        integer = new AtomicInteger(0)
        this.aopsHashMap = aopsHashMap
        this.openDylog = openDylog
    }

    void visit(int version, int access, String name, String signature,
               String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces)
        this.className = name
        this.superName = superName
        this.interfaces = interfaces

        list = new ArrayList<>()
    }

/**
 * 剔除
 * access$,
 * <init>,
 * <clinit>,
 * invokeMethodByIndex
 *
 * @param name
 * @return
 */
    static boolean shouldProcessMethod(String className, String name) {
        if ("<init>".equals(name)
                || "<clinit>".equals(name)
                || name.startsWith('access$')
                || name.startsWith('invokeMethodByIndex'))
            return false

        return true
    }

/**
 *
 * @param access
 * @param name
 * @param desc
 * @param signature
 * @param exceptions
 * @return
 */
    @Override
    MethodVisitor visitMethod(int access, String name, String desc,
                              String signature, String[] exceptions) {

        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions)
        if (shouldProcessMethod(className, name) && !ignoredClass) {
            mv = new DynamicLogMethodVisitor(Opcodes.ASM5, mv, access, name, desc, signature, exceptions, className)
        }
        return mv
    }

/**
 *   增加静态类方法
 *   public static Object invokeMethodByIndex(int methodIndex,
 *   int n,int na,int nb,Object instanceOrClass, Object[] args,int logtype,Object params)
 *{return null;}*
 *
 *   */
    @Override
    void visitEnd() {

        if (integer.get() > 0 && !ignoredClass) {
            //增加static的实现
            MethodVisitor mv = cv.visitMethod(ACC_PUBLIC + ACC_STATIC, "invokeMethodByIndex", "(IIIILjava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;ILjava/lang/Object;)Ljava/lang/Object;", null, null)
            MethodVisitor addInvokeMethod = new AddInvokeMethodVisitor(Opcodes.ASM5, mv,
                    Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "invokeMethodByIndex", "(IIIILjava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;ILjava/lang/Object;)V", null, null, className, integer.get(), list)
            addInvokeMethod.visitCode()
            addInvokeMethod.visitMaxs(0, 0)
            addInvokeMethod.visitEnd()
        }
        super.visitEnd()
    }

    @Override
    AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc == 'Lcom/qiudaoyu/monitor/log/annotation/IgnoreLog;') {
            Logger.info("LogAop 发现 ${className}${desc} 有注解 @IgnoreLog")
            ignoredClass = true
            return null
        }
        return super.visitAnnotation(desc, visible)
    }

    class DynamicLogMethodVisitor extends LocalVariablesSorter {
        int access, methodIndex
        String name, desc, signature, className
        String[] exceptions
        int logType
        boolean isIgnored
        MAopAnnomationVisitor annomationVisitor

        DynamicLogMethodVisitor(
                final int api,
                final MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions, String className) {
            super(api, access, desc, mv)
            this.access = access
            this.name = name
            this.desc = desc
            this.signature = signature
            this.exceptions = exceptions
            this.className = className
        }

        @Override
        AnnotationVisitor visitAnnotation(String s, boolean b) {
            if (s == 'Lcom/qiudaoyu/monitor/log/annotation/IgnoreLog;') {
                Logger.info("LogAop 发现 ${name}${desc} 有注解 @IgnoreLog")
                isIgnored = true
                return null
            }

            if (s == 'Lcom/qiudaoyu/monitor/log/mlog/annotation/MAop;') {
                Logger.info("LogAop 发现 ${name}${desc} 有注解 @MAop")
                annomationVisitor = new MAopAnnomationVisitor(ASM5)
                return annomationVisitor
            }
            return super.visitAnnotation(s, b)
        }

        @Override
        void visitCode() {

            super.visitCode()

            //如果忽略log
            if (isIgnored) {
                return
            }
            logType = 0

            if (isAnnomationMAop(annomationVisitor) || isExtentionMAop(className, name)) {
                logType |= LOG_TYPE_MANUAL
            }

            if (openDylog) {
                logType |= LOG_TYPE_DYNAMIC
            }

            if (logType == 0) {
                return
            }

            //maop的参数
            ArrayList<String> params
            if ((logType & LOG_TYPE_MANUAL) > 0) {
                if (isAnnomationMAop(annomationVisitor)) {
                    params = annomationVisitor.params
                } else {
                    params = aopsHashMap.get((className + name)).params
                }
            }

            println('log aop method: start ' + name + desc)

            methodIndex = integer.incrementAndGet()
            list.add(new MethodInfo(name, desc, access, methodIndex, logType, params))
            /*
            *   if(Monitor.isEnable(n,nA,nB,logtype)!=0){
            *      有返回值：
            *      return invokeMethodByIndex(methodIndex,n,nA,nB,des,instanceOrClass,args,logtype,params);
            *      无返回值：
            *      invokeMethodByIndex(methodIndex,n,nA,nB,des,instanceOrClass,args,logtype,params);
            *      return;
            *   }
            *   ....
            *   ....
            */

            //计算className+name+desc的hash值
            int nHash = ConfigOneWayHashTable.HashString((className + name + desc), ConfigOneWayHashTable.HASH_OFFSET)
            int nHashA = ConfigOneWayHashTable.HashString((className + name + desc), ConfigOneWayHashTable.HASH_A)
            int nHashB = ConfigOneWayHashTable.HashString((className + name + desc), ConfigOneWayHashTable.HASH_B)

            //nHash
            mv.visitLdcInsn(nHash)
            //nHashA
            mv.visitLdcInsn(nHashA)
            //nHashB
            mv.visitLdcInsn(nHashB)

            //logType
            mv.visitLdcInsn(logType)

            //调用方法
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/qiudaoyu/monitor/Monitor", "isEnable", "(IIII)I", false)

            /*
            * 判断返回值是否为0
            * 如果不为空直接跳转到origiLabel
            * 不为0空根据是否有返回值点用Monitor.invoke(...)方法
            * */
            Label originStart = new Label()
            mv.visitJumpInsn(Opcodes.IFEQ, originStart)

            /*
          *
          * 调用invokeMethodByIndex
          * 参数：
          * nHash
          * nHashA
          * nHashB
          * methodIndex
          * 方法描述符
          * 静态方法第一个参数为class对象，实例方法第一个参数为实例对象
          * 原方法参数的类类型args[]
          *
          *
          *
          */

            //方法索引值压栈
            mv.visitLdcInsn(methodIndex)
            //nHash
            mv.visitLdcInsn(nHash)
            //nHashA
            mv.visitLdcInsn(nHashA)
            //nHashB
            mv.visitLdcInsn(nHashB)

            //方法描述符
            mv.visitLdcInsn(desc)

            //this or Class
            if (Modifier.isStatic(access)) {
                //如果是静态方法当前class的class入栈
                mv.visitLdcInsn(Type.getObjectType(className))
            } else {
                //实例方法this入栈
                mv.visitVarInsn(Opcodes.ALOAD, 0)
            }

            //args数组处理
            Type[] localVars = Type.getArgumentTypes(desc)
            int argsize = localVars.size()
            if (argsize == 0) {
                mv.visitInsn(Opcodes.ACONST_NULL)
            } else {
                mv.visitLdcInsn(argsize)
                mv.visitTypeInsn(ANEWARRAY, "java/lang/Object")
                int startIndex
                if (Modifier.isStatic(access)) {
                    startIndex = 0
                } else {
                    startIndex = 1
                }
                for (int i = 0; i < argsize; i++) {
                    //load局部变量到操作栈
                    mv.visitInsn(DUP)
                    mv.visitLdcInsn(i)
                    int opcode = getVarLoadOp(localVars[i])
                    mv.visitVarInsn(opcode, getLocalSortIndex((i + startIndex), access, localVars))
                    //基本类型需要要包裹类型转换成对象类型
                    if (opcode != Opcodes.ALOAD) {
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, getWrapType(localVars[i]).getInternalName(), "valueOf", Type.getMethodDescriptor(getWrapType(localVars[i]), localVars[i]), false)
                    }
                    mv.visitInsn(Opcodes.AASTORE)
                }
            }

            //logTpye
            mv.visitLdcInsn(logType)

            //mlog的params
            if ((logType & LOG_TYPE_MANUAL) == 0) {
                mv.visitInsn(Opcodes.ACONST_NULL)
            } else {
                if (params == null) {
                    mv.visitInsn(Opcodes.ACONST_NULL)
                } else {
                    int count = params.size()
                    //创建string数组
                    mv.visitLdcInsn(count)
                    mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String")
                    for (int i = 0; i < count; i++) {
                        mv.visitInsn(Opcodes.DUP)
                        mv.visitLdcInsn(i)
                        mv.visitLdcInsn(((String) params.get(i)))
                        mv.visitInsn(Opcodes.AASTORE)
                    }
                }
            }

            //invokeMethodByIndex方法,返回包装类型
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, "invokeMethodByIndex", "(IIIILjava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;ILjava/lang/Object;)Ljava/lang/Object;", false)

            //处理包装类型返回值
            Type returnType = Type.getReturnType(desc)

            if (returnType.getSort() == Type.VOID) {
                mv.visitInsn(Opcodes.POP)
            } else {

                //先强制转换为对应才类型
                mv.visitTypeInsn(Opcodes.CHECKCAST, getWrapType(returnType).getInternalName())

                //如果返回值是基本类型,获取对应的值
                if (returnType.getSort() >= Type.BOOLEAN && returnType.getSort() <= Type.DOUBLE) {
                    String owner = getWrapType(returnType).getInternalName()
                    String name = returnType.getClassName() + "Value"
                    String des = "()" + returnType.getDescriptor()
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, des, false)
                }

            }

            // 对应的返回指令
            mv.visitInsn(getReturnOp(returnType))

            //原来代码开始的地方
            mv.visitLabel(originStart)
            println('log aop method: end ' + name + desc)
        }

/**
 * 调整代码位置
 * @param line
 * @param start
 */
        @Override
        void visitLineNumber(int line, Label start) {
            super.visitLineNumber(line, start)
        }

        @Override
        void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(maxStack, maxLocals)
        }

        @Override
        void visitEnd() {
            //增加static的索引方法
            super.visitEnd()

        }
    }

    class AddInvokeMethodVisitor extends LocalVariablesSorter {
        int access, max
        String name, desc, signature, className
        String[] exceptions
        List<MethodInfo> list

        AddInvokeMethodVisitor(
                final int api,
                final MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions, String className, int max, List<MethodInfo> infos) {
            super(api, access, desc, mv)
            this.access = access
            this.name = name
            this.desc = desc
            this.signature = signature
            this.exceptions = exceptions
            this.className = className
            this.max = max
            this.list = infos
        }

/**
 *   增加静态类方法
 *   public static Object invokeMethodByIndex(int methodIndex,int n,int nA,int nB,String des,Object instanceOrClass, Object[] args,int logtype,Object params) {return null;}*
 *  */
        @Override
        void visitCode() {

/*            public static Object invokeMethodByIndex(int methodIndex, int n, int nA, int nB, String des, Object instanceOrClass, Object[] args,int logtype,Object params) {
*                Monitor.invoke();
*                Monitor.before(n, nA, nB, des, instanceOrClass, args,logtype,params);
*                try {
*                    switch (methodIndex) {
*                        case 0:
*                            int a = ((Test) instanceOrClass).test1();
*                            Monitorx.after(a, 1, n, nA, nB, des, instanceOrClass, args,logtype,params);
*                            return null;
*                        default:
*                            return null;
*
*                    }
*                } catch (Exception e) {
*                    Monitor.exception(e, n, nA, nB, des, instanceOrClass, args,logtype,params);
*                    Monitor.after(null, 0, n, nA, nB, des, instanceOrClass, args,logtype,params);
*                    throw e;
*                }
*            }
*
*/

            //调用before
            Label start = new Label()
            Label end = new Label()
            Label handle = new Label()
            Label allEnd = new Label()
            Label allStart = new Label()


            super.visitLabel(allStart)
            //调用Monitor.invoke方法
            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/qiudaoyu/monitor/Monitor", "invoke", "()V", false)

            super.visitLabel(start)
            super.visitTryCatchBlock(start, end, handle, "java/lang/Exception")

            //调用before方法
            //n
            super.visitVarInsn(Opcodes.ILOAD, 1)
            //n1
            super.visitVarInsn(Opcodes.ILOAD, 2)
            //n2
            super.visitVarInsn(Opcodes.ILOAD, 3)
            //des
            super.visitVarInsn(Opcodes.ALOAD, 4)
            //this or class
            super.visitVarInsn(Opcodes.ALOAD, 5)
            //args
            super.visitVarInsn(Opcodes.ALOAD, 6)
            //logtype
            super.visitVarInsn(Opcodes.ILOAD, 7)
            //params
            super.visitVarInsn(Opcodes.ALOAD, 8)

            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/qiudaoyu/monitor/Monitor", "before", "(IIILjava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;ILjava/lang/Object;)V", false)

            // swtich(methodIndex),tableswitch
            Label defaultLable = new Label()
            Label[] labels = new Label[max]
            for (int i = 0; i < labels.size(); i++) {
                labels[i] = new Label()
            }
            //methodIndex
            super.visitVarInsn(Opcodes.ILOAD, 0)
            super.visitTableSwitchInsn(1, max, defaultLable, labels)

            //默认直接return null
            super.visitLabel(defaultLable)
            super.visitInsn(Opcodes.ACONST_NULL)
            super.visitInsn(Opcodes.ARETURN)

            //至少需要一个slot
            int maxStacks = 1
            //类中方法的索引
            CollectionUtils.sort(list, new Comparator<MethodInfo>() {
                @Override
                int compare(MethodInfo o1, MethodInfo o2) {
                    return o1.getIndex() > o2.getIndex() ? 1 : -1
                }
            })

            for (MethodInfo info : list) {
                int curStack = 0
                super.visitLabel(labels[info.index - 1])
                //处理Object转换为变量的问题
                int invokeCode
                if (Modifier.isStatic(info.access)) {
                    invokeCode = Opcodes.INVOKESTATIC
                } else {
                    if (Modifier.isPrivate(info.access)) {
                        invokeCode = Opcodes.INVOKESPECIAL
                    } else {
                        invokeCode = Opcodes.INVOKEVIRTUAL
                    }
                    //this变量，转化为具体类型
                    super.visitVarInsn(Opcodes.ALOAD, 5)
                    super.visitTypeInsn(Opcodes.CHECKCAST, Type.getObjectType(className).getInternalName())
                    curStack = curStack + 1
                }

                //6位置局部变量是方法参数
                StackInfo stackInfo = changObjectArrayToArgs(info.des, 6)
                //处理完局部变量所需的栈大小
                maxStacks = Math.max(maxStacks, curStack + stackInfo.maxStacks)

                //调用方法
                super.visitMethodInsn(invokeCode, className, info.name, info.des, false)

                //处理返回值
                Type returnType = Type.getReturnType(info.des)
                if (returnType.getSort() == Type.VOID) {
                    super.visitInsn(Opcodes.ACONST_NULL)
                } else {
                    //基础类型要转换
                    if (returnType.getSort() >= Type.BOOLEAN && returnType.getSort() <= Type.DOUBLE) {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, getWrapType(returnType).getInternalName(), "valueOf", Type.getMethodDescriptor(getWrapType(returnType), returnType), false)
                    } else {
                        //其他类类型直接放在栈里
                    }
                    super.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Object")
                }

                //正常返回的finally操作，复制返回值，最后return
                super.visitInsn(Opcodes.DUP)

                //正常退出
                super.visitLdcInsn(1)
                //n
                super.visitVarInsn(Opcodes.ILOAD, 1)
                //n1
                super.visitVarInsn(Opcodes.ILOAD, 2)
                //n2
                super.visitVarInsn(Opcodes.ILOAD, 3)
                //des
                super.visitVarInsn(Opcodes.ALOAD, 4)
                //this or class
                super.visitVarInsn(Opcodes.ALOAD, 5)
                //args
                super.visitVarInsn(Opcodes.ALOAD, 6)
                //logtype
                super.visitVarInsn(Opcodes.ILOAD, 7)
                //params
                super.visitVarInsn(Opcodes.ALOAD, 8)
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/qiudaoyu/monitor/Monitor", "after", "(Ljava/lang/Object;IIIILjava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;ILjava/lang/Object;)V", false)
                //对应的返回指令
                super.visitInsn(Opcodes.ARETURN)
            }
            super.visitLabel(end)

            //exception处理代码
            super.visitLabel(handle)

            //异常处理
            super.visitInsn(Opcodes.DUP)

            //n
            super.visitVarInsn(Opcodes.ILOAD, 1)
            //n1
            super.visitVarInsn(Opcodes.ILOAD, 2)
            //n2
            super.visitVarInsn(Opcodes.ILOAD, 3)
            //des
            super.visitVarInsn(Opcodes.ALOAD, 4)
            //this or class
            super.visitVarInsn(Opcodes.ALOAD, 5)
            //args
            super.visitVarInsn(Opcodes.ALOAD, 6)
            //logtype
            super.visitVarInsn(Opcodes.ILOAD, 7)
            //params
            super.visitVarInsn(Opcodes.ALOAD, 8)
            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/qiudaoyu/monitor/Monitor", "exception", "(Ljava/lang/Exception;IIILjava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;ILjava/lang/Object;)V", false)

            //异常返回的finally操作
            //空返回值
            super.visitInsn(Opcodes.ACONST_NULL)
            //异常退出
            super.visitLdcInsn(0)
            //n
            super.visitVarInsn(Opcodes.ILOAD, 1)
            //n1
            super.visitVarInsn(Opcodes.ILOAD, 2)
            //n2
            super.visitVarInsn(Opcodes.ILOAD, 3)
            //des
            super.visitVarInsn(Opcodes.ALOAD, 4)
            //this or class
            super.visitVarInsn(Opcodes.ALOAD, 5)
            //args
            super.visitVarInsn(Opcodes.ALOAD, 6)
            //logtype
            super.visitVarInsn(Opcodes.ILOAD, 7)
            //params
            super.visitVarInsn(Opcodes.ALOAD, 8)

            //访问after函数
            super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/qiudaoyu/monitor/Monitor", "after", "(Ljava/lang/Object;IIIILjava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;ILjava/lang/Object;)V", false)

            super.visitLabel(allEnd)

            //抛出异常
            super.visitInsn(Opcodes.ATHROW)

            //创建局部变量表
            super.visitLocalVariable("methodIndex", "I", null, allStart, allEnd, 0)
            super.visitLocalVariable("n", "I", null, allStart, allEnd, 1)
            super.visitLocalVariable("nA", "I", null, allStart, allEnd, 2)
            super.visitLocalVariable("nB", "I", null, allStart, allEnd, 3)
            super.visitLocalVariable("des", "Ljava/lang/String;", null, allStart, allEnd, 4)
            super.visitLocalVariable("instanceOrClass", "Ljava/lang/Object;", null, allStart, allEnd, 5)
            super.visitLocalVariable("args", "[Ljava/lang/Object;", null, allStart, allEnd, 6)
            super.visitLocalVariable("logType", "I", null, allStart, allEnd, 7)
            super.visitLocalVariable("params", "Ljava/lang/Object;", null, allStart, allEnd, 8)

        }

/**
 *
 * @param des
 * @param varIndex
 */
        StackInfo changObjectArrayToArgs(String des, int varIndex) {
            Type[] argsTypes = Type.getArgumentTypes(des)
            int curstack, maxstack = 0
            for (int i = 0; i < argsTypes.length; i++) {
                Type type = argsTypes[i]
                //数组对应的数据入栈
                super.visitVarInsn(Opcodes.ALOAD, varIndex)
                super.visitLdcInsn(i)
                super.visitInsn(Opcodes.AALOAD)


                maxstack = Math.max((curstack + 2), maxstack)
                curstack = curstack + 1

                //Object强转换为具体类型
                super.visitTypeInsn(Opcodes.CHECKCAST, getWrapType(type).getInternalName())

                //基本类型取值
                if (type.getSort() >= Type.BOOLEAN && type.getSort() <= Type.DOUBLE) {
                    String owner = getWrapType(type).getInternalName()
                    String name = type.getClassName() + "Value"
                    String dess = "()" + type.getDescriptor()
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, name, dess, false)

                    if (type.getSort() >= Type.FLOAT && type.getSort() <= Type.DOUBLE) {
                        //需要两个slot
                        curstack = curstack + 1
                    }
                }

                maxstack = Math.max(curstack, maxstack)
            }

            return new StackInfo(curstack, maxstack)

        }

    }

/**
 * 基础类型转换为包裹类型
 * 其他类型不变
 * @param primitiveType
 * @return
 */
    Type getWrapType(Type primitiveType) {
        Type type
        switch (primitiveType.getSort()) {
            case Type.BOOLEAN:
                type = Type.getType(Boolean.class)
                break
            case Type.CHAR:
                type = Type.getType(Character.class)
                break
            case Type.BYTE:
                type = Type.getType(Byte.class)
                break
            case Type.SHORT:
                type = Type.getType(Short.class)
                break
            case Type.INT:
                type = Type.getType(Integer.class)
                break
            case Type.FLOAT:
                type = Type.getType(Float.class)
                break
            case Type.LONG:
                type = Type.getType(Long.class)
                break
            case Type.DOUBLE:
                type = Type.getType(Double.class)
                break
            default:
                type = primitiveType
                break

        }
        return type

    }

    int getSlot(Type primitiveType) {
        switch (primitiveType.getSort()) {
            case Type.LONG:
            case Type.DOUBLE:
                return 2
            default:
                return 1

        }
    }

    int getVarLoadOp(Type type) {
        int opcode = Opcodes.ILOAD
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                opcode = Opcodes.ILOAD
                break
            case Type.FLOAT:
                opcode = Opcodes.FLOAD
                break
            case Type.LONG:
                opcode = Opcodes.LLOAD
                break
            case Type.DOUBLE:
                opcode = Opcodes.DLOAD
                break
            case Type.ARRAY:
            case Type.OBJECT:
                opcode = Opcodes.ALOAD
        }
        return opcode
    }

    int getReturnOp(Type type) {
        int opcode = Opcodes.IRETURN
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                opcode = Opcodes.IRETURN
                break
            case Type.FLOAT:
                opcode = Opcodes.FRETURN
                break
            case Type.LONG:
                opcode = Opcodes.LRETURN
                break
            case Type.DOUBLE:
                opcode = Opcodes.DRETURN
                break
            case Type.VOID:
                opcode = Opcodes.RETURN
                break
            case Type.ARRAY:
            case Type.OBJECT:
                opcode = Opcodes.ARETURN
                break
        }
        return opcode
    }

    class MethodInfo {
        String name
        String des
        int access
        int index
        int logtype
        Object params

        MethodInfo(String name, String des, int access, int index, int logtype, Object params) {
            this.name = name
            this.des = des
            this.access = access
            this.index = index
            this.logtype = logtype
            this.params = params
        }

    }

    class StackInfo {
        int maxStacks
        int curStack

        StackInfo(int maxStacks, int curStack) {
            this.maxStacks = maxStacks
            this.curStack = curStack
        }
    }

    boolean isAnnomationMAop(MAopAnnomationVisitor annomationVisitor) {
        if (annomationVisitor != null) {
            return true
        }

        return false
    }

    boolean isExtentionMAop(String clazz, String name) {
        if (aopsHashMap != null && aopsHashMap.containsKey((clazz + name))) {
            if (aopsHashMap.get((clazz + name)) != null) {
                return true
            }
        }
        return false
    }


    int getLocalSortIndex(int localIndex, int access, Type[] localVars) {
        if (localIndex == 0) {
            return 0
        }
        if (localVars == null) {
            return 0
        }
        List<Type> types = new ArrayList<>()

        //实例方法有有实例参数
        if (!Modifier.isStatic(access)) {
            types.add(Type.getType(Object.class))
        }

        types.addAll(localVars)

        int slotIndex = 0
        for (int i = 0; i < localIndex; i++) {
            slotIndex += getSlot(types.get(i))
        }
        return slotIndex
    }
}
