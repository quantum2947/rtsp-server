package com.rtsp.rtspserver.server.method;

import com.rtsp.rtspserver.server.RtspServerHandler;
import com.rtsp.rtspserver.server.RtspSession;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.handler.codec.rtsp.RtspVersions;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.PointerPointer;

import java.nio.charset.StandardCharsets;

import static org.bytedeco.ffmpeg.global.avformat.*;

@Slf4j
public class DescribeAction implements MethodAction {
    private static DescribeAction INSTANCE = new DescribeAction();

    private DescribeAction() {}

    public static DescribeAction getInstance() {
        return INSTANCE;
    }

    @Override
    public FullHttpResponse buildHttpResponse(HttpRequest request, RtspServerHandler serverHandler, RtspSession rtspSession) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, HttpResponseStatus.OK);
        response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
        response.headers().add(RtspHeaderNames.CONTENT_TYPE, "application/sdp");
        String sdpMsg = getSdpMsg(serverHandler.getFilePath());
        response.content().writeCharSequence(sdpMsg, StandardCharsets.UTF_8);
        response.headers().set(RtspHeaderNames.CONTENT_LENGTH, response.content().writerIndex());
        response.headers().add(RtspHeaderNames.CONTENT_BASE, request.getUri());
        log.info("sdpMsg : ------------------ {}", sdpMsg);
        return response;
    }

    private String getSdpMsg(String filePath) {
        AVFormatContext avFormatContext = avformat.avformat_alloc_context();
        int res = avformat_open_input(avFormatContext, filePath, null, null);
        if (res < 0) {
            byte[] data = new byte[4096];
            avutil.av_strerror(res, data, data.length);
            log.error("Could not open input stream {}, error code : {}, description : {}", filePath, res, data);
            return "";
        }

        avformat_find_stream_info(avFormatContext, (PointerPointer<?>) null);
        byte[] data = new byte[16384];
        av_sdp_create(avFormatContext, 1, data, 2048);
        String sdpMsg = new String(data, StandardCharsets.UTF_8);
        avformat_free_context(avFormatContext);
        return sdpMsg;
    }

}
