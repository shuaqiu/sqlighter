package com.shuaqiu.sqlighter.processor;

import javax.lang.model.element.Element;

/**
 * 处理异常
 * Created by qiush on 2015/10/8.
 */
public class ProcessingException extends Exception {

    private final Element element;

    public ProcessingException(final Element element, final String msg, final Object... args) {
        super(String.format(msg, args));
        this.element = element;
    }

    public Element getElement() {
        return element;
    }
}
