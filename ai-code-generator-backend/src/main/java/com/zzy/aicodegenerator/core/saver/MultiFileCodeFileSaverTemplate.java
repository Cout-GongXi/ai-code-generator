package com.zzy.aicodegenerator.core.saver;

import cn.hutool.core.util.StrUtil;
import com.zzy.aicodegenerator.ai.model.MultiFileCodeResult;
import com.zzy.aicodegenerator.exception.BusinessException;
import com.zzy.aicodegenerator.exception.ErrorCode;
import com.zzy.aicodegenerator.model.enums.CodeGenTypeEnum;

public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult>{
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        // 保存 HTML 代码
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        // 保存 CSS 代码
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        // 保存 JS 文件
        writeToFile(baseDirPath, "script.js", result.getJsCode());
    }

    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        // 至少要有Html代码、CSS代码、JavaScript代码可以为空
        if (StrUtil.isBlank(result.getHtmlCode())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Html代码内容不能为空");
        }
    }

}
