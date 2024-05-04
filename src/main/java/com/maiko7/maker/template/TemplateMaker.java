package com.maiko7.maker.template;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.maiko7.maker.meta.Meta;
import com.maiko7.maker.meta.enums.FileGenerateTypeEnum;
import com.maiko7.maker.meta.enums.FileTypeEnum;
import com.maiko7.maker.template.model.TemplateMakerConfig;
import com.maiko7.maker.template.model.TemplateMakerFileConfig;
import com.maiko7.maker.template.model.TemplateMakerModelConfig;
import com.maiko7.maker.template.model.TemplateMakerOutputConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TemplateMaker {

    /**
     * 制作模板
     * 获取用户输入的最原始的json文件（或者原始输入）来生成模板。
     *
     * @param templateMakerConfig
     * @return
     */
    public static long makeTemplate(TemplateMakerConfig templateMakerConfig) {
        Meta meta = templateMakerConfig.getMeta();
        String originProjectPath = templateMakerConfig.getOriginProjectPath();
        TemplateMakerFileConfig templateMakerFileConfig = templateMakerConfig.getFileConfig();
        TemplateMakerModelConfig templateMakerModelConfig = templateMakerConfig.getModelConfig();
        TemplateMakerOutputConfig templateMakerOutputConfig = templateMakerConfig.getOutputConfig();
        Long id = templateMakerConfig.getId();

        return makeTemplate(meta, originProjectPath, templateMakerFileConfig, templateMakerModelConfig, templateMakerOutputConfig, id);
    }

    /**
     * 制作模板
     *
     * @param newMeta 你看看他这里做了一个封装。 √√√√√√√
     *                不然你传的就是  String name = "acm-template-pro-generator";
     *                String description = "ACM 示例模板生成器";
     * @param originProjectPath 原始项目路径
     * @param templateMakerFileConfig   同时传入过滤器和文件分组配置
     * @param templateMakerModelConfig  模型参数信息。就好比sum ,outputText这种，替换原模板的sum为outputText
     * @param templateMakerOutputConfig
     * @param id
     * @return
     */
    public static long makeTemplate(Meta newMeta, String originProjectPath, TemplateMakerFileConfig templateMakerFileConfig, TemplateMakerModelConfig templateMakerModelConfig, TemplateMakerOutputConfig templateMakerOutputConfig, Long id) {
        // 没有 id 则生成
        if (id == null) {
            id = IdUtil.getSnowflakeNextId();
        }

        // 复制目录
        String projectPath = System.getProperty("user.dir");
        // maiko7-generator-maker/.temp/1
        String tempDirPath = projectPath + File.separator + ".temp";
        String templatePath = tempDirPath + File.separator + id;

        // 是否为首次制作模板
        // 目录不存在，则是首次制作
        if (!FileUtil.exist(templatePath)) {
            FileUtil.mkdir(templatePath);
            FileUtil.copy(originProjectPath, templatePath, true);
        }

        // 一、输入信息
        /**
         * 输入文件信息，获取到项目根目录。
         * 举个例子.temp/1/springboot-init和meta.json。它获取 就是springboot-init。
         * 前面的代码就是复制originProjectPath原始项目路径，然后复制到.temp/1目录下。
         *
         */
        String sourceRootPath = FileUtil.loopFiles(new File(templatePath), 1, null)
                .stream()
                .filter(File::isDirectory)
                .findFirst()
                .orElseThrow(RuntimeException::new)
                .getAbsolutePath();
        // 注意 win 系统需要对路径进行转义
        sourceRootPath = sourceRootPath.replaceAll("\\\\", "/");

        // 二、制作文件模板
        List<Meta.FileConfig.FileInfo> newFileInfoList = makeFileTemplates(templateMakerFileConfig, templateMakerModelConfig, sourceRootPath);

        // 处理模型信息
        List<Meta.ModelConfig.ModelInfo> newModelInfoList = getModelInfoList(templateMakerModelConfig);

        // 三、生成配置文件
        String metaOutputPath = templatePath + File.separator + "meta.json";

        /**
         *
         * 这行代码是用于从一个 JSON 文件中读取数据并将其转换为 Java 对象。让我们逐步解释：
         *
         * FileUtil.readUtf8String(metaOutputPath)：这部分首先使用 FileUtil 类的 readUtf8String 方法读取
         * 指定路径下的文件内容，并将其以 UTF-8 编码的字符串形式返回。假设 metaOutputPath 是一个指向 JSON 文件的路径。
         *
         * JSONUtil.toBean(...)：然后，JSONUtil 类的 toBean 方法被调用。这个方法通常是一个 JSON 库（
         * 比如 Jackson、Gson 或者 FastJSON）提供的，用于将 JSON 字符串转换为 Java 对象。
         * (metaOutputPath, Meta.class)：toBean 方法通常接受两个参数：第一个是 JSON 字符串，
         * 第二个是要转换成的 Java 对象的类型。Meta.class 表示要转换成的对象类型是 Meta 类型。
         */
        // 如果已有 meta 文件，说明不是第一次制作，则在 meta 基础上进行修改
        if (FileUtil.exist(metaOutputPath)) {
            Meta oldMeta = JSONUtil.toBean(FileUtil.readUtf8String(metaOutputPath), Meta.class);

            /**
             * 这行代码使用了一个名为 BeanUtil 的工具类的 copyProperties 方法，将一个对象的属性值复制到另一个对象中。下面是对这行代码的解释：
             *
             * BeanUtil.copyProperties(...): 这是一个用于对象属性拷贝的方法调用。通常，这种方法会将源对象的属性值复制到目标对象中。
             * (newMeta, oldMeta, CopyOptions.create().ignoreNullValue()): 这部分指定了要进行属性拷贝的对象以及一些配置选项。具体来说：
             * newMeta: 源对象，它的属性值将被复制到目标对象中。
             * oldMeta: 目标对象，它将接收源对象的属性值。
             * CopyOptions.create().ignoreNullValue(): 这里创建了一个 CopyOptions 对象，并调用了 ignoreNullValue() 方法。这意味着在属性复制过程中，将忽略源对象中值为 null 的属性，不会覆盖目标对象中对应属性的值。这通常是为了避免意外地将 null 值覆盖目标对象中已有的非 null 值。
             */
            BeanUtil.copyProperties(newMeta, oldMeta, CopyOptions.create().ignoreNullValue());
            newMeta = oldMeta;

            // 1. 追加配置参数
            List<Meta.FileConfig.FileInfo> fileInfoList = newMeta.getFileConfig().getFiles();
            fileInfoList.addAll(newFileInfoList);
            List<Meta.ModelConfig.ModelInfo> modelInfoList = newMeta.getModelConfig().getModels();
            modelInfoList.addAll(newModelInfoList);

            // 配置去重
            newMeta.getFileConfig().setFiles(distinctFiles(fileInfoList));
            newMeta.getModelConfig().setModels(distinctModels(modelInfoList));
        } else {
            // 1. 构造配置参数
            Meta.FileConfig fileConfig = new Meta.FileConfig();
            newMeta.setFileConfig(fileConfig);
            fileConfig.setSourceRootPath(sourceRootPath);
            List<Meta.FileConfig.FileInfo> fileInfoList = new ArrayList<>();
            fileConfig.setFiles(fileInfoList);
            fileInfoList.addAll(newFileInfoList);

            Meta.ModelConfig modelConfig = new Meta.ModelConfig();
            newMeta.setModelConfig(modelConfig);
            List<Meta.ModelConfig.ModelInfo> modelInfoList = new ArrayList<>();
            modelConfig.setModels(modelInfoList);
            modelInfoList.addAll(newModelInfoList);
        }

        // 2. 额外的输出配置
        if (templateMakerOutputConfig != null) {
            // 文件外层和分组去重
            if (templateMakerOutputConfig.isRemoveGroupFilesFromRoot()) {
                List<Meta.FileConfig.FileInfo> fileInfoList = newMeta.getFileConfig().getFiles();
                newMeta.getFileConfig().setFiles(com.maiko7.maker.template.TemplateMakerUtils.removeGroupFilesFromRoot(fileInfoList));
            }
        }

        // 3. 输出元信息文件
        FileUtil.writeUtf8String(JSONUtil.toJsonPrettyStr(newMeta), metaOutputPath);
        return id;
    }

    /**
     * 获取模型信息列表
     *
     * @param templateMakerModelConfig
     * @return
     */
    private static List<Meta.ModelConfig.ModelInfo> getModelInfoList(TemplateMakerModelConfig templateMakerModelConfig) {
        // 本次新增的模型配置列表
        List<Meta.ModelConfig.ModelInfo> newModelInfoList = new ArrayList<>();
        if (templateMakerModelConfig == null) {
            return newModelInfoList;
        }

        List<TemplateMakerModelConfig.ModelInfoConfig> models = templateMakerModelConfig.getModels();
        if (CollUtil.isEmpty(models)) {
            return newModelInfoList;
        }

        // 处理模型信息
        // - 转换为配置接受的 ModelInfo 对象
        List<Meta.ModelConfig.ModelInfo> inputModelInfoList = models.stream().map(modelInfoConfig -> {
            Meta.ModelConfig.ModelInfo modelInfo = new Meta.ModelConfig.ModelInfo();
            BeanUtil.copyProperties(modelInfoConfig, modelInfo);
            return modelInfo;
        }).collect(Collectors.toList());

        // - 如果是模型组
        TemplateMakerModelConfig.ModelGroupConfig modelGroupConfig = templateMakerModelConfig.getModelGroupConfig();
        if (modelGroupConfig != null) {
            // 复制变量
            Meta.ModelConfig.ModelInfo groupModelInfo = new Meta.ModelConfig.ModelInfo();
            BeanUtil.copyProperties(modelGroupConfig, groupModelInfo);

            // 模型全放到一个分组内
            groupModelInfo.setModels(inputModelInfoList);
            newModelInfoList.add(groupModelInfo);
        } else {
            // 不分组，添加所有的模型信息到列表
            newModelInfoList.addAll(inputModelInfoList);
        }
        return newModelInfoList;
    }

    /**
     * 制作文件配置
     * @param templateMakerFileConfig
     * @param templateMakerModelConfig
     * @param sourceRootPath C:\Users\73450\Desktop\maiko7-generator-master\maiko7-generator-maker\.temp\1\springboot-init
     * @return
     */
    private static List<Meta.FileConfig.FileInfo> makeFileTemplates(TemplateMakerFileConfig templateMakerFileConfig, TemplateMakerModelConfig templateMakerModelConfig, String sourceRootPath) {
        List<Meta.FileConfig.FileInfo> newFileInfoList = new ArrayList<>();
        if (templateMakerFileConfig == null) {
            return newFileInfoList;
        }
        // 文件过滤
        List<TemplateMakerFileConfig.FileInfoConfig> fileConfigInfoList = templateMakerFileConfig.getFiles();
        if (CollUtil.isEmpty(fileConfigInfoList)) {
            return newFileInfoList;
        }

        // 二、生成文件模板
        // 遍历输入文件
        for (TemplateMakerFileConfig.FileInfoConfig fileInfoConfig : fileConfigInfoList) {
            // src/main/resources/application.yml
            String inputFilePath = fileInfoConfig.getPath();

            // 如果填的是相对路径，要改为绝对路径
            if (!inputFilePath.startsWith(sourceRootPath)) {
                //C:\Users\73450\Desktop\maiko7-generator-master\maiko7-generator-maker\.temp\1\springboot-init\src/main/resources/application.yml
                inputFilePath = sourceRootPath + File.separator + inputFilePath;
            }

            // 获取过滤后的文件列表（不会存在目录）。就是获取符合过滤后的文件
            List<File> fileList = com.maiko7.maker.template.FileFilter.doFilter(inputFilePath, fileInfoConfig.getFilterConfigList());
            // 不处理已经生成的FTL模板文件。防止重复生成
            fileList = fileList.stream()
                    .filter(file -> !file.getAbsolutePath().endsWith(".ftl"))
                    .collect(Collectors.toList());


            for (File file : fileList) {
                Meta.FileConfig.FileInfo fileInfo = makeFileTemplate(templateMakerModelConfig, sourceRootPath, file, fileInfoConfig);
                newFileInfoList.add(fileInfo);
            }
        }

        // 如果是文件组
        TemplateMakerFileConfig.FileGroupConfig fileGroupConfig = templateMakerFileConfig.getFileGroupConfig();
        if (fileGroupConfig != null) {
            String condition = fileGroupConfig.getCondition();
            String groupKey = fileGroupConfig.getGroupKey();
            String groupName = fileGroupConfig.getGroupName();

            // 新增分组配置
            Meta.FileConfig.FileInfo groupFileInfo = new Meta.FileConfig.FileInfo();
            groupFileInfo.setType(FileTypeEnum.GROUP.getValue());
            groupFileInfo.setCondition(condition);
            groupFileInfo.setGroupKey(groupKey);
            groupFileInfo.setGroupName(groupName);
            // 文件全放到一个分组内
            groupFileInfo.setFiles(newFileInfoList);
            newFileInfoList = new ArrayList<>();
            newFileInfoList.add(groupFileInfo);
        }
        return newFileInfoList;
    }

    /**
     * 制作文件模板
     *
     * @param templateMakerModelConfig
     * @param sourceRootPath
     * @param inputFile
     * @param fileInfoConfig
     * @return
     */
    private static Meta.FileConfig.FileInfo makeFileTemplate(TemplateMakerModelConfig templateMakerModelConfig, String sourceRootPath, File inputFile, TemplateMakerFileConfig.FileInfoConfig fileInfoConfig) {
        // 要挖坑的文件绝对路径（用于制作模板）
        // 注意 win 系统需要对路径进行转义
        String fileInputAbsolutePath = inputFile.getAbsolutePath().replaceAll("\\\\", "/");
        String fileOutputAbsolutePath = fileInputAbsolutePath + ".ftl";

        /**
         * 他这里，首先，他fileInputPath要的是相对路径对吧。那我inputFile.getAbsolutePath()先获取
         * 这个目录的绝对路径，然后他把sourceRootPath + "/"替换成空的，就变成了相对路径
         * 比如"D:/maiko7/maiko7-generator-master/maiko7-generator-demo-projects/acm-template-pro","src/com/maiko7/acm/MainTemplate.java.ftl",
         * 着两个本来后面是相对路径，那你怎么得到它呢？是不是就是把sourceRootPath + "/"替换成空的
         */
        // 文件输入输出相对路径（用于生成配置）
        String fileInputPath = fileInputAbsolutePath.replace(sourceRootPath + "/", "");
        String fileOutputPath = fileInputPath + ".ftl";

        // 使用字符串替换，生成模板文件
        String fileContent;
        // 如果已有模板文件，说明不是第一次制作，则在模板基础上再次挖坑
        boolean hasTemplateFile = FileUtil.exist(fileOutputAbsolutePath);
        if (hasTemplateFile) {
            // 如果已有模板文件，表示不是第一次制作，则在原有模板的基础上再挖坑

            // readUtf8String() 方法来读取指定文件的内容.传入的是文件的路径
            // 其实这里你不知道输出什么，你可以打个断点，然后看输出什么
            fileContent = FileUtil.readUtf8String(fileOutputAbsolutePath);
        } else {
            // 如果没有模板文件，表示是第一次制作，先读入文件，然后直接挖坑。
            fileContent = FileUtil.readUtf8String(fileInputAbsolutePath);
        }

        // 支持多个模型：对同一个文件的内容，遍历模型进行多轮替换

        // 获取模型组的配置
        TemplateMakerModelConfig.ModelGroupConfig modelGroupConfig = templateMakerModelConfig.getModelGroupConfig();
        // fileContent就是上面说的，已有模板文件则在上面的基础上增加，没有则就是一个要挖坑的文件
        String newFileContent = fileContent;
        // 替换值
        String replacement;

        for (TemplateMakerModelConfig.ModelInfoConfig modelInfoConfig : templateMakerModelConfig.getModels()) {
            // 不是分组
            if (modelGroupConfig == null) {
                replacement = String.format("${%s}", modelInfoConfig.getFieldName());
            } else {
                // 是分组
                String groupKey = modelGroupConfig.getGroupKey();
                // 注意挖坑要多一个层级
                replacement = String.format("${%s.%s}", groupKey, modelInfoConfig.getFieldName());
            }
            // 多次替换。将字符串 newFileContent 中符合指定条件的部分（也就是 modelInfoConfig.getReplaceText指定的替换内容）替换为指定的新内容。
            newFileContent = StrUtil.replace(newFileContent, modelInfoConfig.getReplaceText(), replacement);
        }

        // 文件配置信息
        Meta.FileConfig.FileInfo fileInfo = new Meta.FileConfig.FileInfo();
        // 注意文件输入路径要和输出路径反转
        // 他这里为什么要反转？因为，在制作模板时，我们是根据原始文件得到FTL模板文件。
        // 但是在代码生成器的元信息中，其实是根据FTL模板文件来生成目标文件。
        fileInfo.setInputPath(fileOutputPath);
        fileInfo.setOutputPath(fileInputPath);
        fileInfo.setCondition(fileInfoConfig.getCondition());
        fileInfo.setType(FileTypeEnum.FILE.getValue());
        fileInfo.setGenerateType(FileGenerateTypeEnum.DYNAMIC.getValue());

        // 是否更改了文件内容
        boolean contentEquals = newFileContent.equals(fileContent);
        // 之前不存在模板文件，并且没有更改文件内容，则为静态生成
        if (!hasTemplateFile) {
            if (contentEquals) {
                // 输入路径没有 FTL 后缀
                fileInfo.setInputPath(fileInputPath);
                fileInfo.setGenerateType(FileGenerateTypeEnum.STATIC.getValue());
            } else {
                // 没有模板文件，需要挖坑，生成模板文件
                FileUtil.writeUtf8String(newFileContent, fileOutputAbsolutePath);
            }
        } else if (!contentEquals) {
            // 有模板文件，且增加了新坑，生成模板文件
            FileUtil.writeUtf8String(newFileContent, fileOutputAbsolutePath);
        }

        return fileInfo;
    }

    /**
     * 模型去重
     *
     * @param modelInfoList
     * @return
     */
    private static List<Meta.ModelConfig.ModelInfo> distinctModels(List<Meta.ModelConfig.ModelInfo> modelInfoList) {
        // 策略：同分组内模型 merge，不同分组保留

        // 1. 有分组的，以组为单位划分
        // {"groupKey": "a", models:[1,2], {"groupKey" :"a",models:[2,3], {"groupKey" :"b",models:[4,5]}}
        // 从上面变成下面，以a为组
        // {"groupKey":"a", models:[[1,2],[2,3]]},{"groupKey":"b", models:[[4,5]]}
        Map<String, List<Meta.ModelConfig.ModelInfo>> groupKeyModelInfoListMap = modelInfoList
                .stream()
                .filter(modelInfo -> StrUtil.isNotBlank(modelInfo.getGroupKey()))
                .collect(
                        Collectors.groupingBy(Meta.ModelConfig.ModelInfo::getGroupKey)
                );


        // 2. 同组内的模型配置合并
        // 保存每个组对应的合并后的对象 map
        // 保存每个组对应的合并后的对象 map
        // {"groupKey":"a", models:[[1,2],[2,3]]}
        // 从上面变成下面组内合并
        // {"groupKey":"a",models:[[1,2,3]]}
        Map<String, Meta.ModelConfig.ModelInfo> groupKeyMergedModelInfoMap = new HashMap<>();
        for (Map.Entry<String, List<Meta.ModelConfig.ModelInfo>> entry : groupKeyModelInfoListMap.entrySet()) {
            // tempModelInfoList是[models:[[1,2],[2,3]]}]
            List<Meta.ModelConfig.ModelInfo> tempModelInfoList = entry.getValue();
            List<Meta.ModelConfig.ModelInfo> newModelInfoList = new ArrayList<>(tempModelInfoList.stream()
                    // 这里就是[models:[[1,2],[2,3]]变成[models:[[1,2,2,3]] 多个对象变一个
                    .flatMap(modelInfo -> modelInfo.getModels().stream())
                    .collect(
                            //将模型信息列表按照输出路径进行映射，确保每个输出路径只有一个对应的模型信息对象
                            Collectors.toMap(Meta.ModelConfig.ModelInfo::getFieldName, o -> o, (e, r) -> r)
                    ).values());

            // 使用新的 group 配置
            Meta.ModelConfig.ModelInfo newModelInfo = CollUtil.getLast(tempModelInfoList);
            newModelInfo.setModels(newModelInfoList);
            String groupKey = entry.getKey();
            groupKeyMergedModelInfoMap.put(groupKey, newModelInfo);
        }

        // 3. 将模型分组添加到结果列表
        List<Meta.ModelConfig.ModelInfo> resultList = new ArrayList<>(groupKeyMergedModelInfoMap.values());

        // 4. 将未分组的模型添加到结果列表
        List<Meta.ModelConfig.ModelInfo> noGroupModelInfoList = modelInfoList.stream().filter(modelInfo -> StrUtil.isBlank(modelInfo.getGroupKey()))
                .collect(Collectors.toList());
        resultList.addAll(new ArrayList<>(noGroupModelInfoList.stream()
                .collect(
                        Collectors.toMap(Meta.ModelConfig.ModelInfo::getFieldName, o -> o, (e, r) -> r)
                ).values()));
        return resultList;
    }

    /**
     * 文件去重
     *
     * @param fileInfoList
     * @return
     */
    private static List<Meta.FileConfig.FileInfo> distinctFiles(List<Meta.FileConfig.FileInfo> fileInfoList) {
        // 策略：同分组内文件 merge，不同分组保留

        // 1. 有分组的，以组为单位划分
        // {"groupKey": "a", files:[1,2], {"groupKey" :"a",files:[2,3], {"groupKey" :"b",files:[4,5]}}
        // 从上面变成下面，以a为组
        // {"groupKey":"a", files:[[1,2],[2,3]]},{"groupKey":"b", files:[[4,5]]}
        Map<String, List<Meta.FileConfig.FileInfo>> groupKeyFileInfoListMap = fileInfoList
                .stream()
                .filter(fileInfo -> StrUtil.isNotBlank(fileInfo.getGroupKey()))
                .collect(
                        // 将流中的元素按照 groupKey 属性进行分组
                        Collectors.groupingBy(Meta.FileConfig.FileInfo::getGroupKey)
                );


        // 2. 同组内的文件配置合并
        // 保存每个组对应的合并后的对象 map
        // {"groupKey":"a", files:[[1,2],[2,3]]}
        // 从上面变成下面组内合并
        // {"groupKey":"a",files:[[1,2,3]]}
        Map<String, Meta.FileConfig.FileInfo> groupKeyMergedFileInfoMap = new HashMap<>();
        for (Map.Entry<String, List<Meta.FileConfig.FileInfo>> entry : groupKeyFileInfoListMap.entrySet()) {
            // tempFileInfoList是[files:[[1,2],[2,3]]}]
            List<Meta.FileConfig.FileInfo> tempFileInfoList = entry.getValue();
            List<Meta.FileConfig.FileInfo> newFileInfoList = new ArrayList<>(tempFileInfoList.stream()
                    // 这里就是[files:[[1,2],[2,3]]变成[files:[[1,2,2,3]] 多个对象变一个
                    .flatMap(fileInfo -> fileInfo.getFiles().stream())
                    .collect(
                            /**
                             * Meta.FileConfig.FileInfo::getOutputPath 用作键的提取函数，表示以 FileInfo 对象的 outputPath 属性作为键。
                             * o -> o 用作值的提取函数，表示直接将元素作为值。
                             * (e, r) -> r 是解决冲突的函数，当有重复的键时，保留旧值。这里可能意味着 outputPath 属性在流中是唯一的，因此不会出现重复键。
                             */

                            /**
                             * √√√√√√√√√√√√√√√√√√√√
                             * 他这里是根据同一个输入路径去重，因为你看meta.json，如果一个文件重复，那肯定是
                             * inputPath这些都重复了，那我不可能都去判断一下把，我随便拿一个出来判断即可。
                             * 这里就是拿出来了输入路径去重
                             */
                            /**
                             * o -> o, (e, r) -> r 这里o -> o表示直接使用文件信息对象作为值。
                             * 举个例子就是假设key是"inputPath": "src/com/maiko7/acm/MainTemplate.java", value是下面这个
                             * {
                             *         "inputPath": "src/com/maiko7/acm/MainTemplate.java",
                             *         "outputPath": "src/com/maiko7/acm/MainTemplate.java.ftl",
                             *         "type": "file",
                             *         "generateType": "dynamic"
                             * }
                             * 那么你o->o表示的就是直接使用这个值，不做任何处理
                             *
                             * (e, r) -> r表示当键冲突时，保留原有的值（r），忽略新的值（e）
                             * 这里就是假设有相同的路径，相同的inputPath我怎么办？那我就保留新值
                             *
                             */
                            //将文件信息列表按照输出路径进行映射，确保每个输出路径只有一个对应的文件信息对象
                            Collectors.toMap(Meta.FileConfig.FileInfo::getOutputPath, o -> o, (e, r) -> r)
                    ).values());

            // 使用新的 group 配置
            Meta.FileConfig.FileInfo newFileInfo = CollUtil.getLast(tempFileInfoList);
            newFileInfo.setFiles(newFileInfoList);
            String groupKey = entry.getKey();
            groupKeyMergedFileInfoMap.put(groupKey, newFileInfo);
        }

        // 3. 将文件分组添加到结果列表
        List<Meta.FileConfig.FileInfo> resultList = new ArrayList<>(groupKeyMergedFileInfoMap.values());

        // 4. 将未分组的文件添加到结果列表

        // 从 fileInfoList 中筛选出 groupKey 为空的元素
        List<Meta.FileConfig.FileInfo> noGroupFileInfoList = fileInfoList.stream().filter(fileInfo -> StrUtil.isBlank(fileInfo.getGroupKey()))
                .collect(Collectors.toList());
        resultList.addAll(new ArrayList<>(noGroupFileInfoList.stream()
                .collect(
                        //将文件信息列表按照输出路径进行映射，确保每个输出路径只有一个对应的文件信息对象
                        Collectors.toMap(Meta.FileConfig.FileInfo::getOutputPath, o -> o, (e, r) -> r)
                ).values()));
        return resultList;
    }
}
