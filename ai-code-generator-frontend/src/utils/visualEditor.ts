/**
 * 可视化编辑器工具
 * 用于在 iframe 中实现元素选择和高亮功能
 */

export interface SelectedElementInfo {
  tagName: string
  id?: string
  className?: string
  textContent?: string
  xpath?: string
  pagePath?: string
}

/**
 * 注入到 iframe 中的脚本
 * 用于监听鼠标事件并高亮元素
 */
export const getInjectedScript = () => {
  return `
    (function() {
      let hoveredElement = null;
      let selectedElement = null;
      const HOVER_BORDER_CLASS = 'visual-editor-hover';
      const SELECTED_BORDER_CLASS = 'visual-editor-selected';

      // 添加样式
      const style = document.createElement('style');
      style.textContent = \`
        .visual-editor-hover {
          outline: 2px dashed #1890ff !important;
          outline-offset: 2px !important;
          cursor: pointer !important;
        }
        .visual-editor-selected {
          outline: 2px solid #52c41a !important;
          outline-offset: 2px !important;
        }
      \`;
      document.head.appendChild(style);

      // 获取元素的 XPath
      function getXPath(element) {
        if (element.id) {
          return '//*[@id="' + element.id + '"]';
        }
        if (element === document.body) {
          return '/html/body';
        }
        let ix = 0;
        const siblings = element.parentNode.childNodes;
        for (let i = 0; i < siblings.length; i++) {
          const sibling = siblings[i];
          if (sibling === element) {
            return getXPath(element.parentNode) + '/' + element.tagName.toLowerCase() + '[' + (ix + 1) + ']';
          }
          if (sibling.nodeType === 1 && sibling.tagName === element.tagName) {
            ix++;
          }
        }
      }

      // 获取元素信息
      function getElementInfo(element) {
        return {
          tagName: element.tagName,
          id: element.id || undefined,
          className: element.className || undefined,
          textContent: element.textContent?.trim().substring(0, 100) || undefined,
          xpath: getXPath(element),
          pagePath: window.location.pathname
        };
      }

      // 鼠标移入事件
      function handleMouseOver(e) {
        if (selectedElement) return; // 已选中元素时不响应悬浮

        e.stopPropagation();
        if (hoveredElement) {
          hoveredElement.classList.remove(HOVER_BORDER_CLASS);
        }
        hoveredElement = e.target;
        hoveredElement.classList.add(HOVER_BORDER_CLASS);
      }

      // 鼠标移出事件
      function handleMouseOut(e) {
        if (selectedElement) return;

        e.stopPropagation();
        if (hoveredElement) {
          hoveredElement.classList.remove(HOVER_BORDER_CLASS);
          hoveredElement = null;
        }
      }

      // 点击事件
      function handleClick(e) {
        e.preventDefault();
        e.stopPropagation();

        // 清除之前的选中状态
        if (selectedElement) {
          selectedElement.classList.remove(SELECTED_BORDER_CLASS);
        }

        // 清除悬浮状态
        if (hoveredElement) {
          hoveredElement.classList.remove(HOVER_BORDER_CLASS);
        }

        // 设置新的选中状态
        selectedElement = e.target;
        selectedElement.classList.add(SELECTED_BORDER_CLASS);

        // 发送选中元素信息到父窗口
        const elementInfo = getElementInfo(selectedElement);
        window.parent.postMessage({
          type: 'ELEMENT_SELECTED',
          data: elementInfo
        }, '*');
      }

      // 启动编辑模式
      function startEditMode() {
        document.addEventListener('mouseover', handleMouseOver, true);
        document.addEventListener('mouseout', handleMouseOut, true);
        document.addEventListener('click', handleClick, true);
      }

      // 停止编辑模式
      function stopEditMode() {
        document.removeEventListener('mouseover', handleMouseOver, true);
        document.removeEventListener('mouseout', handleMouseOut, true);
        document.removeEventListener('click', handleClick, true);

        // 清除所有高亮
        if (hoveredElement) {
          hoveredElement.classList.remove(HOVER_BORDER_CLASS);
          hoveredElement = null;
        }
        if (selectedElement) {
          selectedElement.classList.remove(SELECTED_BORDER_CLASS);
          selectedElement = null;
        }
      }

      // 清除选中元素
      function clearSelection() {
        if (selectedElement) {
          selectedElement.classList.remove(SELECTED_BORDER_CLASS);
          selectedElement = null;
        }
      }

      // 监听来自父窗口的消息
      window.addEventListener('message', function(event) {
        if (event.data.type === 'START_EDIT_MODE') {
          startEditMode();
        } else if (event.data.type === 'STOP_EDIT_MODE') {
          stopEditMode();
        } else if (event.data.type === 'CLEAR_SELECTION') {
          clearSelection();
        }
      });

      // 通知父窗口脚本已加载
      window.parent.postMessage({ type: 'EDITOR_READY' }, '*');
    })();
  `;
}

/**
 * 格式化元素信息为提示词
 */
export const formatElementInfoToPrompt = (elementInfo: SelectedElementInfo): string => {
  const parts: string[] = []

  parts.push(`选中元素：${elementInfo.tagName.toLowerCase()}`)

  if (elementInfo.id) {
    parts.push(`ID: ${elementInfo.id}`)
  }

  if (elementInfo.className) {
    parts.push(`类名: ${elementInfo.className}`)
  }

  if (elementInfo.textContent) {
    parts.push(`内容: ${elementInfo.textContent}`)
  }

  if (elementInfo.pagePath) {
    parts.push(`页面路径: ${elementInfo.pagePath}`)
  }

  return `\n\n[选中的元素信息]\n${parts.join('\n')}\n`
}
