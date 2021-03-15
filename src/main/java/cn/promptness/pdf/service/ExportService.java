package cn.promptness.pdf.service;

import cn.promptness.pdf.util.PdfUtil;
import cn.promptness.pdf.util.TooltipUtil;
import javafx.concurrent.Task;

import java.awt.*;
import java.io.File;


public class ExportService extends Task<Void> {

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
    protected Void call() {
        try {
            PdfUtil.images(source, out);
            Desktop.getDesktop().open(new File(out));
        } catch (Exception e) {
            TooltipUtil.show(e.getMessage());
        }
        return null;
    }

}
