package cn.promptness.pdf.controller;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;
import java.io.IOException;

@Controller
public class MainController {


    @Resource
    private ConfigurableApplicationContext applicationContext;

    public void initialize() throws IOException {

    }

}
