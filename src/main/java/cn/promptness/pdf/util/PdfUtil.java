package cn.promptness.pdf.util;

import com.pngencoder.PngEncoder;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

public class PdfUtil {


    /**
     * 压缩PDF文件
     *
     * @param source
     * @param out
     * @throws Exception
     */
    public static void compress(String source, String out) throws Exception {

        PDDocument document = PDDocument.load(new File(source));
        PDPageTree pdPageTree = document.getDocumentCatalog().getPages();

        if (pdPageTree.getCount() <= 0) {
            return;
        }

        for (PDPage pdPage : pdPageTree) {
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
                    // CompressionLevel : 0 - 9 数字越大压缩越小
                    new PngEncoder().withBufferedImage(bufferedImage).withCompressionLevel(9).toStream(outputStream);
                } else {
                    // Quality 0 - 1 数字越小压缩越小
                    Thumbnails.of(bufferedImage).scale(1F).outputQuality(0.1F).outputFormat("JPG").toOutputStream(outputStream);
                }
                // 替换图片
                PDImageXObject fromByteArray = PDImageXObject.createFromByteArray(document, outputStream.toByteArray(), xObjectName.getName());
                resources.put(xObjectName, fromByteArray);
            }
        }
        document.save(out);
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
