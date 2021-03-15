package cn.promptness.pdf.service;

import cn.promptness.pdf.util.PdfUtil;
import cn.promptness.pdf.util.TooltipUtil;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.math.BigDecimal;

public class CompressService extends Task<Void> {

    private static final Logger logger = LoggerFactory.getLogger(CompressService.class);


    private BigDecimal rate;

    private String sourceName;

    private String absoluteSourcePath;

    private String out;

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getAbsoluteSourcePath() {
        return absoluteSourcePath;
    }

    public void setAbsoluteSourcePath(String absoluteSourcePath) {
        this.absoluteSourcePath = absoluteSourcePath;
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
            String outPath = String.format("%s/%s_%s_%s", out, System.currentTimeMillis(), rate, sourceName);
            logger.info(outPath);
            PdfUtil.compress(new File(absoluteSourcePath), new File(outPath), true, rate);
            Desktop.getDesktop().open(new File(out));
        } catch (Exception e) {
            TooltipUtil.show(e.getMessage());
        }
        return null;
    }
}
