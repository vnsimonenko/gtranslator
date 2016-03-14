package com.gtranslator.cache;

import com.gtranslator.BaseException;
import org.objectweb.asm.*;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

//import java.io.ByteArrayInputStream;
//import java.io.FileOutputStream;
//import org.apache.commons.io.FileUtils;
//import org.apache.commons.io.FilenameUtils;
//import org.apache.commons.lang3.StringUtils;

public class CacheManager {
    private static ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
    private static KeyGenerator keyGenerator = new SimpleKeyGenerator();

    public static Class transform(String className) throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
        final org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(ClassWriter.COMPUTE_FRAMES);
        org.objectweb.asm.ClassReader cr = new org.objectweb.asm.ClassReader(className);
        cr.accept(new org.objectweb.asm.ClassVisitor(Opcodes.ASM4, cw) {

            @Override
            public MethodVisitor visitMethod(final int access,
                                             final String name, final String desc,
                                             final String signature, final String[] exceptions) {
                final Type[] args = Type.getArgumentTypes(desc);
                final Type retType = Type.getReturnType(desc);
                MethodVisitor v = cv.visitMethod(access, name, desc, signature,
                        exceptions);
                return new MethodVisitor(Opcodes.ASM5, v) {

                    private final Map<String, Object> params = new HashMap<>();

                    @Override
                    public AnnotationVisitor visitAnnotation(String s, boolean b) {
                        AnnotationVisitor av = mv.visitAnnotation(s, b);
                        if (s.equals("L" + PACKAGE + "/Caching;")) {
                            AnnotationVisitor visitor = new AnnotationVisitor(Opcodes.ASM5, av) {
                                @Override
                                public void visit(String s, Object o) {
                                    //System.out.println(s + "," + o);
                                    params.put(s, o);
                                    super.visit(s, o);
                                }

                                @Override
                                public void visitEnum(String s, String s1, String s2) {
                                    params.put(s, s2);
                                    super.visitEnum(s, s1, s2);
                                }
                            };
                            return visitor;
                        } else {
                            return av;
                        }
                    }

                    @Override
                    public void visitCode() {
                        if (params.size() > 0 && Type.VOID_TYPE != retType) {
                            doBegin(mv, params, args, retType);
                        }
                    }

                    public void visitInsn(int opcode) {
                        if ((opcode == Opcodes.ARETURN || opcode == Opcodes.RETURN) && params.size() > 0) {
                            if (opcode == Opcodes.ARETURN) {
                                mv.visitInsn(Opcodes.DUP);
                            } else {
                                mv.visitInsn(Opcodes.ACONST_NULL);
                                mv.visitInsn(Opcodes.ACONST_NULL);
                            }
                            doExit(mv, params, args);
                            if (opcode == Opcodes.RETURN) {
                                mv.visitInsn(Opcodes.POP);
                            }
                        }
                        mv.visitInsn(opcode);
                    }
                };
            }
        }, 0);

        return new ClassLoader(CacheManager.class.getClassLoader()) {
            @Override
            public Class<?> loadClass(final String name)
                    throws ClassNotFoundException {
                System.out.println(name);
                if (name.equals(className)) {
                    byte[] b = cw.toByteArray();
                    //debug mode
                    /*
                    ByteArrayInputStream in = new ByteArrayInputStream(b);
                    try {
                        String storeClassPath = FilenameUtils.normalizeNoEndSeparator("/tmp/" + className + ".class");
                        IOUtils.copy(in, new FileOutputStream(storeClassPath));
                        ASMifier.main(new String[] {storeClassPath});
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    */
                    return defineClass(name, b, 0, b.length);
                }
                return super.loadClass(name);
            }
        }.loadClass(className);
    }

    private static final String PACKAGE = CacheManager.class.getPackage().getName().replaceAll("[.]", "/");

    public static void doBegin(MethodVisitor mv, Map<String, Object> params, Type[] args, Type retType) {
        mv.visitIntInsn(Opcodes.BIPUSH, args.length);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        for (int i = 0; i < args.length; i++) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitIntInsn(Opcodes.BIPUSH, i);
            mv.visitVarInsn(Opcodes.ALOAD, i + 1);
            mv.visitInsn(Opcodes.AASTORE);
        }
        mv.visitVarInsn(Opcodes.ASTORE, 1 + args.length);
        mv.visitLdcInsn(params.get("name").toString());
        mv.visitLdcInsn(params.get("key").toString());
        mv.visitFieldInsn(Opcodes.GETSTATIC, PACKAGE + "/Caching$TYPE",
                params.get("operType").toString(),
                "L" + PACKAGE + "/Caching$TYPE;");
        mv.visitVarInsn(Opcodes.ALOAD, 1 + args.length);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, PACKAGE + "/CacheManager",
                "getValueFromCache",
                "(Ljava/lang/String;Ljava/lang/String;L" + PACKAGE + "/Caching$TYPE;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 2 + args.length);
        mv.visitVarInsn(Opcodes.ALOAD, 2 + args.length);
        Label l0 = new Label();
        mv.visitJumpInsn(Opcodes.IFNULL, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 2 + args.length);
        mv.visitTypeInsn(Opcodes.CHECKCAST, retType.getInternalName());
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(l0);
        mv.visitFrame(Opcodes.F_APPEND, 2, new Object[]{"[Ljava/lang/Object;", "java/lang/Object"}, 0, null);
    }

    static void doExit(MethodVisitor mv, Map<String, Object> params, Type[] args) {
        mv.visitIntInsn(Opcodes.BIPUSH, args.length);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        for (int i = 0; i < args.length; i++) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitIntInsn(Opcodes.BIPUSH, i);
            mv.visitVarInsn(Opcodes.ALOAD, i + 1);
            mv.visitInsn(Opcodes.AASTORE);
        }
        mv.visitVarInsn(Opcodes.ASTORE, 1 + args.length);
        mv.visitLdcInsn(params.get("name").toString());
        mv.visitLdcInsn(params.get("key").toString());
        mv.visitFieldInsn(Opcodes.GETSTATIC, PACKAGE + "/Caching$TYPE",
                params.get("operType").toString(),
                "L" + PACKAGE + "/Caching$TYPE;");
        mv.visitVarInsn(Opcodes.ALOAD, 1 + args.length);
        //mv.visitIntInsn(Opcodes.SIPUSH, opcode);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC,
                PACKAGE + "/CacheManager", "finishCache",
                "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;L" + PACKAGE + "/Caching$TYPE;[Ljava/lang/Object;)V", false);
    }

    public static Object getValueFromCache(String cacheName, String key, Caching.TYPE operType, Object[] args) {
        return Caching.TYPE.EVICT == operType ? null : resolveCaching(cacheName, key, operType, args, null);
    }

    public static void finishCache(Object result, String cacheName, String key, Caching.TYPE operType, Object[] args) {
        resolveCaching(cacheName, key, operType, args, result);
    }

    private static Object resolveCaching(String cacheName, String cachekey, Caching.TYPE operType, Object[] args, Object result) {
        try {
            if (cacheManager.getCache(cacheName) == null) {
                cacheManager.setCacheNames(Arrays.asList(cacheName));
                cacheManager.setAllowNullValues(true);
            }
            ExpressionParser parser = new SpelExpressionParser();
            Expression exp = parser.parseExpression(cachekey);
            StandardEvaluationContext context = new StandardEvaluationContext();
            int i = 0;
            for (Object value : args) {
                context.setVariable("p" + i++, value);
            }
            Object key = keyGenerator.generate(null, null, exp.getValue(context));
            //System.out.println(exp.getValue(context));
            if (operType == Caching.TYPE.EVICT) {
                cacheManager.getCache(cacheName).evict(key);
                return null;
            } else {
                synchronized (cacheManager) {
                    Cache.ValueWrapper valueWrapper = cacheManager.getCache(cacheName).get(key);
                    if (valueWrapper != null && valueWrapper.get() != null) {
                        return valueWrapper.get();
                    }
                    cacheManager.getCache(cacheName).put(key, result);
                    return result;
                }
            }
        } catch (Throwable throwable) {
            throw new BaseException(throwable);
        }
    }
}
