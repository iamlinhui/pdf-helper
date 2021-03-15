package cn.promptness.pdf.controller;

import cn.promptness.pdf.data.Constant;
import cn.promptness.pdf.util.SystemTrayUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;

@Controller
public class MenuBarController {


    @Resource
    private CompressController compressController;
    @Resource
    private ExportController exportController;

    public void initialize() {


    }

    @FXML
    public void exit() {
        System.exit(0);
    }

    @FXML
    public void about() {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle(Constant.TITLE);
        alert.setHeaderText("关于");
        alert.setContentText("Version 1.0.0 \nPowered By Lynn");
        alert.initOwner(SystemTrayUtil.getPrimaryStage());
        alert.getButtonTypes().add(ButtonType.CLOSE);
        alert.showAndWait();
    }

    @FXML
    public void instruction() {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle(Constant.TITLE);
        alert.setHeaderText("使用说明");
        alert.setContentText("1.压缩比率越大文件压缩越小\n2.文件中小于512K的图片会过滤压缩");
        alert.initOwner(SystemTrayUtil.getPrimaryStage());
        alert.getButtonTypes().add(ButtonType.CLOSE);
        alert.showAndWait();
    }

    @FXML
    public void reset() {
        compressController.reset();
        exportController.reset();
    }
}
