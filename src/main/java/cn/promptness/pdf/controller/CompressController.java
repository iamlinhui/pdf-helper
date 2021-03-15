package cn.promptness.pdf.controller;

import cn.promptness.pdf.service.CompressService;
import cn.promptness.pdf.util.ProgressStage;
import cn.promptness.pdf.util.SystemTrayUtil;
import cn.promptness.pdf.util.TooltipUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Controller
public class CompressController extends PdfController {

    @FXML
    public Slider slider;

    @Override
    public void initialize() {
        super.initialize();
    }

    @FXML
    public void showTooltip() {
        TooltipUtil.show("压缩比率越大文件压缩越小");
    }


    @FXML
    public void compress() {
        CompressService compressService = new CompressService();
        compressService.setSourceName(sourcePath.getText());
        compressService.setAbsoluteSourcePath(sourcePath.getId());
        compressService.setOut(outPath.getText());
        compressService.setRate(BigDecimal.valueOf(slider.getValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));

        ProgressStage.of(SystemTrayUtil.getPrimaryStage(), compressService, "PDF文件压缩中...").show();
    }

}
