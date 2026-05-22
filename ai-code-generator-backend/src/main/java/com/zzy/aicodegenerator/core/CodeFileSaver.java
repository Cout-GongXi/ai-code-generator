package com.zzy.aicodegenerator.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.zzy.aicodegenerator.ai.model.HtmlCodeResult;
import com.zzy.aicodegenerator.ai.model.MultiFileCodeResult;
import com.zzy.aicodegenerator.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 代码文件保存器
 *
 * @author zzy
 * @date 2024/6/17
 */
@Deprecated
public class CodeFileSaver {
    // 文件保存的根目录
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 保存 HTML 网页的代码
     */
    public static File saveHtmlCodeResult(HtmlCodeResult htmlCodeResult) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        writeToFile(baseDirPath, "index.html", htmlCodeResult.getHtmlCode());
        return new File(baseDirPath);

    }

    /**
     * 保存多文件代码
     */
    public static File saveMultiFileCodeResult(MultiFileCodeResult multiFileCodeResult) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.MULTI_FILE.getValue());
        writeToFile(baseDirPath, "index.html", multiFileCodeResult.getHtmlCode());
        writeToFile(baseDirPath, "style.css", multiFileCodeResult.getCssCode());
        writeToFile(baseDirPath, "script.js", multiFileCodeResult.getJsCode());
        return new File(baseDirPath);
    }

    /**
     * 约定文件的命名规则（文件类型 + 雪花ID）tmp/code_output/HTML_1234567890123456789
     *
     * @param bizType 代码生成类型
     * @return 唯一的目录路径
     */
    private static String buildUniqueDir(String bizType) {
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + bizType + "_" + IdUtil.getSnowflakeNextIdStr();
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 写入文件内容
     *
     * @param dirPath  目录路径
     * @param fileName 文件名
     * @param content  文件内容
     */
    private static void writeToFile(String dirPath, String fileName, String content) {
        // 1. 创建目录
        String filePath = dirPath + File.separator + fileName;
        // 3. 将内容写入文件
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }

}
