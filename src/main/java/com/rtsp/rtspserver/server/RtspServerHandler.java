package com.rtsp.rtspserver.server;

import com.rtsp.rtspserver.server.method.MethodAction;
import com.rtsp.rtspserver.server.method.MethodFactory;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RtspServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    @Getter
    private String filePath;

    private RtspFrameSender frameSender;

    private Map<Channel, RtspSession> lstChannels = new HashMap<>();

    public RtspServerHandler(String filePath, RtspFrameSender frameSender) {
        this.filePath = filePath;
        this.frameSender = frameSender;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        Channel channel = ctx.channel();
        if (lstChannels.containsKey(channel)) {
            ctx.channel().close();
        } else {
            lstChannels.put(channel, new RtspSession(channel, frameSender));
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        HttpMethod method = request.method();
        MethodAction methodAction = MethodFactory.getMethodAction(method);
        FullHttpResponse response = methodAction.buildHttpResponse(request, this, lstChannels.get(ctx.channel()));
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(cause.getMessage(), cause);
        lstChannels.remove(ctx.channel());
        ctx.close();
    }
}
