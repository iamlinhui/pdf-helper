package cn.promptness.pdf.controller;

import cn.promptness.pdf.util.SystemTrayUtil;
import cn.promptness.pdf.util.TooltipUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.springframework.util.StringUtils;

import java.io.File;

public abstract class PdfController {

    @FXML
    public Text sourcePath;
    @FXML
    public Text outPath;
    @FXML
    public Button button;

    public boolean isRunning;

    public void initialize() {

        sourcePath.textProperty().addListener((observable, oldValue, newValue) -> {
            button.setDisable(StringUtils.isEmpty(sourcePath.getText()) || StringUtils.isEmpty(outPath.getText()));
        });

        outPath.textProperty().addListener((observable, oldValue, newValue) -> {
            button.setDisable(StringUtils.isEmpty(sourcePath.getText()) || StringUtils.isEmpty(outPath.getText()));
        });
    }

    @FXML
    public void fileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("请选择PDF文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("需要操作的PDF文件", "*.pdf")
        );
        File selectedFile = fileChooser.showOpenDialog(SystemTrayUtil.getPrimaryStage());

        if (selectedFile == null || !selectedFile.isFile() || !selectedFile.exists()) {
            TooltipUtil.show("选择的文件选择不合法");
            return;
        }
        sourcePath.setText(selectedFile.getName());
        sourcePath.setId(selectedFile.getAbsolutePath());
    }

    @FXML
    public void directoryChooser() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("请设置导出目录");

        File selectedDirectory = directoryChooser.showDialog(SystemTrayUtil.getPrimaryStage());

        if (selectedDirectory == null || !selectedDirectory.isDirectory() || !selectedDirectory.exists()) {
            TooltipUtil.show("导出文件夹选择不合法");
            return;
        }
        outPath.setText(selectedDirectory.getAbsolutePath());
    }

    public void reset() {
        if (!isRunning) {
            sourcePath.setId("");
            sourcePath.setText("");
            outPath.setText("");
            button.setDisable(true);
        }
    }
}
