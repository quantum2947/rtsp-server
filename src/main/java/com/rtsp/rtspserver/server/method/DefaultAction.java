package com.rtsp.rtspserver.server.method;

import com.rtsp.rtspserver.server.RtspServerHandler;
import com.rtsp.rtspserver.server.RtspSession;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.rtsp.RtspVersions;

public class DefaultAction implements MethodAction {
    private static DefaultAction INSTANCE = new DefaultAction();

    private DefaultAction() {}

    public static DefaultAction getInstance() {
        return INSTANCE;
    }

    @Override
    public FullHttpResponse buildHttpResponse(HttpRequest request, RtspServerHandler serverHandler, RtspSession rtspSession) {
        return new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.BAD_REQUEST);
    }
}
