package com.cohen.getter;

import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * 1.需要单独搞个项目，在META-INF/services目录下创建文件（UTF-8）名为javax.annotation.processing.Processor，文件内容为com.cohen.spring.debug.getter.GetterProcessor，打成jar包
 * --- 这个项目在编译生成jar包的时候，因为maven需要拿到META-INF/services的配置增加调用javac的处理器参数，但是GetterProcessor又没有编译，所以有冲突；需要在编译的时候屏蔽META-INF/services并且在生成jar包的时候必须包括这个文件
 * 2.项目引用上述jar包，META-INF/services目录对当前项目也生效，在编译当前项目的时候会触发GetterProcessor这个注解处理器
 *
 * @author linjincheng
 * @version v1.0.0
 * @date 2019/5/14 16:01
 */
@SupportedAnnotationTypes("com.cohen.getter.Getter")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class GetterProcessor extends AbstractProcessor {

    private Trees trees;
    private TreeMaker treeMaker;
    private Names names;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        trees = Trees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        treeMaker = TreeMaker.instance(context);
        names = Names.instance(context);
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            messager.printMessage(Diagnostic.Kind.WARNING, "GetterProcessor start");
            Set<? extends Element> getters = roundEnv.getElementsAnnotatedWith(Getter.class);
            for (Element element : getters) {
                JCTree tree = ((JCTree) trees.getTree(element));
                tree.accept(new TreeTranslator() {
                    @Override
                    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                        // 遍历类定义的定义集合（成员变量定义、成员方法定义、构造器定义...）
                        ListBuffer<JCTree.JCMethodDecl> getMethods = new ListBuffer<>();
                        for (JCTree def : jcClassDecl.defs) {
                            // 判断定义是否为成员变量定义
                            if (def.getKind().equals(Tree.Kind.VARIABLE)) {
                                JCTree.JCVariableDecl var = (JCTree.JCVariableDecl) def;
                                // 根据成员变量定义，生成getter方法定义
                                ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
                                JCTree.JCReturn aReturn = treeMaker.Return(treeMaker.Select(treeMaker.Ident(names.fromString("this")), var.getName()));
                                statements.append(aReturn);
                                JCTree.JCMethodDecl methodDecl = treeMaker.MethodDef(
                                        // 限定符
                                        treeMaker.Modifiers(Flags.PUBLIC),
                                        // 方法名
                                        names.fromString("get" + var.getName().toString().substring(0, 1).toUpperCase() + var.getName().toString().substring(1)),
                                        // 返回值类型
                                        var.vartype,
                                        // 方法入参类型
                                        List.nil(),
                                        // 未知
                                        null,
                                        // 参数定义（描述）
                                        List.nil(),
                                        // 抛出异常集合
                                        List.nil(),
                                        // 方法体
                                        treeMaker.Block(0, statements.toList()),
                                        // 默认值（默认的返回值？？）
                                        null
                                );
                                // 生成的get方法描述加入到集合中
                                getMethods.append(methodDecl);
                            }
                        }
                        // 将生成的get方法注册到类定义中
                        for (JCTree.JCMethodDecl method : getMethods.toList()) {
                            messager.printMessage(Diagnostic.Kind.WARNING, method.toString());
                            jcClassDecl.defs = jcClassDecl.defs.prepend(method);
                        }
                        super.visitClassDef(jcClassDecl);
                    }
                });
            }
            messager.printMessage(Diagnostic.Kind.WARNING, "GetterProcessor end");
        }
        return true;
    }
}
