package com.rtsp.rtspserver.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FrameRecorder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;

@Slf4j
public class RtspSession {
    private static AtomicInteger RTP_PORT = new AtomicInteger(34532);

    private FFmpegFrameRecorder recorder;

    private RtspFrameSender frameSender;

    private AVFormatContext formatContext;

    @Getter
    private Channel channel;

    private String rtpUrl;

    private double timeStamp;

    private double timeStep;

    public RtspSession(Channel channel, RtspFrameSender frameSender) {
        this.channel = channel;
        this.frameSender = frameSender;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RtspSession) {
            return this.channel.compareTo(((RtspSession) obj).channel) == 0;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.channel.hashCode();
    }

    public void subscribeStream() {
        if (frameSender != null) {
            frameSender.subscribe(this);
        }
        channel.closeFuture().addListener((ChannelFutureListener) future -> {
            end(true);
        });
    }

    public void sendFrame(FFmpegFrameGrabber grabber, AVPacket frame) throws FFmpegFrameRecorder.Exception {
        if (recorder == null) {
            prepareFrameRecorder(grabber);
        }

        if (timeStep == 0) {
            timeStep = formatContext.streams(0).time_base().den() / recorder.getFrameRate();
        }

        frame.pts(Math.round(timeStamp));
        frame.dts(Math.round(timeStamp));
        recorder.recordPacket(frame);
        timeStamp += timeStep;
    }

    public void end(boolean isRemoteClose) {
        try {
            channel.close().syncUninterruptibly();
            if (recorder != null) {
                recorder.close();
            }
        } catch (FrameRecorder.Exception e) {
            log.error("Close ffmpeg frame recorder failed {}", e.getMessage());
        }

        if (isRemoteClose) {
            frameSender.unsubscribeStream(this);
        }
    }

    public String parseTranspost(String transport) {
        String[] split = transport.split(";");
        String[] remotePort = split[2].split("-");
        int remoteRtpPort = Integer.parseInt(remotePort[0].substring(remotePort[0].indexOf("=") + 1));
        int serverRtpPort = RTP_PORT.getAndIncrement();
        int serverRtcpPort = RTP_PORT.getAndIncrement();
        String remoteIp = getRemoteIp();
        String serverIp;
        try {
            InetAddress localInetAddress = InetAddress.getLocalHost();
            serverIp = localInetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            serverIp = "localhost";
        }
        rtpUrl = String.format(Locale.ENGLISH, "rtp://%s:%d?localaddr=%s&localrtpport=%d&localrtcpport=%d&connect=1",
                remoteIp, remoteRtpPort, serverIp, serverRtpPort, serverRtcpPort);
        return String.format(Locale.ENGLISH, "%s;server_port=%d-%d", transport, serverRtpPort, serverRtcpPort);
    }

    private String getRemoteIp() {
        SocketAddress remoteAddress = channel.remoteAddress();
        if (remoteAddress instanceof InetSocketAddress) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) remoteAddress;
            return inetSocketAddress.getAddress().getHostAddress(); // 获取IPv4地址
        }

        return null;
    }

    private void prepareFrameRecorder(FFmpegFrameGrabber grabber) throws FFmpegFrameRecorder.Exception {
        av_log_set_level(avutil.AV_LOG_INFO);
        int width = grabber.getImageWidth();
        int height = grabber.getImageHeight();
        recorder = new FFmpegFrameRecorder(rtpUrl, width, height);
        recorder.setVideoBitrate(grabber.getVideoBitrate());
        recorder.setVideoCodec(grabber.getVideoCodec());
        recorder.setFormat("rtp");
        recorder.setFrameRate(grabber.getFrameRate());
        formatContext = grabber.getFormatContext();
        recorder.start(formatContext);
    }
}
