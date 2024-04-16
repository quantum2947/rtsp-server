package com.rtsp.rtspserver.server;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FrameGrabber;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RtspFrameSender {
    private List<RtspSession> subscribers = new ArrayList<>();

    private RtspServer rtspServer;

    private FFmpegFrameGrabber grabber;

    private AVPacket frame;

    private int fileTimeBase;

    private double fileFrameRate;

    private long startRealTimeMillis;

    private long nextTimeStamp;

    private long startTimeStamp;

    private boolean isStart;

    public RtspFrameSender(RtspServer rtspServer) {
        this.rtspServer = rtspServer;
    }

    public void subscribe(RtspSession rtspSession) {
        log.info("remote ip [{}] subscribe stream", rtspSession.getChannel().remoteAddress().toString());
        subscribers.add(rtspSession);
    }

    public boolean init(String filePath) {
        try {
            grabber = new FFmpegFrameGrabber(filePath);
            grabber.start();
            frame = grabber.grabPacket();
            fileTimeBase = grabber.getFormatContext().streams(0).time_base().den();
            fileFrameRate = grabber.getFrameRate();
            log.info("fileTimeBase: {}, fileFrameRate: {}", fileTimeBase, fileFrameRate);
        } catch (FFmpegFrameGrabber.Exception e) {
            log.info("start frame grabber failed, filePath [{}]", filePath);
            return false;
        }

        return true;
    }

    public void sendFrame() {
        if (!isStart) {
            startRealTimeMillis = System.currentTimeMillis();
            isStart = true;
        }

        try {
            for (RtspSession subscriber : subscribers) {
                AVPacket packet = new AVPacket();
                avcodec.av_packet_ref(packet, frame);
                subscriber.sendFrame(grabber, packet);
                avcodec.av_packet_unref(packet);
            }

            avcodec.av_packet_unref(frame);

            frame = grabber.grabPacket();
            if (frame == null) {
                log.info("send rtsp frame finished");
                close();
            }

            nextTimeStamp += Math.round(1000 / fileFrameRate);
        } catch (FFmpegFrameGrabber.Exception | FFmpegFrameRecorder.Exception e) {
            log.error("send frame failed", e.getMessage());
            close();
        }
    }

    public void close() {
        try {
            subscribers.forEach(rtspSession -> {rtspSession.end(false);});
            subscribers.clear();
            grabber.close();
            rtspServer.close();
        } catch (FrameGrabber.Exception e) {
            log.error("Close frame grabber failed, {}", e.getMessage());
        }
    }

    public void unsubscribeStream(RtspSession rtspSession) {
        subscribers.remove(rtspSession);
    }

    public long getFrameSendDelay() {
        long nowTime = System.currentTimeMillis();
        return (nextTimeStamp - startTimeStamp) - (nowTime - startRealTimeMillis);
    }
}
