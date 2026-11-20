package com.costlens;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * DDD 分层与精度约束的架构守卫。违反即构建失败。
 */
@AnalyzeClasses(packages = "com.costlens", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** 领域层零技术依赖：不能出现 Spring / MyBatis / Jackson / 上层模块 */
    @ArchTest
    static final ArchRule domain_is_technology_free = noClasses()
            .that().resideInAPackage("com.costlens.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..",
                    "org.apache.ibatis..",
                    "com.baomidou..",
                    "com.fasterxml.jackson..",
                    "jakarta..",
                    "com.costlens.infrastructure..",
                    "com.costlens.application..",
                    "com.costlens.adaptor..");

    /** 计价链路禁止浮点装箱类型 */
    @ArchTest
    static final ArchRule domain_forbids_boxed_floating_point = noClasses()
            .that().resideInAPackage("com.costlens.domain..")
            .should().dependOnClassesThat()
            .haveFullyQualifiedName("java.lang.Double")
            .orShould().dependOnClassesThat()
            .haveFullyQualifiedName("java.lang.Float");

    /** 适配器层不得直接依赖基础设施实现，仓储接口由 domain 暴露 */
    @ArchTest
    static final ArchRule adaptor_must_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage("com.costlens.adaptor..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.costlens.infrastructure..");

    /** 应用层不得依赖基础设施与适配器 */
    @ArchTest
    static final ArchRule application_must_not_depend_on_outer_layers = noClasses()
            .that().resideInAPackage("com.costlens.application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.costlens.infrastructure..", "com.costlens.adaptor..");

    /** 基础设施只能依赖应用层的端口定义，不得依赖场景编排与装配器 */
    @ArchTest
    static final ArchRule infrastructure_only_depends_on_ports = noClasses()
            .that().resideInAPackage("com.costlens.infrastructure..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "com.costlens.application..scenario..",
                    "com.costlens.application..assembler..",
                    "com.costlens.adaptor..");
}