package cn.promptness.pdf.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Component
public final class SpringFxmlLoader {

    /**
     * 注入Spring上下文对象
     */
    @Resource
    private ApplicationContext applicationContext;

    /**
     * 获取一个ControllerFactory被SpringBeanFactory管理的FXMLLoader对象
     *
     * @param resource fxml文件的路径
     */
    public FXMLLoader getLoader(String resource) {
        FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource(resource));
        fxmlLoader.setControllerFactory(applicationContext::getBean);
        return fxmlLoader;
    }

    public Parent load(String resource) {
        FXMLLoader fxmlLoader = getLoader(resource);
        try {
            return fxmlLoader.load();
        } catch (IOException ignored) {

        }
        return new Pane();
    }

}
