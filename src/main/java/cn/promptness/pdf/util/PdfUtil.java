package cn.promptness.pdf.util;


import cn.promptness.pdf.image.png.PngEncoderSimple;
import com.pngencoder.PngEncoder;
import javafx.application.Platform;
import javafx.util.Pair;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.bytedeco.javacpp.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class PdfUtil {

    private static final Logger logger = LoggerFactory.getLogger(PdfUtil.class);
    private static final Integer CORE_SIZE = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService TASK_THREAD_POOL = new ThreadPoolExecutor(CORE_SIZE * 2, CORE_SIZE * 3, 5L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(CORE_SIZE * 1000), new ThreadPoolExecutor.CallerRunsPolicy());

    private static volatile String ffmpegPath;

    private static String getFfmpegPath() {
        if (ffmpegPath == null) {
            synchronized (PdfUtil.class) {
                if (ffmpegPath == null) {
                    try {
                        ffmpegPath = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
                        logger.info("ffmpeg loaded from bytedeco: {}", ffmpegPath);
                    } catch (Exception e) {
                        logger.warn("Failed to load ffmpeg from bytedeco: {}", e.getMessage());
                        ffmpegPath = "";
                    }
                }
            }
        }
        return ffmpegPath;
    }


    /**
     * 压缩PDF文件
     *
     * @param source
     * @param out
     * @throws Exception
     */
    public static void compress(File source, File out, boolean pngSimple, BigDecimal rate) throws Exception {

        logger.info("{}", rate);

        PDDocument document = PDDocument.load(source);
        PDPageTree pdPageTree = document.getDocumentCatalog().getPages();
        int count = pdPageTree.getCount();
        if (count <= 0) {
            return;
        }

        int index = 0;
        for (PDPage pdPage : pdPageTree) {
            final String processing = String.format("第%s页", ++index);
            logger.info(processing);
            Platform.runLater(() -> TooltipUtil.show(processing));
            PDResources resources = pdPage.getResources();
            List<Future<Pair<COSName, PDImageXObject>>> futureList = new LinkedList<>();
            for (COSName xObjectName : resources.getXObjectNames()) {
                if (!resources.isImageXObject(xObjectName)) {
                    continue;
                }
                PDImageXObject pdImageObject = (PDImageXObject) resources.getXObject(xObjectName);
                BufferedImage bufferedImage = pdImageObject.getImage();
                // 小于512K的不压缩
                if (bufferedImage.getData().getDataBuffer().getSize() < 1024 * 512) {
                    continue;
                }
                futureList.add(TASK_THREAD_POOL.submit(() -> getPdImageXObject(pngSimple, rate, document, xObjectName, bufferedImage)));
            }
            for (Future<Pair<COSName, PDImageXObject>> pairFuture : futureList) {
                pairFuture.get();
                // 替换图片
                resources.put(pairFuture.get().getKey(), pairFuture.get().getValue());
            }
        }
        // PDF contains an encryption dictionary, please remove it with setAllSecurityToBeRemoved()
        document.setAllSecurityToBeRemoved(true);
        document.save(out);
    }

    private static Pair<COSName, PDImageXObject> getPdImageXObject(boolean pngSimple, BigDecimal rate, PDDocument document, COSName xObjectName, BufferedImage bufferedImage) throws IOException {
        // 根据压缩率计算缩放因子: rate=0 -> scale=1.0, rate=1.0 -> scale=0.4
        double scaleFactor = BigDecimal.ONE.subtract(rate.multiply(new BigDecimal("0.6"))).setScale(2, RoundingMode.HALF_UP).doubleValue();
        scaleFactor = Math.max(0.2, Math.min(1.0, scaleFactor));

        // 先转成PNG设置alpha通道
        BufferedImage newBufferedImage = Thumbnails.of(bufferedImage).imageType(BufferedImage.TYPE_INT_ARGB).scale(1F).outputQuality(1F).outputFormat("PNG").asBufferedImage();

        byte[] compressedBytes;
        // 检测是否包含透明像素点
        boolean transparent = isTransparent(newBufferedImage);

        String ffmpeg = getFfmpegPath();
        if (!ffmpeg.isEmpty()) {
            compressedBytes = compressWithFfmpeg(ffmpeg, bufferedImage, transparent, rate, scaleFactor);
        } else {
            compressedBytes = compressWithJava(bufferedImage, transparent, pngSimple, rate, scaleFactor);
        }

        return new Pair<>(xObjectName, PDImageXObject.createFromByteArray(document, compressedBytes, xObjectName.getName()));
    }

    /**
     * 使用ffmpeg进行图片压缩 (更好的编码质量)
     */
    private static byte[] compressWithFfmpeg(String ffmpeg, BufferedImage image, boolean transparent, BigDecimal rate, double scaleFactor) throws IOException {
        File tempInput = File.createTempFile("pdf_compress_in_", ".png");
        File tempOutput = null;
        try {
            ImageIO.write(image, "PNG", tempInput);

            String scaleFilter = String.format("scale=iw*%.2f:ih*%.2f", scaleFactor, scaleFactor);

            if (transparent) {
                tempOutput = File.createTempFile("pdf_compress_out_", ".png");
                // PNG: 使用ffmpeg缩放 + 最大压缩
                CommandLine cmdLine = new CommandLine(ffmpeg);
                cmdLine.addArgument("-y");
                cmdLine.addArgument("-i").addArgument(tempInput.getAbsolutePath());
                cmdLine.addArgument("-vf").addArgument(scaleFilter);
                cmdLine.addArgument("-compression_level").addArgument("9");
                cmdLine.addArgument(tempOutput.getAbsolutePath());
                executeFfmpeg(cmdLine);
            } else {
                tempOutput = File.createTempFile("pdf_compress_out_", ".jpg");
                // JPG: 使用ffmpeg缩放 + quality控制 (q:v 2-31, 越小质量越好)
                int qv = 2 + new BigDecimal("29").multiply(rate).intValue();
                CommandLine cmdLine = new CommandLine(ffmpeg);
                cmdLine.addArgument("-y");
                cmdLine.addArgument("-i").addArgument(tempInput.getAbsolutePath());
                cmdLine.addArgument("-vf").addArgument(scaleFilter);
                cmdLine.addArgument("-q:v").addArgument(String.valueOf(qv));
                cmdLine.addArgument(tempOutput.getAbsolutePath());
                executeFfmpeg(cmdLine);
            }

            return Files.readAllBytes(tempOutput.toPath());
        } finally {
            tempInput.delete();
            if (tempOutput != null) {
                tempOutput.delete();
            }
        }
    }

    private static void executeFfmpeg(CommandLine cmdLine) throws IOException {
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWatchdog(new ExecuteWatchdog(60_000L));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        executor.setStreamHandler(new PumpStreamHandler(outputStream));
        try {
            executor.execute(cmdLine);
        } catch (IOException e) {
            throw new IOException("ffmpeg failed: " + outputStream.toString(), e);
        }
    }

    /**
     * 使用Java内置编码器压缩 (ffmpeg不可用时的降级方案, 增加了缩放)
     */
    private static byte[] compressWithJava(BufferedImage image, boolean transparent, boolean pngSimple, BigDecimal rate, double scaleFactor) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (transparent) {
            // PNG先缩放再压缩
            BufferedImage scaled = Thumbnails.of(image).scale(scaleFactor).asBufferedImage();
            if (pngSimple) {
                new PngEncoderSimple().setCompressed(true).write(scaled, outputStream);
            } else {
                int level = new BigDecimal("9").multiply(rate).intValue();
                new PngEncoder().withBufferedImage(scaled).withCompressionLevel(level).toStream(outputStream);
            }
        } else {
            // JPG: 缩放 + 质量控制
            double quality = BigDecimal.ONE.subtract(rate).setScale(2, RoundingMode.HALF_UP).doubleValue();
            Thumbnails.of(image).scale(scaleFactor).outputQuality(quality).outputFormat("JPG").toOutputStream(outputStream);
        }
        return outputStream.toByteArray();
    }


    /**
     * 导出PDF中的图片
     *
     * @author lynn
     * @date 2021/3/15 14:19
     * @since v1.0.0
     */
    public static void images(String source, String out) throws Exception {

        PDDocument document = PDDocument.load(new File(source));
        PDPageTree pdPageTree = document.getDocumentCatalog().getPages();
        int count = pdPageTree.getCount();
        if (count <= 0) {
            return;
        }
        int index = 0;
        int page = 0;
        for (PDPage pdPage : pdPageTree) {
            final String processing = String.format("第%s页", ++page);
            logger.info(processing);
            Platform.runLater(() -> TooltipUtil.show(processing));
            PDResources resources = pdPage.getResources();
            resources.getXObjectNames();
            for (COSName xObjectName : resources.getXObjectNames()) {
                if (!resources.isImageXObject(xObjectName)) {
                    continue;
                }
                PDImageXObject pdImageObject = (PDImageXObject) resources.getXObject(xObjectName);
                BufferedImage bufferedImage = pdImageObject.getImage();
                ImageIO.write(bufferedImage, "PNG", new File(String.format("%s/%s.png", out, index++)));
            }
        }
    }

    public static boolean isTransparent(BufferedImage bufImg) {
        int height = bufImg.getHeight();
        int width = bufImg.getWidth();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int pixel = bufImg.getRGB(i, j);
                if (pixel >> 24 == 0x00) {
                    return true;
                }
            }
        }
        return false;
    }
}
