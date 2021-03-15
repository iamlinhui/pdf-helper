package cn.promptness.pdf.controller;

import cn.promptness.pdf.service.ExportService;
import cn.promptness.pdf.util.ProgressStage;
import cn.promptness.pdf.util.SystemTrayUtil;
import javafx.fxml.FXML;
import org.springframework.stereotype.Controller;

@Controller
public class ExportController extends PdfController {

    @Override
    public void initialize() {
        super.initialize();
    }

    @FXML
    public void export() {
        ExportService exportService = new ExportService();
        exportService.setSource(sourcePath.getId());
        exportService.setOut(outPath.getText());
        ProgressStage.of(SystemTrayUtil.getPrimaryStage(), exportService, "PDF文件导出图片中...").show();
    }

}
