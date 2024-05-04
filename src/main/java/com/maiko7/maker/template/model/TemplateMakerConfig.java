package com.maiko7.maker.template.model;

import com.maiko7.maker.meta.Meta;
import lombok.Data;

/**
 * 模板制作配置
 * 他就是makeTemplate里面的参数，封装成了一个类，不然你每次传那么多参数，不优雅。
 */
@Data
public class TemplateMakerConfig {

    /**
     * 这个id是用来，就是生成临时空间。比如maiko7-generator-maker/.temp/1
     * 你id我可以用雪花算法生成。保证每次临时空间不重样。
     */
    private Long id;

    private Meta meta = new Meta();

    // 原始项目路径
    private String originProjectPath;

    TemplateMakerFileConfig fileConfig = new TemplateMakerFileConfig();

    TemplateMakerModelConfig modelConfig = new TemplateMakerModelConfig();

    TemplateMakerOutputConfig outputConfig = new TemplateMakerOutputConfig();
}
