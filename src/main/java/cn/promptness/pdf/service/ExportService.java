package cn.promptness.pdf.service;

import cn.promptness.pdf.util.PdfUtil;
import cn.promptness.pdf.util.TooltipUtil;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.File;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ExportService extends Service<Void> {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

    private String source;
    private String out;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getOut() {
        return out;
    }

    public void setOut(String out) {
        this.out = out;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    PdfUtil.images(source, out);
                    Desktop.getDesktop().open(new File(out));
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    Platform.runLater(() -> TooltipUtil.show(e.getMessage()));
                }
                return null;
            }
        };
    }

}
