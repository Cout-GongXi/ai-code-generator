package com.zzy.aicodegenerator.core.saver;

import com.jfinal.template.stat.ast.Switch;
import com.zzy.aicodegenerator.ai.model.HtmlCodeResult;
import com.zzy.aicodegenerator.ai.model.MultiFileCodeResult;
import com.zzy.aicodegenerator.exception.BusinessException;
import com.zzy.aicodegenerator.exception.ErrorCode;
import com.zzy.aicodegenerator.model.enums.CodeGenTypeEnum;

import java.io.File;

public class CodeFileSaverExecutor {

    private static final HtmlCodeFileSaverTemplate htmlCodeFileSaverTemplate = new HtmlCodeFileSaverTemplate();

    private static final MultiFileCodeFileSaverTemplate multiFileCodeFileSaverTemplate = new MultiFileCodeFileSaverTemplate();

    /**
     * 执行保存代码
     *
     * @param codeResult 代码结果
     * @param codeGenType 代码生成类型
     * @return 文件目录对象
     */
    public static File executeSaver(Object codeResult, CodeGenTypeEnum codeGenType) {
        return switch (codeGenType) {
            case HTML -> htmlCodeFileSaverTemplate.saveCode((HtmlCodeResult) codeResult);
            case MULTI_FILE -> multiFileCodeFileSaverTemplate.saveCode((MultiFileCodeResult) codeResult);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        };
    }
}
