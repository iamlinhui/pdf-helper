package cn.promptness.pdf;

import com.idrsolutions.image.png.PngEncoder;
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

public class PdfApplication {


    public static void main(String[] args) throws Exception {

        readPDF("C:\\Users\\lynnlin\\Documents\\2021.01.21湖北宜昌温德姆至尊酒店概念意向方案.pdf", "D:/dddd.pdf");


    }


    /**
     * 压缩PDF文件
     *
     * @param source
     * @param out
     * @throws Exception
     */
    public static void readPDF(String source, String out) throws Exception {

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
                    PngEncoder pngEncoder = new PngEncoder();
                    pngEncoder.setCompressed(true);
                    pngEncoder.write(newBufferedImage, outputStream);
                } else {
                    Thumbnails.of(newBufferedImage).scale(1F).outputQuality(0.25F).outputFormat("JPG").toOutputStream(outputStream);
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
