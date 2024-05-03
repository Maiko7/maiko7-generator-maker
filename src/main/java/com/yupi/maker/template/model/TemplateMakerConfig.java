package com.yupi.maker.template.model;

import com.yupi.maker.meta.Meta;
import lombok.Data;

/**
 * 模板制作配置
 * 他就是makeTemplate里面的参数，封装成了一个类，不然你每次传那么多参数，不优雅。
 */
@Data
public class TemplateMakerConfig {

    private Long id;

    private Meta meta = new Meta();

    private String originProjectPath;

    TemplateMakerFileConfig fileConfig = new TemplateMakerFileConfig();

    TemplateMakerModelConfig modelConfig = new TemplateMakerModelConfig();

    TemplateMakerOutputConfig outputConfig = new TemplateMakerOutputConfig();
}
