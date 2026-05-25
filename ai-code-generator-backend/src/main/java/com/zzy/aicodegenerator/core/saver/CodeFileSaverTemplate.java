package com.zzy.aicodegenerator.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.zzy.aicodegenerator.ai.model.MultiFileCodeResult;
import com.zzy.aicodegenerator.constant.AppConstant;
import com.zzy.aicodegenerator.exception.BusinessException;
import com.zzy.aicodegenerator.exception.ErrorCode;
import com.zzy.aicodegenerator.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;


/**
 * 抽象代码文件保存器 - 模版方法模式
 *
 * @param <T>
 */
public abstract class CodeFileSaverTemplate<T> {
    /**
     * 文件保存的根目录
     */
    private static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 模版方法：保存代码的标准流程
     *
     * @param result 待保存的代码结果对象
     * @param appId  应用 ID
     * @return 文件目录对象
     */
    public final File saveCode(T result, Long appId) {
        //  1, 验证输入
        validateInput(result);
        // 2. 构建唯一目录
        String baseDirPath = buildUniqueDir(appId);
        // 3. 保存文件(具体实现交给子类）
        saveFiles(result, baseDirPath);
        // 4. 返回文件目录对象
        return new File(baseDirPath);
    }

    /**
     * 写入文件内容
     *
     * @param dirPath  目录路径
     * @param fileName 文件名
     * @param content  文件内容
     */
    public final void writeToFile(String dirPath, String fileName, String content) {
        if (StrUtil.isNotBlank(content)) {
            // 1. 创建目录
            String filePath = dirPath + File.separator + fileName;
            // 3. 将内容写入文件
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
        }
    }

    /**
     * 验证输入
     *
     * @param result 待保存的代码结果对象
     */
    protected void validateInput(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码结果对象不能为空");
        }
    }

    /**
     * 约定文件的命名规则（文件类型 + 雪花ID）tmp/code_output/HTML_1234567890123456789
     * @param appId  应用 ID
     * @return 唯一的目录路径
     */
    private String buildUniqueDir(Long appId) {
        if (appId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        }
        String codeType = getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", codeType, appId);
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 保存文件 具体实现交给子类
     *
     * @param result      代码结果
     * @param baseDirPath 文件保存的根目录
     */
    protected abstract void saveFiles(T result, String baseDirPath);

    /**
     * 获取代码的生成类型
     *
     * @return 枚举代码生成类型
     */
    protected abstract CodeGenTypeEnum getCodeType();

}
