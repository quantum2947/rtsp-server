package com.rtsp.rtspserver.server.method;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.rtsp.RtspMethods;

public enum MethodFactory {
    OPTIONS(RtspMethods.OPTIONS, OptionsAction.getInstance()),

    DESCRIBE(RtspMethods.DESCRIBE, DescribeAction.getInstance()),

    SETUP(RtspMethods.SETUP, SetupAction.getInstance()),

    PLAY(RtspMethods.PLAY, PlayAction.getInstance()),

    TEARDOWN(RtspMethods.TEARDOWN, TearDownAction.getInstance());

    HttpMethod httpMethod;
    MethodAction methodAction;

    MethodFactory(HttpMethod method, MethodAction action) {
        this.httpMethod = method;
        this.methodAction = action;
    }

    public static MethodAction getMethodAction(HttpMethod method) {
        for (MethodFactory value : MethodFactory.values()) {
            if (value.httpMethod.equals(method)) {
                return value.methodAction;
            }
        }

        return DefaultAction.getInstance();
    }
}
