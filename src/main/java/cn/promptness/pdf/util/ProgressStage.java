package cn.promptness.pdf.util;

import javafx.concurrent.Service;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;

public class ProgressStage {

    private Stage stage;
    private Service<?> work;

    private ProgressStage() {
    }

    /**
     * 创建
     *
     * @param parent
     * @param work
     * @return
     */
    public static ProgressStage of(Stage parent, Service<?> work) {
        ProgressStage ps = new ProgressStage();
        ps.work = Objects.requireNonNull(work);
        ps.initUI(parent);
        return ps;
    }

    /**
     * 显示
     */
    public void show() {
        work.start();
        stage.show();
    }

    private void initUI(Stage parent) {
        stage = new Stage();
        stage.initOwner(parent);
        // style
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);

        // progress
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setProgress(-1);
        indicator.progressProperty().bind(work.progressProperty());

        // pack
        VBox vBox = new VBox();
        vBox.setSpacing(10);
        vBox.setBackground(Background.EMPTY);
        vBox.getChildren().addAll(indicator);

        // scene
        Scene scene = new Scene(vBox);
        scene.setFill(null);
        stage.setScene(scene);
        stage.setWidth(120);
        stage.setHeight(120);

        // show center of parent
        double x = parent.getX() + (parent.getWidth() - stage.getWidth()) / 2;
        double y = parent.getY() + (parent.getHeight() - stage.getHeight()) / 2;
        stage.setX(x);
        stage.setY(y);

        // close if work finish
        EventHandler<WorkerStateEvent> onSucceeded = work.getOnSucceeded();
        work.setOnSucceeded(e -> {
            stage.close();
            if (onSucceeded != null) {
                onSucceeded.handle(e);
            }
        });
    }
}