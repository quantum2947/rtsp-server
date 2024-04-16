package com.rtsp.rtspserver.server.method;

import com.rtsp.rtspserver.server.RtspServerHandler;
import com.rtsp.rtspserver.server.RtspSession;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.handler.codec.rtsp.RtspVersions;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class SetupAction implements MethodAction {
    private static SetupAction INSTANCE = new SetupAction();

    private SetupAction() {
    }

    public static SetupAction getInstance() {
        return INSTANCE;
    }

    @Override
    public FullHttpResponse buildHttpResponse(HttpRequest request, RtspServerHandler serverHandler, RtspSession rtspSession) {
        log.info("SetUp request : " + request.toString());
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.OK);
        response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
        response.headers().set(RtspHeaderNames.SESSION, UUID.randomUUID().toString().substring(0, 12));
        String transport = rtspSession.parseTranspost(request.headers().get(RtspHeaderNames.TRANSPORT));
        response.headers().set(RtspHeaderNames.TRANSPORT, transport);
        log.info("SetUp response : " + response.toString());
        return response;
    }
}
