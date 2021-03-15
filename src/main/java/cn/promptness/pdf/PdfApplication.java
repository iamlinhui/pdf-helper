package cn.promptness.pdf;

import cn.promptness.pdf.data.Constant;
import cn.promptness.pdf.util.SpringFxmlLoader;
import cn.promptness.pdf.util.SystemTrayUtil;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.awt.*;

@SpringBootApplication
public class PdfApplication extends Application {


    private ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        if (!SystemTray.isSupported()) {
            System.exit(1);
        }
        Application.launch(PdfApplication.class, args);
    }

    @Override
    public void init() {
        applicationContext = new SpringApplicationBuilder().sources(PdfApplication.class).bannerMode(Banner.Mode.OFF).web(WebApplicationType.NONE).run(getParameters().getRaw().toArray(new String[0]));
    }


    @Override
    public void start(Stage primaryStage) {
        Parent root = applicationContext.getBean(SpringFxmlLoader.class).load("/fxml/main.fxml");
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        primaryStage.setTitle(Constant.TITLE);
        primaryStage.getIcons().add(new Image("/logo.png"));
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setHeight(400);
        primaryStage.setWidth(600);
        primaryStage.show();
        SystemTrayUtil.getInstance(primaryStage);
    }
}
