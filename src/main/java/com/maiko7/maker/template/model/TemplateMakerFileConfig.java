package com.maiko7.maker.template.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文件过滤、文件分组
 */
@Data
public class TemplateMakerFileConfig {

    private List<FileInfoConfig> files;

    private FileGroupConfig fileGroupConfig;

    @NoArgsConstructor
    @Data
    // 文件过滤
    public static class FileInfoConfig {

        /**
         * 他为什么不用这个进行分组，因为path的是目录，对应目录下的所有文件都是同组
         * 但是我有可能要跨目录设置为一个组
         */
        private String path;

        private String condition;

        private List<FileFilterConfig> filterConfigList;
    }

    @Data
    public static class FileGroupConfig {

        private String condition;

        private String groupKey;

        private String groupName;
    }
}
