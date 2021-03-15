package cn.promptness.pdf.util;


import cn.promptness.pdf.image.png.PngEncoderSimple;
import com.pngencoder.PngEncoder;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PdfUtil {

    private final static Logger logger = LoggerFactory.getLogger(PdfUtil.class);


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
            logger.info("第{}页", ++index);
            PDResources resources = pdPage.getResources();
            resources.getXObjectNames();
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
                // 先转成PNG设置alpha通道
                BufferedImage newBufferedImage = Thumbnails.of(bufferedImage).imageType(BufferedImage.TYPE_INT_ARGB).scale(1F).outputQuality(1F).outputFormat("PNG").asBufferedImage();

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                // 检测是否包含透明像素点
                if (isTransparent(newBufferedImage)) {
                    if (pngSimple) {
                        new PngEncoderSimple().setCompressed(true).write(bufferedImage, outputStream);
                    } else {
                        // CompressionLevel : 0 - 9 数字越大压缩越小
                        int level = new BigDecimal("9").multiply(rate).intValue();
                        new PngEncoder().withBufferedImage(bufferedImage).withCompressionLevel(level).toStream(outputStream);
                    }
                } else {
                    // Quality 0 - 1 数字越小压缩越小
                    double quality = BigDecimal.ONE.subtract(rate).setScale(2, RoundingMode.HALF_UP).doubleValue();
                    Thumbnails.of(bufferedImage).scale(1F).outputQuality(quality).outputFormat("JPG").toOutputStream(outputStream);
                }
                // 替换图片
                PDImageXObject fromByteArray = PDImageXObject.createFromByteArray(document, outputStream.toByteArray(), xObjectName.getName());
                resources.put(xObjectName, fromByteArray);
            }
        }
        document.save(out);
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
            logger.info("第{}页", ++page);
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
