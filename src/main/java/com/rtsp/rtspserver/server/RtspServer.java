package com.rtsp.rtspserver.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.rtsp.RtspDecoder;
import io.netty.handler.codec.rtsp.RtspEncoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.bytedeco.ffmpeg.ffmpeg;
import org.bytedeco.javacpp.Loader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RtspServer {
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    private String serverHost;

    private int serverPort;

    private File originVideo;

    private String transferedFilePath;

    private ServerBootstrap serverBootstrap;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private ChannelFuture channelFuture;

    private RtspFrameSender rtspFrameSender;

    public RtspServer(File originVideo, int serverPort) {
        this.originVideo = originVideo;
        this.serverPort = serverPort;
    }

    private void init() throws IOException, InterruptedException {
        if (!originVideo.exists()) {
            throw new FileNotFoundException(originVideo.getCanonicalPath());
        }

        // add sps/pps to every frame
        transferedFilePath = FileUtils.getTempDirectoryPath() + File.separator + "rtsp" + File.separator + originVideo.getName();
        File file = new File(transferedFilePath);
        if (!file.exists()) {
            file.mkdirs();
            String ffmpeg = Loader.load(ffmpeg.class);
            String commond = "-i inputFile -preset ultrafast -threads 4 -c:v libx264 -x264opts keyint=50 -bsf:v dump_extra -an outputFile";
            commond = commond.replace("inputFile", originVideo.getCanonicalPath());
            commond = commond.replace("outputFile", transferedFilePath);
            ProcessBuilder processBuilder = new ProcessBuilder((ffmpeg + " " + commond).split(" "));
            processBuilder.inheritIO().start().waitFor();
        }

        // create frame sender
        rtspFrameSender = new RtspFrameSender(this);
        rtspFrameSender.init(transferedFilePath);

        // rtsp server param
        this.serverBootstrap = new ServerBootstrap();
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup(4);
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            serverBootstrap.channel(NioServerSocketChannel.class);
        }

        if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            serverBootstrap.channel(EpollServerSocketChannel.class);
        }

        serverBootstrap.group(bossGroup, workerGroup);
    }

    public String start() {
        log.info("RtspServer start");
        try {
            // init rtsp server
            init();

            // start rtsp server
            serverBootstrap.option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.SO_SNDBUF, 128 * 1024)
                    .childOption(ChannelOption.SO_RCVBUF, 64 * 1024)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(64 * 1024, 128 * 1024))
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(64 * 1024, 128 * 1024))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast(new RtspDecoder())
                                    .addLast(new RtspEncoder())
                                    .addLast(new HttpObjectAggregator(64 * 1024))
                                    .addLast(new RtspServerHandler(transferedFilePath, rtspFrameSender));
                        }
                    });

            InetAddress localInetAddress = InetAddress.getLocalHost();
            serverHost = localInetAddress.getHostAddress();
            channelFuture = serverBootstrap.bind(serverHost, serverPort).syncUninterruptibly();

            // send frame
            sendRtspFrame();
        } catch (Exception e) {
            log.error("RtspServer start failed, {}", e.getMessage());
            return "";
        }

        log.info("RtspServer start successfully, port:{}", serverPort);
        return String.format("rtsp://%s:%d", serverHost, serverPort);
    }

    private void sendRtspFrame() {
        long delay = rtspFrameSender.getFrameSendDelay();
        log.info("sendRtspFrame delay {}", delay);
        executor.schedule(() -> {
            rtspFrameSender.sendFrame();
            sendRtspFrame();
        }, delay, TimeUnit.MILLISECONDS);
    }

    public void close() {
        log.info("RtspServer start to close");
        executor.shutdownNow();
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
        channelFuture.channel().closeFuture().syncUninterruptibly();
        log.info("RtspServer close successfully");
    }
}
