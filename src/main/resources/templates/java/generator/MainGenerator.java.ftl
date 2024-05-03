package ${basePackage}.generator;

import com.yupi.model.DataModel;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;

<#--整体来说，这个宏根据传入的文件信息以及生成类型，选择性地执行静态文件复制或动态文件生成操作。  -->

<#--  这一行定义了一个名为 generateFile 的宏，它接受两个参数 indent 和 fileInfo。
      indent 是一个用于控制缩进的字符串，而 fileInfo 是一个包含文件信息的对象或结构体。 -->
<#macro generateFile indent fileInfo>
<#-- 这一行使用了 indent 变量来控制代码块的缩进，然后创建了一个名为 inputPath 的变量，
    通过将 inputRootPath 和 fileInfo.inputPath 组合而成的路径创建了一个 File 对象，并获取其绝对路径。-->
${indent}inputPath = new File(inputRootPath, "${fileInfo.inputPath}").getAbsolutePath();

${indent}outputPath = new File(outputRootPath, "${fileInfo.outputPath}").getAbsolutePath();
<#if fileInfo.generateType == "static">
${indent}StaticGenerator.copyFilesByHutool(inputPath, outputPath);
<#else>
${indent}DynamicGenerator.doGenerate(inputPath, outputPath, model);
</#if>
</#macro>

/**
 * 核心生成器
 */
public class MainGenerator {

    /**
     * 生成
     *
     * @param model 数据模型
     * @throws TemplateException
     * @throws IOException
     */
    public static void doGenerate(DataModel model) throws TemplateException, IOException {
        String inputRootPath = "${fileConfig.inputRootPath}";
        String outputRootPath = "${fileConfig.outputRootPath}";

        String inputPath;
        String outputPath;

    <#-- 获取模型变量 -->
    <#list modelConfig.models as modelInfo>
        <#-- 有分组 -->
        <#if modelInfo.groupKey??>
        <#list modelInfo.models as subModelInfo>
        ${subModelInfo.type} ${subModelInfo.fieldName} = model.${modelInfo.groupKey}.${subModelInfo.fieldName};
        </#list>
        <#else>
        ${modelInfo.type} ${modelInfo.fieldName} = model.${modelInfo.fieldName};
        </#if>
    </#list>

    <#list fileConfig.files as fileInfo>
        <#if fileInfo.groupKey??>
        // groupKey = ${fileInfo.groupKey}
        <#if fileInfo.condition??>
        if (${fileInfo.condition}) {
            <#list fileInfo.files as fileInfo>
            <@generateFile fileInfo=fileInfo indent="            " />
            </#list>
        }
        <#else>
        <#list fileInfo.files as fileInfo>
        <@generateFile fileInfo=fileInfo indent="        " />
        </#list>
        </#if>
        <#else>
        <#if fileInfo.condition??>
        if(${fileInfo.condition}) {
            <@generateFile fileInfo=fileInfo indent="            " />
        }
        <#else>
        <@generateFile fileInfo=fileInfo indent="        " />
        </#if>
        </#if>
    </#list>
    }
}