package com.rtsp.rtspserver.server.method;

import com.rtsp.rtspserver.server.RtspServerHandler;
import com.rtsp.rtspserver.server.RtspSession;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

public interface MethodAction {
    FullHttpResponse buildHttpResponse(HttpRequest request, RtspServerHandler serverHandler, RtspSession rtspSession);
}
